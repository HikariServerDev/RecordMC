package com.atsukimc.recordmc;

import com.atsukimc.recordmc.config.RecordConfigs;
import com.atsukimc.recordmc.recorder.AudioRecorder;
import com.atsukimc.recordmc.recorder.ClipboardHelper;
import com.atsukimc.recordmc.recorder.FFmpegManager;
import com.atsukimc.recordmc.recorder.GameAudioMixer;
import com.atsukimc.recordmc.recorder.PcmSoundData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class RecordMCTest {

    @Test
    public void testFFmpegPathDetection() {
        String ffmpegPath = FFmpegManager.getFfmpegPath();
        assertNotNull(ffmpegPath, "FFmpeg path should be found on the system");
        System.out.println("Detected FFmpeg at: " + ffmpegPath);
    }

    @Test
    public void testFileNameFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH-mm-ss");
        String formatted = LocalDateTime.now().format(formatter) + ".mp4";

        Pattern pattern = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{2}-\\d{2}-\\d{2}\\.mp4$");
        assertTrue(pattern.matcher(formatted).matches(), "Filename must match yyyy.mm.dd.hh-mm-ss.mp4 format");
    }

    @Test
    public void testEvenResolutionAlignment() {
        int oddWidth = 1921;
        int oddHeight = 1081;
        int alignedWidth = oddWidth & ~1;
        int alignedHeight = oddHeight & ~1;

        assertEquals(1920, alignedWidth);
        assertEquals(1080, alignedHeight);
        assertEquals(0, alignedWidth % 2);
        assertEquals(0, alignedHeight % 2);
    }

    @Test
    public void testConfigDefaults() {
        assertEquals(60, RecordConfigs.Generic.FPS.getIntegerValue());
        assertEquals(20, RecordConfigs.Generic.CRF.getIntegerValue());
        assertEquals("ultrafast", RecordConfigs.Generic.PRESET.getStringValue());
        assertEquals("mp4", RecordConfigs.Generic.VIDEO_FORMAT.getStringValue());
        assertTrue(RecordConfigs.Generic.RECORD_AUDIO.getBooleanValue());
        assertTrue(RecordConfigs.Generic.SHOW_INDICATOR.getBooleanValue());
        assertTrue(RecordConfigs.Generic.COPY_TO_CLIPBOARD.getBooleanValue());
        assertEquals("H,R", RecordConfigs.Hotkeys.OPEN_GUI.getKeybind().getStringValue());
        assertEquals("F9", RecordConfigs.Hotkeys.TOGGLE_RECORDING.getKeybind().getStringValue());
    }

    @Test
    public void testEncodingPresetCycling() {
        com.atsukimc.recordmc.config.EncodingPreset preset = com.atsukimc.recordmc.config.EncodingPreset.ULTRAFAST;
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.SUPERFAST, preset);
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.VERYFAST, preset);
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.FASTER, preset);
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.FAST, preset);
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.MEDIUM, preset);
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.ULTRAFAST, preset);

        // 逆順
        preset = (com.atsukimc.recordmc.config.EncodingPreset) preset.cycle(false);
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.MEDIUM, preset);

        // 文字列復元
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.FAST, com.atsukimc.recordmc.config.EncodingPreset.fromStringStatic("fast"));
        assertEquals(com.atsukimc.recordmc.config.EncodingPreset.ULTRAFAST, com.atsukimc.recordmc.config.EncodingPreset.fromStringStatic("unknown_foo"));
    }

    @Test
    public void testVideoFormatCycling() {
        com.atsukimc.recordmc.config.VideoFormat format = com.atsukimc.recordmc.config.VideoFormat.MP4;
        format = (com.atsukimc.recordmc.config.VideoFormat) format.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.MOV, format);
        format = (com.atsukimc.recordmc.config.VideoFormat) format.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.MKV, format);
        format = (com.atsukimc.recordmc.config.VideoFormat) format.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.WEBM, format);
        format = (com.atsukimc.recordmc.config.VideoFormat) format.cycle(true);
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.MP4, format);

        // 逆順
        format = (com.atsukimc.recordmc.config.VideoFormat) format.cycle(false);
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.WEBM, format);

        // 拡張子
        assertEquals(".mp4", com.atsukimc.recordmc.config.VideoFormat.MP4.getExtension());
        assertEquals(".mov", com.atsukimc.recordmc.config.VideoFormat.MOV.getExtension());
        assertEquals(".mkv", com.atsukimc.recordmc.config.VideoFormat.MKV.getExtension());
        assertEquals(".webm", com.atsukimc.recordmc.config.VideoFormat.WEBM.getExtension());

        // 文字列復元
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.MOV, com.atsukimc.recordmc.config.VideoFormat.fromStringStatic("mov"));
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.MKV, com.atsukimc.recordmc.config.VideoFormat.fromStringStatic(".mkv"));
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.WEBM, com.atsukimc.recordmc.config.VideoFormat.fromStringStatic("webm"));
        assertEquals(com.atsukimc.recordmc.config.VideoFormat.MP4, com.atsukimc.recordmc.config.VideoFormat.fromStringStatic("invalid_xyz"));
    }

    @Test
    public void testEffectiveVideoDirResolution() {
        com.atsukimc.recordmc.recorder.ScreenRecorder recorder = com.atsukimc.recordmc.recorder.ScreenRecorder.getInstance();
        File defaultDir = recorder.getEffectiveVideoDir();
        assertNotNull(defaultDir);
        assertTrue(defaultDir.getPath().endsWith("video"));

        // 相対パス設定テスト
        RecordConfigs.Generic.CUSTOM_VIDEO_DIR.setValueFromString("my_recordings");
        File customRelative = recorder.getEffectiveVideoDir();
        assertTrue(customRelative.getPath().endsWith("my_recordings"));

        // リセット
        RecordConfigs.Generic.CUSTOM_VIDEO_DIR.resetToDefault();
        assertEquals("", RecordConfigs.Generic.CUSTOM_VIDEO_DIR.getStringValue());
    }

    @Test
    public void testPcmSoundDataSampling() {
        short[] monoSamples = new short[]{0, 16384, 32767, 0, -32768};
        PcmSoundData monoData = new PcmSoundData(44100, 1, monoSamples);

        assertEquals(0.0f, monoData.getSample(0.0, 0), 0.001f);
        assertEquals(0.5f, monoData.getSample(1.0, 0), 0.01f);
        assertEquals(1.0f, monoData.getSample(2.0, 0), 0.01f);
        assertEquals(-1.0f, monoData.getSample(4.0, 0), 0.01f);
        // 補間テスト
        assertEquals(0.25f, monoData.getSample(0.5, 0), 0.02f);
    }

    @Test
    public void testGameAudioMixerRendering() {
        GameAudioMixer mixer = GameAudioMixer.getInstance();
        mixer.start();

        byte[] outBytes = new byte[882 * 4];
        mixer.renderSamples(882, outBytes);

        // 無音の場合すべて0
        for (byte b : outBytes) {
            assertEquals(0, b);
        }

        mixer.stop();
    }

    @Test
    public void testAudioRecorderWavLifecycle(@TempDir Path tempDir) throws Exception {
        File wavFile = tempDir.resolve("test_game_audio.wav").toFile();
        AudioRecorder recorder = new AudioRecorder(wavFile);

        assertTrue(recorder.start(), "Audio recorder should start");
        Thread.sleep(100);
        recorder.stop();

        assertTrue(wavFile.exists(), "WAV file should exist");
        assertTrue(wavFile.length() >= 44, "WAV file should have at least header size (44 bytes)");
    }

    @Test
    public void testClipboardHelper(@TempDir Path tempDir) throws Exception {
        File dummyFile = tempDir.resolve("dummy_video.mp4").toFile();
        dummyFile.createNewFile();

        // クリップボードコピー処理の実行テスト（例外が発生しないこと）
        assertDoesNotThrow(() -> {
            ClipboardHelper.copyFileToClipboard(dummyFile);
        });
    }

    @Test
    public void testFFmpegVideoEncodingAndMux(@TempDir Path tempDir) throws Exception {
        String ffmpeg = FFmpegManager.getFfmpegPath();
        if (ffmpeg == null) {
            return;
        }

        File tempVideo = tempDir.resolve("test_temp_video.mp4").toFile();
        File tempAudio = tempDir.resolve("test_temp_audio.wav").toFile();
        File finalOutput = tempDir.resolve("test_output.mp4").toFile();

        int width = 320;
        int height = 240;
        int fps = 30;

        // 1. テスト用動画フレーム（赤色 10フレーム）をFFmpegプロセスへ書き込み
        Process videoProc = FFmpegManager.startVideoEncodingProcess(width, height, fps, tempVideo);
        OutputStream stdin = videoProc.getOutputStream();
        byte[] frame = new byte[width * height * 4];
        for (int i = 0; i < frame.length; i += 4) {
            frame[i] = (byte) 255;   // R
            frame[i + 1] = 0;        // G
            frame[i + 2] = 0;        // B
            frame[i + 3] = (byte) 255; // A
        }

        for (int f = 0; f < 10; f++) {
            stdin.write(frame);
        }
        stdin.flush();
        stdin.close();
        assertTrue(videoProc.waitFor(10, TimeUnit.SECONDS), "Video encoder should finish");
        assertTrue(tempVideo.exists() && tempVideo.length() > 0, "Temp video file must exist and be non-empty");

        // 2. AudioRecorder でテストWAVを生成
        AudioRecorder audioRecorder = new AudioRecorder(tempAudio);
        assertTrue(audioRecorder.start());
        Thread.sleep(200);
        audioRecorder.stop();
        assertTrue(tempAudio.exists() && tempAudio.length() > 44, "Audio WAV file must be valid");

        // 3. Mux実行 (mp4, mov, mkv, webm の全フォーマットをテスト)
        String[] formats = new String[]{"test_output.mp4", "test_output.mov", "test_output.mkv", "test_output.webm"};
        for (String fmtName : formats) {
            File outFile = tempDir.resolve(fmtName).toFile();
            boolean muxSuccess = FFmpegManager.muxVideoAndAudio(tempVideo, tempAudio, outFile);
            assertTrue(muxSuccess, "Muxing to " + fmtName + " should succeed");
            assertTrue(outFile.exists() && outFile.length() > 0, "Output file " + fmtName + " must exist and be non-empty");
            System.out.println("Test generated " + fmtName + " successfully: " + outFile.length() + " bytes");
        }
    }

    @Test
    public void testLanguageFiles() throws Exception {
        String[] supportedLanguages = new String[]{
                "ja_jp", "en_us", "zh_cn", "zh_tw", "ko_kr", "ru_ru",
                "de_de", "fr_fr", "es_es", "es_mx", "pt_br", "it_it", "pl_pl", "uk_ua"
        };

        com.google.gson.Gson gson = new com.google.gson.Gson();

        for (String lang : supportedLanguages) {
            String path = "/assets/recordmc/lang/" + lang + ".json";
            InputStream in = getClass().getResourceAsStream(path);
            assertNotNull(in, "Language file must exist: " + path);

            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            com.google.gson.JsonObject json = gson.fromJson(reader, com.google.gson.JsonObject.class);
            assertNotNull(json, "Language JSON must be valid: " + path);

            // 必須キーの存在チェック
            assertTrue(json.has("category.recordmc.title"), "Missing title in " + lang);
            assertTrue(json.has("recordmc.gui.title.config"), "Missing config title in " + lang);
            assertTrue(json.has("recordmc.gui.tab.generic"), "Missing generic tab in " + lang);
            assertTrue(json.has("recordmc.gui.tab.hotkeys"), "Missing hotkeys tab in " + lang);
            assertTrue(json.has("config.name.fps"), "Missing fps name in " + lang);
            assertTrue(json.has("config.name.crf"), "Missing crf name in " + lang);
            assertTrue(json.has("config.name.preset"), "Missing preset name in " + lang);
            assertTrue(json.has("config.name.videoformat"), "Missing videoformat name in " + lang);
            assertTrue(json.has("config.name.recordaudio"), "Missing recordaudio name in " + lang);
            assertTrue(json.has("config.name.showindicator"), "Missing showindicator name in " + lang);
            assertTrue(json.has("config.name.copytoclipboard"), "Missing copytoclipboard name in " + lang);
            assertTrue(json.has("config.name.opengui"), "Missing opengui name in " + lang);
            assertTrue(json.has("config.name.togglerecording"), "Missing togglerecording name in " + lang);
            assertTrue(json.has("recordmc.message.recording_started"), "Missing started message in " + lang);
            assertTrue(json.has("recordmc.message.recording_saved"), "Missing saved message in " + lang);
        }
        System.out.println("Verified " + supportedLanguages.length + " language files successfully.");
    }
}
