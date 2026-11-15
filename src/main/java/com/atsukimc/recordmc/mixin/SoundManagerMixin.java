package com.atsukimc.recordmc.mixin;

import com.atsukimc.recordmc.recorder.GameAudioMixer;
//? if >=26.1 {
/*import net.minecraft.client.Options;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
*///?} elif <=1.16.1 {
/*import net.minecraft.client.options.GameOptions;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resource.ResourceManager;
*///?} else {
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resource.ResourceManager;
//?}
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

    //? if >=26.1 {
    /*@Shadow
    public abstract void addListener(SoundEventListener listener);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Options options, CallbackInfo ci) {
        this.addListener(GameAudioMixer.getInstance());
    }
    *///?} else {
    @Shadow
    public abstract void registerListener(SoundInstanceListener listener);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ResourceManager resourceManager, GameOptions gameOptions, CallbackInfo ci) {
        this.registerListener(GameAudioMixer.getInstance());
    }
    //?}
}
