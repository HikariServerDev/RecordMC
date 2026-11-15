package com.atsukimc.recordmc.compat;

//? if >=26.1 {
/*import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
*///?} else {
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;
//?}

/**
 * 効果音再生のバージョン差異（<1.19: SoundEvent 直渡し, >=1.19: SoundEvent.value(), >=26.1: LocalPlayer/NOTE_BLOCK_PLING）を吸収するファサード
 */
public final class SoundCompat {
    private SoundCompat() {}

    //? if >=26.1 {
    /*public static void playPling(LocalPlayer player, float pitch) {
        if (player == null) {
            return;
        }
        player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, pitch);
    }
    *///?} elif >=1.19 {
    /*public static void playPling(ClientPlayerEntity player, float pitch) {
        if (player == null) {
            return;
        }
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, pitch);
    }
    *///?} else {
    public static void playPling(ClientPlayerEntity player, float pitch) {
        if (player == null) {
            return;
        }
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
    }
    //?}
}
