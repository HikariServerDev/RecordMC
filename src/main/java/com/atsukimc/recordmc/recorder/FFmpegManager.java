package com.atsukimc.recordmc.recorder;

import com.atsukimc.recordmc.config.RecordConfigs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpegのプロセス起動・制御・動画結合を管理するクラス
 */
public class FFmpegManager {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/FFmpeg");
    private static String cachedFfmpegPath = null;

    /**
     * 利用可能なFFmpegの実行可能ファイルパスを取得する
     *
     * @return FFmpegのパス。見つからない場合はnull
     */
    public static String getFfmpegPath() {
        String customPath = null;
        try {
            if (RecordConfigs.Generic.CUSTOM_FFMPEG_PATH != null) {
                customPath = RecordConfigs.Generic.CUSTOM_FFMPEG_PATH.getStringValue();
            }
        } catch (Throwable ignored) {
        }

        if (customPath != null && !customPath.trim().isEmpty()) {
            String trimmed = customPath.trim();
            // 1. 絶対パスまたはカレントディレクトリからの相対パス
            File customFile = new File(trimmed);
            if (customFile.exists() && customFile.canExecute()) {
                return customFile.getAbsolutePath();
            }
            // 2. ゲーム実行ディレクトリからの相対パス
            try {
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null && client.runDirectory != null) {
                    File relativeFile = new File(client.runDirectory, trimmed);
                    if (relativeFile.exists() && relativeFile.canExecute()) {
                        return relativeFile.getAbsolutePath();
                    }
                }
            } catch (Throwable ignored) {
            }
            // 3. コマンド名として実行可能かチェック
            try {
                ProcessBuilder pb = new ProcessBuilder(trimmed, "-version");
                pb.redirectOutput(new File(System.getProperty("os.name").toLowerCase().contains("win") ? "NUL" : "/dev/null"));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return trimmed;
                }
                if (process.isAlive()) process.destroyForcibly();
            } catch (Throwable ignored) {
            }
        }

        if (cachedFfmpegPath != null) {
            return cachedFfmpegPath;
        }

        // 1. PATH環境変数の検索
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String isWindows = System.getProperty("os.name").toLowerCase();
            String[] exeNames = isWindows.contains("win") ? new String[]{"ffmpeg.exe", "ffmpeg"} : new String[]{"ffmpeg"};
            for (String dir : pathEnv.split(File.pathSeparator)) {
                for (String exe : exeNames) {
                    try {
                        Path candidate = Paths.get(dir, exe);
                        if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                            cachedFfmpegPath = candidate.toAbsolutePath().toString();
                            LOGGER.info("FFmpeg found in PATH: {}", cachedFfmpegPath);
                            return cachedFfmpegPath;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 2. 一般的なインストール場所の検索
        String[] candidatePaths = new String[]{
                "/usr/bin/ffmpeg",
                "/usr/local/bin/ffmpeg",
                "/snap/bin/ffmpeg",
                "/opt/homebrew/bin/ffmpeg",
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe"
        };

        for (String candidate : candidatePaths) {
            File f = new File(candidate);
            if (f.isFile() && f.canExecute()) {
                cachedFfmpegPath = f.getAbsolutePath();
                LOGGER.info("FFmpeg found at standard path: {}", cachedFfmpegPath);
                return cachedFfmpegPath;
            }
        }

        // 3. 'ffmpeg -version' コマンドで直接確認
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectOutput(new File(System.getProperty("os.name").toLowerCase().contains("win") ? "NUL" : "/dev/null"));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                cachedFfmpegPath = "ffmpeg";
                return cachedFfmpegPath;
            }
            if (process.isAlive()) process.destroyForcibly();
        } catch (Exception ignored) {
        }

        LOGGER.warn("FFmpeg not found on the system");
        return null;
    }

    /**
     * 映像キャプチャ用FFmpegプロセスを起動する
     *
     * @param width          動画幅
     * @param height         動画高さ
     * @param fps            フレームレート
     * @param tempOutputFile 一時動画ファイル保存先
     * @return 起動したProcess
     * @throws IOException 起動失敗時
     */
    public static Process startVideoEncodingProcess(int width, int height, int fps, File tempOutputFile) throws IOException {
        String ffmpeg = getFfmpegPath();
        if (ffmpeg == null) {
            throw new FileNotFoundException("FFmpeg executable not found");
        }

        String preset = RecordConfigs.Generic.PRESET.getStringValue();
        int crf = RecordConfigs.Generic.CRF.getIntegerValue();

        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        command.add("-y"); // 上書き許可
        command.add("-f");
        command.add("rawvideo");
        command.add("-vcodec");
        command.add("rawvideo");
        command.add("-s");
        command.add(width + "x" + height);
        command.add("-pix_fmt");
        command.add("rgba");
        command.add("-r");
        command.add(String.valueOf(fps));
        command.add("-i");
        command.add("-"); // 標準入力からフレーム受信
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(preset != null && !preset.isEmpty() ? preset : "ultrafast");
        command.add("-crf");
        command.add(String.valueOf(crf));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-vf");
        command.add("vflip"); // OpenGLの上下反転を補正
        command.add("-an");   // 音声なし
        command.add(tempOutputFile.getAbsolutePath());

        LOGGER.info("Starting FFmpeg video encoder: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // ログ出力を別スレッドで破棄・記録（バッファ詰まり防止）
        Thread loggerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("[FFmpeg-Video] " + line);
                }
            } catch (IOException ignored) {
            }
        }, "RecordMC-FFmpeg-Logger");
        loggerThread.setDaemon(true);
        loggerThread.start();

        return process;
    }

    /**
     * 録画終了時に映像と音声を高速結合 (Mux) して完成MP4を作成する
     *
     * @param videoFile 一時動画ファイル
     * @param audioFile 一時音声ファイル (nullまたは存在しない場合は映像のみ)
     * @param finalFile 最終MP4ファイル
     * @return 成功したかどうか
     */
    public static boolean muxVideoAndAudio(File videoFile, File audioFile, File finalFile) {
        String ffmpeg = getFfmpegPath();
        if (ffmpeg == null || !videoFile.exists()) {
            return false;
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        command.add("-y");
        command.add("-i");
        command.add(videoFile.getAbsolutePath());

        boolean hasAudio = audioFile != null && audioFile.exists() && audioFile.length() > 44; // WAVヘッダー44バイト超
        if (hasAudio) {
            command.add("-i");
            command.add(audioFile.getAbsolutePath());
        }

        String fileNameLower = finalFile.getName().toLowerCase();
        boolean isWebm = fileNameLower.endsWith(".webm");
        boolean isMkv = fileNameLower.endsWith(".mkv");
        boolean isMov = fileNameLower.endsWith(".mov");

        if (isWebm) {
            // WebM形式: VP9/VP8映像 + Opus音声
            int crf = 20;
            try {
                if (RecordConfigs.Generic.CRF != null) {
                    crf = RecordConfigs.Generic.CRF.getIntegerValue();
                }
            } catch (Throwable ignored) {
            }
            command.add("-c:v");
            command.add("libvpx-vp9");
            command.add("-b:v");
            command.add("0");
            command.add("-crf");
            command.add(String.valueOf(crf));
            command.add("-deadline");
            command.add("realtime");
            command.add("-cpu-used");
            command.add("8");

            if (hasAudio) {
                command.add("-c:a");
                command.add("libopus");
                command.add("-b:a");
                command.add("128k");
                command.add("-shortest");
            }
        } else {
            // MP4 / MOV / MKV: 映像は再エンコードなしで高速コピー
            command.add("-c:v");
            command.add("copy");

            if (hasAudio) {
                command.add("-c:a");
                command.add("aac");
                command.add("-b:a");
                command.add("192k");
                command.add("-shortest");
            }

            if (!isMkv) {
                // MP4 / MOV はWeb最適化のfaststartフラグを付与
                command.add("-movflags");
                command.add("+faststart");
            }
        }

        command.add(finalFile.getAbsolutePath());

        LOGGER.info("Muxing final video: {}", String.join(" ", command));

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            // ログ読み取り
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("[FFmpeg-Mux] " + line);
                }
            }

            int exitCode = process.waitFor();
            LOGGER.info("FFmpeg mux exited with code: {}", exitCode);
            return exitCode == 0 && finalFile.exists() && finalFile.length() > 0;
        } catch (Exception e) {
            LOGGER.error("Failed to mux video and audio", e);
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
