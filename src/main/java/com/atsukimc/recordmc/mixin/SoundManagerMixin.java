package com.atsukimc.recordmc.mixin;

import com.atsukimc.recordmc.recorder.GameAudioMixer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SoundManager初期化時にゲーム音声ミキサーをリスナーとして登録するMixin
 */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow
    public abstract void registerListener(SoundInstanceListener listener);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ResourceManager resourceManager, GameOptions gameOptions, CallbackInfo ci) {
        this.registerListener(GameAudioMixer.getInstance());
    }
}
