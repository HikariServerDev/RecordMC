package com.atsukimc.recordmc.compat;

//? if >=26.1 {
/*import net.minecraft.client.resources.language.I18n;
*///?} else {
import net.minecraft.client.resource.language.I18n;
//?}

/**
 * 国際化（I18n）APIのバージョン差異を吸収する互換ヘルパー
 * （<=1.21.11: client.resource.language.I18n, >=26.1: client.resources.language.I18n）
 */
public final class I18nCompat {
    private I18nCompat() {}

    /**
     * 指定されたキーの翻訳が存在するか確認する
     */
    public static boolean hasTranslation(String key) {
        //? if >=26.1 {
        /*return net.minecraft.locale.Language.getInstance().has(key);
        *///?} else {
        return I18n.hasTranslation(key);
        //?}
    }

    /**
     * 指定されたキーの翻訳文字列を取得する
     */
    public static String translate(String key, Object... args) {
        //? if >=26.1 {
        /*return I18n.get(key, args);
        *///?} else {
        return I18n.translate(key, args);
        //?}
    }
}
