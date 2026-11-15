package com.atsukimc.recordmc.compat;

//? if >=26.1 {
/*import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
*///?} else {
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
//? if <1.19 {
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
//?}
//?}

import java.io.File;

/**
 * Text 生成のバージョン差異（<1.19: LiteralText/TranslatableText, >=1.19: Text.literal/Text.translatable, ClickEvent等, >=26.1: Component）を吸収するファサード
 */
public final class TextCompat {
    private TextCompat() {}

    //? if >=26.1 {
    /*public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }
    *///?} elif >=1.19 {
    /*public static MutableText literal(String text) {
        return Text.literal(text);
    }

    public static MutableText translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
    *///?} else {
    public static MutableText literal(String text) {
        return new LiteralText(text);
    }

    public static MutableText translatable(String key, Object... args) {
        return new TranslatableText(key, args);
    }
    //?}

    public static ClickEvent createOpenFileClickEvent(File file) {
        //? if >=1.21.5 {
        /*return new ClickEvent.OpenFile(file);
        *///?} else {
        return new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath());
        //?}
    }
}
