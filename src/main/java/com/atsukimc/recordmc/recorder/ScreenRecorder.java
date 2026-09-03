package com.atsukimc.recordmc.recorder;

import com.atsukimc.recordmc.config.RecordConfigs;
import com.atsukimc.recordmc.gui.RecordingOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PBO非同期キャプチャとFFmpeg連携を行う画面録画メインコントローラー
 */
public class ScreenRecorder {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/Recorder");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH-mm-ss");

    private static final ScreenRecorder INSTANCE = new ScreenRecorder();

    public enum State {
        IDLE,
        STARTING,
        RECORDING,
        STOPPING
    }

    private volatile State state = State.IDLE;
    private long recordingStartTime = 0;

    // 録画設定とパラメータ
    private int captureWidth;
    private int captureHeight;
    private int frameSize;
    private long frameIntervalNanos;
    private long lastFrameNanos = 0;
    private long frameCount = 0;

    // PBO (Pixel Buffer Objects) ダブルバッファリング用
    private final int[] pboIds = new int[2];
    private int pboIndex = 0;
    private boolean pboInitialized = false;

    // FFmpegプロセスとパイプ
    private Process videoProcess;
    private OutputStream videoStdin;
    private BlockingQueue<byte[]> frameQueue;
    private Thread videoFeedThread;
    private final AtomicBoolean feedingFrames = new AtomicBoolean(false);

    // 音声レコーダー
    private AudioRecorder audioRecorder;

    // 出力ファイルパス
    private File tempVideoFile;
    private File tempAudioFile;
    private File finalOutputFile;

    public static ScreenRecorder getInstance() {
        return INSTANCE;
    }

    public State getState() {
        return state;
    }

    public boolean isRecording() {
        return state == State.RECORDING || state == State.STARTING;
    }

    public long getRecordingStartTime() {
        return recordingStartTime;
    }

    /**
     * 現在有効な動画保存先ディレクトリを取得する（絶対パス・相対パスの両方に対応）
     */
    public File getEffectiveVideoDir() {
        MinecraftClient client = MinecraftClient.getInstance();
        File baseDir = client != null && client.runDirectory != null ? client.runDirectory : new File(".");

        String customDir = null;
        try {
            if (RecordConfigs.Generic.CUSTOM_VIDEO_DIR != null) {
                customDir = RecordConfigs.Generic.CUSTOM_VIDEO_DIR.getStringValue();
            }
        } catch (Throwable ignored) {
        }

        if (customDir != null && !customDir.trim().isEmpty()) {
            File custom = new File(customDir.trim());
            if (custom.isAbsolute()) {
                return custom;
            } else {
                return new File(baseDir, customDir.trim());
            }
        }

        return new File(baseDir, "video");
    }

    /**
     * 録画の開始・停止をトグルする
     */
    public synchronized void toggleRecording() {
        if (state == State.IDLE) {
            startRecording();
        } else if (state == State.RECORDING) {
            stopRecording();
        }
    }

    /**
     * 録画を開始する
     */
    public synchronized void startRecording() {
        if (state != State.IDLE) {
            return;
        }
        state = State.STARTING;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            state = State.IDLE;
            return;
        }

        // FFmpegの存在チェック
        String ffmpeg = FFmpegManager.getFfmpegPath();
        if (ffmpeg == null) {
            sendMessage(new TranslatableText("recordmc.message.error_ffmpeg_not_found"));
            state = State.IDLE;
            return;
        }

        int fps = RecordConfigs.Generic.FPS.getIntegerValue();

        // フレームバッファの解像度取得（偶数サイズに丸める）
        Framebuffer fb = client.getFramebuffer();
        int width = (fb != null ? fb.textureWidth : client.getWindow().getFramebufferWidth()) & ~1;
        int height = (fb != null ? fb.textureHeight : client.getWindow().getFramebufferHeight()) & ~1;

        if (width <= 0 || height <= 0) {
            sendMessage(new LiteralText("§c[RecordMC] 無効な画面解像度です: " + width + "x" + height));
            state = State.IDLE;
            return;
        }

        this.captureWidth = width;
        this.captureHeight = height;
        this.frameSize = width * height * 4; // RGBA
        this.frameIntervalNanos = 1_000_000_000L / Math.max(1, fps);
        this.lastFrameNanos = 0L;
        this.frameCount = 0;

        // 保存先ディレクトリの取得・作成 (絶対パス・相対パスの両方に対応)
        File videoDir = getEffectiveVideoDir();
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }

        com.atsukimc.recordmc.config.VideoFormat format = com.atsukimc.recordmc.config.VideoFormat.MP4;
        try {
            if (RecordConfigs.Generic.VIDEO_FORMAT != null) {
                format = (com.atsukimc.recordmc.config.VideoFormat) RecordConfigs.Generic.VIDEO_FORMAT.getOptionListValue();
            }
        } catch (Throwable ignored) {
        }
        String extension = (format != null) ? format.getExtension() : ".mp4";

        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMAT);
        this.tempVideoFile = new File(videoDir, ".temp_" + timestamp + ".mp4");
        this.tempAudioFile = new File(videoDir, ".temp_" + timestamp + ".wav");
        this.finalOutputFile = new File(videoDir, timestamp + extension);

        try {
            // FFmpeg映像プロセスの開始
            videoProcess = FFmpegManager.startVideoEncodingProcess(captureWidth, captureHeight, fps, tempVideoFile);
            videoStdin = videoProcess.getOutputStream();

            // フレーム送信キューとワーカースレッド
            frameQueue = new ArrayBlockingQueue<>(30);
            feedingFrames.set(true);
            videoFeedThread = new Thread(this::feedFramesLoop, "RecordMC-VideoFeeder");
            videoFeedThread.setDaemon(true);
            videoFeedThread.start();

            // 音声録音の開始
            if (RecordConfigs.Generic.RECORD_AUDIO.getBooleanValue()) {
                audioRecorder = new AudioRecorder(tempAudioFile);
                audioRecorder.start();
            } else {
                audioRecorder = null;
            }

            // PBOの初期化 (Renderスレッドで行うためフラグをリセット)
            pboInitialized = false;

            recordingStartTime = System.currentTimeMillis();
            state = State.RECORDING;

            // 開始通知 & 効果音再生
            sendMessage(new TranslatableText("recordmc.message.recording_started"));
            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
            LOGGER.info("Recording started: {}x{} @ {}fps -> {}", captureWidth, captureHeight, fps, finalOutputFile.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to start recording", e);
            sendMessage(new TranslatableText("recordmc.message.error_recording_failed", e.getMessage()));
            cleanupOnError();
            state = State.IDLE;
        }
    }

    /**
     * 録画を終了し、動画を保存する
     */
    public synchronized void stopRecording() {
        if (state != State.RECORDING) {
            return;
        }
        state = State.STOPPING;

        MinecraftClient client = MinecraftClient.getInstance();
        sendMessage(new TranslatableText("recordmc.message.recording_stopping"));
        if (client != null && client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }

        // バックグラウンドでエンコード終了とMuxを実行
        new Thread(() -> {
            try {
                LOGGER.info("Finalizing recording...");

                // 1. フレーム送信の停止
                feedingFrames.set(false);
                if (videoFeedThread != null) {
                    try {
                        videoFeedThread.join(3000);
                    } catch (InterruptedException ignored) {
                    }
                }

                // 2. 映像プロセスの終了待機
                if (videoStdin != null) {
                    try {
                        videoStdin.flush();
                        videoStdin.close();
                    } catch (Exception ignored) {
                    }
                }
                if (videoProcess != null) {
                    if (!videoProcess.waitFor(10, TimeUnit.SECONDS)) {
                        videoProcess.destroyForcibly();
                    }
                }

                // 3. 音声録音の停止
                if (audioRecorder != null) {
                    audioRecorder.stop();
                }

                // 4. Mux (映像と音声の結合)
                boolean success = FFmpegManager.muxVideoAndAudio(tempVideoFile, tempAudioFile, finalOutputFile);

                // 5. 一時ファイルの削除
                if (success) {
                    if (tempVideoFile != null && tempVideoFile.exists()) {
                        tempVideoFile.delete();
                    }
                    if (tempAudioFile != null && tempAudioFile.exists()) {
                        tempAudioFile.delete();
                    }
                } else {
                    LOGGER.warn("Muxing failed. Keeping temporary files for debugging.");
                }

                if (success && finalOutputFile.exists()) {
                    LOGGER.info("Recording successfully saved: {}", finalOutputFile.getAbsolutePath());

                    // クリップボードへコピー
                    boolean copied = false;
                    if (RecordConfigs.Generic.COPY_TO_CLIPBOARD.getBooleanValue()) {
                        copied = ClipboardHelper.copyFileToClipboard(finalOutputFile);
                    }

                    // クリックでフォルダを開くチャットメッセージ
                    Text linkText = new LiteralText(finalOutputFile.getName())
                            .formatted(Formatting.GREEN, Formatting.UNDERLINE)
                            .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, finalOutputFile.getParentFile().getAbsolutePath())));
                    Text message = new TranslatableText(
                            copied ? "recordmc.message.recording_saved_and_copied" : "recordmc.message.recording_saved",
                            linkText
                    ).append(" ").append(new TranslatableText("recordmc.message.recording_saved_click"));
                    sendMessage(message);
                } else {
                    LOGGER.error("Failed to create final video file");
                    sendMessage(new LiteralText("§c[RecordMC] 動画の保存に失敗しました。"));
                }
            } catch (Exception e) {
                LOGGER.error("Error during recording finalization", e);
                sendMessage(new TranslatableText("recordmc.message.error_recording_failed", e.getMessage()));
            } finally {
                cleanupPBO();
                videoProcess = null;
                videoStdin = null;
                frameQueue = null;
                videoFeedThread = null;
                audioRecorder = null;
                state = State.IDLE;
            }
        }, "RecordMC-Finalizer").start();
    }

    /**
     * 毎フレームの描画完了時に呼ばれるキャプチャメソッド（レンダースレッド上で実行）
     */
    public void onRenderFrame() {
        if (state != State.RECORDING) {
            return;
        }

        long nowNanos = System.nanoTime();
        if (lastFrameNanos == 0) {
            lastFrameNanos = nowNanos;
        } else if (nowNanos - lastFrameNanos < frameIntervalNanos) {
            // FPS制限：指定インターバル未満ならスキップ
            return;
        }
        lastFrameNanos = nowNanos;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        Framebuffer fb = client.getFramebuffer();
        if (fb == null) {
            return;
        }

        try {
            captureFramePBO(fb);
        } catch (Exception e) {
            LOGGER.error("Error capturing frame via PBO", e);
        }
    }

    /**
     * PBOダブルバッファを用いた非同期ピクセル読み出し
     */
    private void captureFramePBO(Framebuffer fb) {
        if (!pboInitialized) {
            initPBO();
            pboInitialized = true;
        }

        int nextIndex = (pboIndex + 1) % 2;

        // 1. 現在のPBOバッファへglReadPixelsで非同期転送をリクエスト (DMA)
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pboIds[pboIndex]);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);

        // 2. 前のフレームのPBOバッファからCPUメモリへマッピングしてピクセルを取得
        if (frameCount > 0) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pboIds[nextIndex]);
            ByteBuffer mappedBuffer = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY, null);

            if (mappedBuffer != null) {
                byte[] frameBytes = new byte[frameSize];
                mappedBuffer.get(frameBytes);
                GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);

                // フレームキューへ追加（キューが満杯の場合は古いフレームを破棄して同期維持）
                if (frameQueue != null && feedingFrames.get()) {
                    if (!frameQueue.offer(frameBytes)) {
                        frameQueue.poll(); // 古いフレームを破棄
                        frameQueue.offer(frameBytes);
                    }
                }
            }
        }

        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);

        pboIndex = nextIndex;
        frameCount++;
    }

    private void initPBO() {
        pboIds[0] = GL15.glGenBuffers();
        pboIds[1] = GL15.glGenBuffers();

        for (int id : pboIds) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, id);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, (long) frameSize, GL15.GL_STREAM_READ);
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        pboIndex = 0;
        frameCount = 0;
    }

    private void cleanupPBO() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // OpenGLリソースの解放は必ずメインのレンダースレッドで実行する
        client.execute(() -> {
            if (pboInitialized) {
                for (int id : pboIds) {
                    if (id != 0) {
                        GL15.glDeleteBuffers(id);
                    }
                }
                pboIds[0] = 0;
                pboIds[1] = 0;
                pboInitialized = false;
            }
        });
    }

    private void feedFramesLoop() {
        while (feedingFrames.get() || (frameQueue != null && !frameQueue.isEmpty())) {
            try {
                byte[] frame = frameQueue != null ? frameQueue.poll(100, TimeUnit.MILLISECONDS) : null;
                if (frame != null && videoStdin != null) {
                    videoStdin.write(frame);
                }
            } catch (Exception e) {
                if (feedingFrames.get()) {
                    LOGGER.debug("Error feeding frame to FFmpeg", e);
                }
                break;
            }
        }
    }

    private void cleanupOnError() {
        feedingFrames.set(false);
        if (videoStdin != null) {
            try {
                videoStdin.close();
            } catch (Exception ignored) {
            }
        }
        if (videoProcess != null && videoProcess.isAlive()) {
            videoProcess.destroyForcibly();
        }
        if (audioRecorder != null) {
            audioRecorder.stop();
        }
        cleanupPBO();
    }

    private void sendMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(message, false);
                }
            });
        }
    }
}
