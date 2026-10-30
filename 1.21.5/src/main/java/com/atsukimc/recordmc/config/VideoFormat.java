package com.atsukimc.recordmc.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 出力動画のフォーマット（コンテナ形式）
 */
public enum VideoFormat implements IConfigOptionListEntry {
    MP4("mp4", ".mp4", "MP4 (.mp4)"),
    MOV("mov", ".mov", "MOV (.mov)"),
    MKV("mkv", ".mkv", "MKV (.mkv)"),
    WEBM("webm", ".webm", "WebM (.webm)");

    private final String configString;
    private final String extension;
    private final String displayName;

    VideoFormat(String configString, String extension, String displayName) {
        this.configString = configString;
        this.extension = extension;
        this.displayName = displayName;
    }

    @Override
    public String getStringValue() {
        return this.configString;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public String getExtension() {
        return this.extension;
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int id = this.ordinal();
        if (forward) {
            if (++id >= values().length) {
                id = 0;
            }
        } else {
            if (--id < 0) {
                id = values().length - 1;
            }
        }
        return values()[id];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        return fromStringStatic(value);
    }

    public static VideoFormat fromStringStatic(String value) {
        if (value == null) {
            return MP4;
        }
        String clean = value.trim().toLowerCase();
        if (clean.startsWith(".")) {
            clean = clean.substring(1);
        }
        for (VideoFormat format : values()) {
            if (format.configString.equalsIgnoreCase(clean)) {
                return format;
            }
        }
        return MP4;
    }
}
