package com.atsukimc.recordmc.config;

import com.atsukimc.recordmc.recorder.FFmpegManager;
import com.atsukimc.recordmc.recorder.ScreenRecorder;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.*;
import net.minecraft.client.resource.language.I18n;

import java.io.File;
import java.util.List;

/**
 * RecordMCのMalilib設定定義クラス
 */
public class RecordConfigs {

    public static class Generic {
        public static final ConfigInteger FPS = new ConfigInteger("fps", 60, 1, 240, "recordmc.config.comment.fps");
        public static final ConfigInteger CRF = new ConfigInteger("crf", 20, 0, 51, "recordmc.config.comment.crf");
        public static final ConfigOptionList PRESET = new ConfigOptionList("preset", EncodingPreset.ULTRAFAST, "recordmc.config.comment.preset");
        public static final ConfigOptionList VIDEO_FORMAT = new ConfigOptionList("videoFormat", VideoFormat.MP4, "recordmc.config.comment.video_format");
        public static final ConfigBoolean RECORD_AUDIO = new ConfigBoolean("recordAudio", true, "recordmc.config.comment.record_audio");
        public static final ConfigBoolean SHOW_INDICATOR = new ConfigBoolean("showIndicator", true, "recordmc.config.comment.show_indicator");
        public static final ConfigBoolean COPY_TO_CLIPBOARD = new ConfigBoolean("copyToClipboard", true, "recordmc.config.comment.copy_to_clipboard");

        // カスタムFFmpegパス（空欄で自動検出。ホバーで現在の有効パスを表示）
        public static final ConfigString CUSTOM_FFMPEG_PATH = new ConfigString("customFfmpegPath", "", "recordmc.config.comment.custom_ffmpeg_path") {
            @Override
            public String getComment() {
                String baseComment = super.getComment();
                String detected = FFmpegManager.getFfmpegPath();
                String effective = (detected != null) ? detected : I18n.translate("recordmc.config.status.not_detected");
                String hint = I18n.translate("recordmc.config.comment.custom_ffmpeg_path.current", effective);
                return baseComment + "\n" + hint;
            }
        };

        // カスタム保存先ディレクトリ（空欄で ./video。ホバーで現在の有効パスを表示）
        public static final ConfigString CUSTOM_VIDEO_DIR = new ConfigString("customVideoDir", "", "recordmc.config.comment.custom_video_dir") {
            @Override
            public String getComment() {
                String baseComment = super.getComment();
                File effectiveDir = ScreenRecorder.getInstance().getEffectiveVideoDir();
                String pathStr = (effectiveDir != null) ? effectiveDir.getAbsolutePath() : "./video";
                String hint = I18n.translate("recordmc.config.comment.custom_video_dir.current", pathStr);
                return baseComment + "\n" + hint;
            }
        };

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FPS,
                CRF,
                PRESET,
                VIDEO_FORMAT,
                RECORD_AUDIO,
                SHOW_INDICATOR,
                COPY_TO_CLIPBOARD,
                CUSTOM_FFMPEG_PATH,
                CUSTOM_VIDEO_DIR
        );
    }

    public static class Hotkeys {
        public static final ConfigHotkey OPEN_GUI = new ConfigHotkey("openGui", "H,R", "recordmc.config.comment.open_gui");
        public static final ConfigHotkey TOGGLE_RECORDING = new ConfigHotkey("toggleRecording", "F9", "recordmc.config.comment.toggle_recording");

        public static final ImmutableList<ConfigHotkey> HOTKEYS = ImmutableList.of(
                OPEN_GUI,
                TOGGLE_RECORDING
        );
    }

    public static List<IConfigBase> getGenericOptions() {
        return Generic.OPTIONS;
    }

    public static List<ConfigHotkey> getHotkeyOptions() {
        return Hotkeys.HOTKEYS;
    }
}
