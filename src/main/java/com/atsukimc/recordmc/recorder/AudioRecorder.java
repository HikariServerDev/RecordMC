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
 * Minecraft内部のゲーム音声のみをWAVファイルとして録音するクラス
 */
public class AudioRecorder {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/Audio");
    public static final int SAMPLE_RATE = GameAudioMixer.SAMPLE_RATE; // 44100
    public static final int CHANNELS = GameAudioMixer.CHANNELS;       // 2 (Stereo)
    public static final int BYTES_PER_SAMPLE = 2;                     // 16-bit
    public static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE; // 4 bytes

    // 20ms あたりのフレーム数 (44100 * 0.02 = 882)
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
     * 音声録音を開始する
     */
    public boolean start() {
        try {
            recording.set(true);
            totalBytesWritten = 0;

            // WAVファイルヘッダー（暫定値）を書き込み開始
            writeInitialWavHeader(outputFile);

            // ミキサーの起動
            GameAudioMixer.getInstance().start();

            captureThread = new Thread(this::recordLoop, "RecordMC-AudioCapture");
            captureThread.setDaemon(true);
            captureThread.start();
            LOGGER.info("Game internal audio recording started");
            return true;
        } catch (Throwable t) {
            LOGGER.error("Failed to start audio recording", t);
            return false;
        }
    }

    /**
     * 音声録音ループ（バックグラウンドスレッドでMinecraftサウンドを20ms周期でミックスして書き込み）
     */
    private void recordLoop() {
        byte[] chunkBuffer = new byte[CHUNK_BYTES];
        long intervalNanos = 20_000_000L; // 20ms

        try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
            long nextTickTime = System.nanoTime();

            while (recording.get()) {
                // GameAudioMixer から 882フレーム (20ms) 分のPCMデータをレンダリング
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
                        break;
                    }
                }
            }
            fos.flush();
        } catch (Exception e) {
            LOGGER.error("Error writing audio data", e);
        }
    }

    /**
     * 音声録音を停止し、WAVヘッダーを確定する
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
            } catch (InterruptedException ignored) {
            }
        }

        // WAVファイルのRIFFサイズとDataサイズを更新
        finalizeWavHeader(outputFile, totalBytesWritten);
        LOGGER.info("Game audio recording stopped. Total bytes: {}", totalBytesWritten);
    }

    /**
     * WAVファイルのヘッダー（44バイト）を初期書き込みする
     */
    private static void writeInitialWavHeader(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] header = new byte[44];
            ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

            // RIFF header
            bb.put(new byte[]{'R', 'I', 'F', 'F'});
            bb.putInt(36); // 後で更新
            bb.put(new byte[]{'W', 'A', 'V', 'E'});

            // fmt chunk
            bb.put(new byte[]{'f', 'm', 't', ' '});
            bb.putInt(16); // Chunk size (16 for PCM)
            bb.putShort((short) 1); // Audio format (1 = PCM)
            bb.putShort((short) CHANNELS);
            bb.putInt(SAMPLE_RATE);
            bb.putInt(SAMPLE_RATE * FRAME_SIZE); // Byte rate
            bb.putShort((short) FRAME_SIZE); // Block align
            bb.putShort((short) 16); // Bits per sample

            // data chunk
            bb.put(new byte[]{'d', 'a', 't', 'a'});
            bb.putInt(0); // 後で更新

            fos.write(header);
        } catch (Exception e) {
            LOGGER.error("Failed to write initial WAV header", e);
        }
    }

    /**
     * WAVファイルのサイズフィールドを修正してヘッダーを確定する
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
            LOGGER.error("Failed to finalize WAV header", e);
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
