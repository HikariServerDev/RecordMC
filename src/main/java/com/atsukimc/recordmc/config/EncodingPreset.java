package com.atsukimc.recordmc.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.client.resource.language.I18n;

/**
 * FFmpegのエンコードプリセット（ボタンクリックで循環切り替え）
 */
public enum EncodingPreset implements IConfigOptionListEntry {
    ULTRAFAST("ultrafast", "recordmc.config.preset.ultrafast", "ultrafast (最速・超軽量)"),
    SUPERFAST("superfast", "recordmc.config.preset.superfast", "superfast (高速)"),
    VERYFAST("veryfast", "recordmc.config.preset.veryfast", "veryfast (準高速)"),
    FASTER("faster", "recordmc.config.preset.faster", "faster (高圧縮寄り)"),
    FAST("fast", "recordmc.config.preset.fast", "fast (高圧縮)"),
    MEDIUM("medium", "recordmc.config.preset.medium", "medium (最高圧縮・負荷高)");

    private final String configString;
    private final String translationKey;
    private final String defaultDisplayName;

    EncodingPreset(String configString, String translationKey, String defaultDisplayName) {
        this.configString = configString;
        this.translationKey = translationKey;
        this.defaultDisplayName = defaultDisplayName;
    }

    @Override
    public String getStringValue() {
        return this.configString;
    }

    @Override
    public String getDisplayName() {
        if (I18n.hasTranslation(this.translationKey)) {
            return I18n.translate(this.translationKey);
        }
        return this.defaultDisplayName;
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

    public static EncodingPreset fromStringStatic(String value) {
        if (value == null) {
            return ULTRAFAST;
        }
        for (EncodingPreset preset : values()) {
            if (preset.configString.equalsIgnoreCase(value.trim())) {
                return preset;
            }
        }
        return ULTRAFAST;
    }
}
