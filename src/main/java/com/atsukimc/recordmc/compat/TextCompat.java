package com.atsukimc.recordmc.compat;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
//? if <1.19 {
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
//?}

import java.io.File;

/**
 * Text 生成のバージョン差異（<1.19: LiteralText/TranslatableText, >=1.19: Text.literal/Text.translatable, ClickEvent等）を吸収するファサード
 */
public final class TextCompat {
    private TextCompat() {}

    public static MutableText literal(String text) {
        //? if >=1.19 {
        /*return Text.literal(text);
        *///?} else {
        return new LiteralText(text);
        //?}
    }

    public static MutableText translatable(String key, Object... args) {
        //? if >=1.19 {
        /*return Text.translatable(key, args);
        *///?} else {
        return new TranslatableText(key, args);
        //?}
    }

    public static ClickEvent createOpenFileClickEvent(File file) {
        //? if >=1.21.5 {
        /*return new ClickEvent.OpenFile(file);
        *///?} else {
        return new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath());
        //?}
    }
}
