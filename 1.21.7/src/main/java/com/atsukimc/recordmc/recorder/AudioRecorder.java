package com.atsukimc.recordmc.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minecraft内部のゲーム音声（BGM、効果音、環境音）のみをPCMミックスし、
 * 標準RIFF WAV形式のファイルとして記録するクラス。
 *
 * <p>設計上の特徴:
 * <ul>
 *   <li>外部マイクやOSの他アプリ音声を含めず、ゲーム内の純粋なオーディオのみを収録。</li>
 *   <li>20ms（882サンプル）単位の周期でレンダリングとディスク書き込みを行い、オーディオバッファの遅延とメモリ負荷を最小化。</li>
 *   <li>録画開始時に仮ヘッダーを書き込み、停止時にRandomAccessFileで正確なデータ長ヘッダーを確定。</li>
 * </ul>
 */
public class AudioRecorder {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/Audio");

    public static final int SAMPLE_RATE = GameAudioMixer.SAMPLE_RATE; // 44100 Hz
    public static final int CHANNELS = GameAudioMixer.CHANNELS;       // 2 (Stereo)
    public static final int BYTES_PER_SAMPLE = 2;                     // 16-bit signed
    public static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE; // 4 bytes

    // 20msあたりのサンプルフレーム数 (44100 * 0.02 = 882)
    private static final int CHUNK_FRAMES = 882;
    private static final int CHUNK_BYTES = CHUNK_FRAMES * FRAME_SIZE;

    private final File outputFile;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Thread captureThread;
    private volatile long totalBytesWritten = 0;

    public AudioRecorder(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * 音声録音スレッドを起動し、WAVヘッダーの初期書き込みを開始する。
     *
     * @return 正常に録音が開始された場合はtrue
     */
    public boolean start() {
        try {
            recording.set(true);
            totalBytesWritten = 0;

            // WAVヘッダー（暫定データ長0）を初期書き込み
            writeInitialWavHeader(outputFile);

            // ゲーム内サウンドリスナー・ミキサーを起動
            GameAudioMixer.getInstance().start();

            captureThread = new Thread(this::recordLoop, "RecordMC-AudioCapture");
            captureThread.setDaemon(true);
            captureThread.start();
            LOGGER.info("Internal game audio recorder started: {}", outputFile.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            LOGGER.error("Failed to start audio recording", t);
            recording.set(false);
            return false;
        }
    }

    /**
     * 20ms周期でGameAudioMixerからPCMサンプルを取得し、ファイルへ順次追記するループ。
     */
    private void recordLoop() {
        byte[] chunkBuffer = new byte[CHUNK_BYTES];
        long intervalNanos = 20_000_000L; // 20ms

        try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
            long nextTickTime = System.nanoTime();

            while (recording.get()) {
                GameAudioMixer.getInstance().renderSamples(CHUNK_FRAMES, chunkBuffer);

                fos.write(chunkBuffer, 0, CHUNK_BYTES);
                totalBytesWritten += CHUNK_BYTES;

                nextTickTime += intervalNanos;
                long sleepNanos = nextTickTime - System.nanoTime();
                if (sleepNanos > 0) {
                    long sleepMs = sleepNanos / 1_000_000L;
                    int sleepNs = (int) (sleepNanos % 1_000_000L);
                    try {
                        Thread.sleep(sleepMs, sleepNs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            fos.flush();
        } catch (Exception e) {
            LOGGER.error("Exception in audio recording loop", e);
        }
    }

    /**
     * 音声録音スレッドを停止し、WAVファイルヘッダーのデータ長フィールドを確定する。
     */
    public void stop() {
        if (!recording.compareAndSet(true, false)) {
            return;
        }

        GameAudioMixer.getInstance().stop();

        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted while waiting for audio capture thread to terminate");
                Thread.currentThread().interrupt();
            }
        }

        finalizeWavHeader(outputFile, totalBytesWritten);
        LOGGER.info("Internal game audio recording stopped. Written {} bytes to {}", totalBytesWritten, outputFile.getName());
    }

    /**
     * 標準44バイトのPCM WAVヘッダーを書き込む。
     */
    private static void writeInitialWavHeader(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] header = new byte[44];
            ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

            // RIFF header
            bb.put(new byte[]{'R', 'I', 'F', 'F'});
            bb.putInt(36); // データ確定時に (36 + dataBytes) で更新
            bb.put(new byte[]{'W', 'A', 'V', 'E'});

            // fmt chunk
            bb.put(new byte[]{'f', 'm', 't', ' '});
            bb.putInt(16); // PCM形式のfmtチャンクサイズ
            bb.putShort((short) 1); // 1 = Linear PCM
            bb.putShort((short) CHANNELS);
            bb.putInt(SAMPLE_RATE);
            bb.putInt(SAMPLE_RATE * FRAME_SIZE); // 1秒あたりのバイト数
            bb.putShort((short) FRAME_SIZE);     // 1ブロックのバイト数
            bb.putShort((short) 16);             // 1サンプルあたりのビット数

            // data chunk
            bb.put(new byte[]{'d', 'a', 't', 'a'});
            bb.putInt(0); // 録音完了時に実サイズへ更新

            fos.write(header);
        } catch (Exception e) {
            LOGGER.error("Failed to write initial WAV header for {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * 録音完了後、実際の書き込みバイト数に合わせてRIFFサイズおよびdataチャンクサイズを上書き更新する。
     */
    private static void finalizeWavHeader(File file, long dataBytes) {
        if (!file.exists() || dataBytes <= 0) {
            return;
        }
        int clampedDataBytes = (int) Math.min(dataBytes, Integer.MAX_VALUE);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // RIFF chunk size (4 + (8 + 16) + (8 + dataBytes) - 8 = 36 + dataBytes)
            raf.seek(4);
            raf.write(intToLittleEndian(36 + clampedDataBytes));

            // data chunk size
            raf.seek(40);
            raf.write(intToLittleEndian(clampedDataBytes));
        } catch (Exception e) {
            LOGGER.error("Failed to finalize WAV header for {}", file.getAbsolutePath(), e);
        }
    }

    private static byte[] intToLittleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }
}
