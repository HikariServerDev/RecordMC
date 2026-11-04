package com.atsukimc.recordmc.compat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;

/**
 * 効果音再生のバージョン差異（<1.19: SoundEvent 直渡し, >=1.19: SoundEvent.value()）を吸収するファサード
 */
public final class SoundCompat {
    private SoundCompat() {}

    public static void playPling(ClientPlayerEntity player, float pitch) {
        if (player == null) {
            return;
        }
        //? if >=1.19 {
        /*player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, pitch);
        *///?} else {
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
        //?}
    }
}
