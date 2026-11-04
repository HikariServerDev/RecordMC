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
 * FFmpegプロセスのライフサイクル（検出・起動・エンコード・Mux結合）を管理するクラス。
 *
 * <p>設計上の特徴:
 * <ul>
 *   <li>OpenGLから読み出したRGBA生フレームを標準入力経由でFFmpegにパイプ送信し、ディスクI/O遅延を極小化。</li>
 *   <li>Flatpak環境等のプラットフォーム制約（特許問題でlibx264が非同梱の環境）に対応するため、
 *       起動時にダミープローブを行い、最適な動画エンコーダー（libx264 -> nvenc -> vaapi -> mpeg4）へ自動フォールバック。</li>
 *   <li>録画完了時は映像と音声を再エンコードなし（-c:v copy）で瞬時にMux結合し、録画終了時の待ち時間を短縮。</li>
 * </ul>
 */
public class FFmpegManager {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/FFmpeg");

    // ProcessBuilderのリダイレクト先として使用するNullデバイスファイル（Java 8〜21互換）
    private static final File NULL_FILE = new File(System.getProperty("os.name").toLowerCase().contains("win") ? "NUL" : "/dev/null");

    private static String cachedFfmpegPath = null;
    private static volatile String cachedVideoEncoder = null;

    /**
     * 実行中の環境で正常に動作する最適な動画エンコーダーをプローブして返す。
     * 一度検出されたエンコーダーはセッション終了までキャッシュされる。
     */
    public static String getBestVideoEncoder() {
        if (cachedVideoEncoder != null) {
            return cachedVideoEncoder;
        }

        String ffmpeg = getFfmpegPath();
        if (ffmpeg == null) {
            return "libx264";
        }

        // エンコーダーの優先順位:
        // 1. libx264: 最も標準的で互換性の高いCPUエンコーダー
        // 2. h264_nvenc: NVIDIA GPU搭載時の高速ハードウェアエンコーダー
        // 3. h264_vaapi: Linux標準のハードウェアアクセラレーション
        // 4. mpeg4: Flatpak (Freedesktop/KDE Runtime) 等でlibx264が非同梱の場合でも動作するMP4互換フォールバック
        String[] candidates = new String[]{"libx264", "h264_nvenc", "h264_vaapi", "mpeg4"};
        for (String encoder : candidates) {
            if (testEncoder(ffmpeg, encoder)) {
                cachedVideoEncoder = encoder;
                LOGGER.info("Detected and selected video encoder: {}", encoder);
                return encoder;
            }
        }

        LOGGER.warn("All video encoder probes failed. Falling back to default: libx264");
        cachedVideoEncoder = "libx264";
        return cachedVideoEncoder;
    }

    /**
     * lavfiのテストソースを用いて0.1秒のダミーエンコードを実行し、エンコーダーの実動可能性をミリ秒単位で確認する。
     */
    private static boolean testEncoder(String ffmpeg, String encoder) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpeg,
                    "-y",
                    "-f", "lavfi",
                    "-i", "testsrc=duration=0.1:size=64x64:rate=10",
                    "-c:v", encoder,
                    "-f", "null",
                    "-"
            );
            pb.redirectOutput(ProcessBuilder.Redirect.to(NULL_FILE));
            pb.redirectError(ProcessBuilder.Redirect.to(NULL_FILE));
            Process p = pb.start();
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                return true;
            }
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        } catch (Throwable t) {
            LOGGER.debug("Encoder probe failed for '{}': {}", encoder, t.getMessage());
        }
        return false;
    }

    /**
     * 実行可能なFFmpegバイナリのパスを検出する。
     * ユーザー設定パス、PATH環境変数、標準的なインストール先の順に探索する。
     *
     * @return 実行可能なFFmpegのフルパスまたはコマンド名。見つからない場合はnull
     */
    public static String getFfmpegPath() {
        String customPath = null;
        try {
            if (RecordConfigs.Generic.CUSTOM_FFMPEG_PATH != null) {
                customPath = RecordConfigs.Generic.CUSTOM_FFMPEG_PATH.getStringValue();
            }
        } catch (Throwable t) {
            LOGGER.debug("Could not read custom FFmpeg path from config: {}", t.getMessage());
        }

        if (customPath != null && !customPath.trim().isEmpty()) {
            String trimmed = customPath.trim();

            // 1. 指定パスが絶対パスまたは直接実行可能な場合
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
            } catch (Throwable t) {
                LOGGER.debug("Could not resolve relative FFmpeg path against runDirectory: {}", t.getMessage());
            }

            // 3. コマンド名としての実行可否チェック
            try {
                ProcessBuilder pb = new ProcessBuilder(trimmed, "-version");
                pb.redirectOutput(ProcessBuilder.Redirect.to(NULL_FILE));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return trimmed;
                }
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Throwable t) {
                LOGGER.debug("Custom FFmpeg command verification failed for '{}': {}", trimmed, t.getMessage());
            }
        }

        if (cachedFfmpegPath != null) {
            return cachedFfmpegPath;
        }

        // PATH環境変数を走査
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
                    } catch (Exception e) {
                        LOGGER.trace("Skipping inaccessible PATH directory '{}': {}", dir, e.getMessage());
                    }
                }
            }
        }

        // 各OSの一般的なインストール先を検索
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
                LOGGER.info("FFmpeg found at standard installation path: {}", cachedFfmpegPath);
                return cachedFfmpegPath;
            }
        }

        // 最後のフォールバックとして 'ffmpeg -version' を直接実行
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectOutput(ProcessBuilder.Redirect.to(NULL_FILE));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                cachedFfmpegPath = "ffmpeg";
                return cachedFfmpegPath;
            }
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            LOGGER.debug("System 'ffmpeg -version' command execution failed: {}", e.getMessage());
        }

        LOGGER.warn("FFmpeg executable was not found on the system");
        return null;
    }

    /**
     * 映像キャプチャ用のFFmpegエンコードプロセスを起動する。
     *
     * @param width          キャプチャ幅（偶数に調整済みであること）
     * @param height         キャプチャ高さ（偶数に調整済みであること）
     * @param fps            目標フレームレート
     * @param tempOutputFile 一時動画ファイルの保存先
     * @return 起動したProcessインスタンス
     * @throws IOException プロセス起動失敗時
     */
    public static Process startVideoEncodingProcess(int width, int height, int fps, File tempOutputFile) throws IOException {
        String ffmpeg = getFfmpegPath();
        if (ffmpeg == null) {
            throw new FileNotFoundException("FFmpeg executable not found on the system");
        }

        String encoder = getBestVideoEncoder();
        String preset = RecordConfigs.Generic.PRESET.getStringValue();
        int crf = RecordConfigs.Generic.CRF.getIntegerValue();

        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        command.add("-y");
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
        command.add("-"); // 標準入力からOpenGLのピクセルデータを受信
        command.add("-c:v");
        command.add(encoder);

        // 各エンコーダーに最適なパラメータを付与
        if ("libx264".equals(encoder)) {
            command.add("-preset");
            command.add(preset != null && !preset.isEmpty() ? preset : "ultrafast");
            command.add("-crf");
            command.add(String.valueOf(crf));
        } else if ("mpeg4".equals(encoder)) {
            // mpeg4はpreset/crfをサポートしないため、qscaleで品質を制御 (1〜31, 低いほど高品質)
            int qscale = Math.max(1, Math.min(31, (int) Math.round(crf * 31.0 / 51.0)));
            command.add("-qscale:v");
            command.add(String.valueOf(qscale));
        } else if (encoder.contains("nvenc")) {
            command.add("-preset");
            command.add("p1");
            command.add("-cq");
            command.add(String.valueOf(crf));
        }

        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-vf");
        command.add("vflip"); // OpenGLの原点(左下)と動画の原点(左上)の不一致を補正
        command.add("-an");   // 映像専用プロセス（音声は別録音してMux時に結合）
        command.add(tempOutputFile.getAbsolutePath());

        LOGGER.info("Starting FFmpeg video encoder (using {}): {}", encoder, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // FFmpegの標準エラー出力を読み取り、バッファ溢れによるパイプ詰まりを防止するスレッド
        Thread loggerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("[FFmpeg-Video] {}", line);
                }
            } catch (IOException e) {
                LOGGER.trace("FFmpeg video logger stream closed: {}", e.getMessage());
            }
        }, "RecordMC-FFmpeg-Logger");
        loggerThread.setDaemon(true);
        loggerThread.start();

        return process;
    }

    /**
     * 録画終了時に映像ストリームと音声ストリームを結合 (Mux) して完成動画ファイルを出力する。
     *
     * @param videoFile 一時動画ファイル
     * @param audioFile 一時音声ファイル (nullまたは存在しない場合は映像のみ出力)
     * @param finalFile 完成動画の保存先
     * @return 処理が正常に完了し、有効なファイルが生成された場合はtrue
     */
    public static boolean muxVideoAndAudio(File videoFile, File audioFile, File finalFile) {
        String ffmpeg = getFfmpegPath();
        if (ffmpeg == null) {
            LOGGER.error("Muxing failed: FFmpeg executable not found");
            return false;
        }
        if (videoFile == null || !videoFile.exists()) {
            LOGGER.error("Muxing failed: Temporary video file does not exist: {}", videoFile != null ? videoFile.getAbsolutePath() : "null");
            return false;
        }
        if (videoFile.length() == 0) {
            LOGGER.error("Muxing failed: Temporary video file is empty (0 bytes): {}", videoFile.getAbsolutePath());
            return false;
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        command.add("-y");
        command.add("-i");
        command.add(videoFile.getAbsolutePath());

        boolean hasAudio = audioFile != null && audioFile.exists() && audioFile.length() > 44; // 有効なWAVデータが存在するか
        if (hasAudio) {
            command.add("-i");
            command.add(audioFile.getAbsolutePath());
        }

        String fileNameLower = finalFile.getName().toLowerCase();
        boolean isWebm = fileNameLower.endsWith(".webm");
        boolean isMkv = fileNameLower.endsWith(".mkv");

        if (isWebm) {
            // WebM形式: VP9映像 + Opus音声
            int crf = 20;
            try {
                if (RecordConfigs.Generic.CRF != null) {
                    crf = RecordConfigs.Generic.CRF.getIntegerValue();
                }
            } catch (Throwable t) {
                LOGGER.debug("Could not read CRF config for WebM mux, using default 20: {}", t.getMessage());
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
            // MP4 / MOV / MKV: 映像ストリームは再エンコードなし（copy）で超高速結合
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
                // MP4 / MOV はWeb再生やプレビューの即時読み込みのため+faststartフラグを付与
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

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("[FFmpeg-Mux] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.error("FFmpeg mux process exited with non-zero code: {}", exitCode);
                return false;
            }

            boolean success = finalFile.exists() && finalFile.length() > 0;
            if (success) {
                LOGGER.info("Final video successfully created: {} ({} bytes)", finalFile.getAbsolutePath(), finalFile.length());
            } else {
                LOGGER.error("FFmpeg mux exited with code 0 but final file is missing or empty: {}", finalFile.getAbsolutePath());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Failed to mux video and audio streams into final container", e);
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
