package com.atsukimc.recordmc.mixin;

import com.atsukimc.recordmc.recorder.ScreenRecorder;
import net.minecraft.client.render.GameRenderer;
//? if >=1.21.6 {
/*import net.minecraft.client.render.RenderTickCounter;
*///?} else {
import com.atsukimc.recordmc.gui.RecordingOverlay;
//? if <1.20 {
import net.minecraft.client.util.math.MatrixStack;
//?}
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 画面レンダリングの末尾にフックし、インジケーター描画とフレームキャプチャを実行するMixin
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    //? if >=1.21.6 {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ScreenRecorder recorder = ScreenRecorder.getInstance();
        if (recorder.isRecording()) {
            recorder.onRenderFrame();
        }
    }
    *///?} elif >=1.20 {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        ScreenRecorder recorder = ScreenRecorder.getInstance();
        if (recorder.isRecording()) {
            recorder.onRenderFrame();
            RecordingOverlay.render();
        }
    }
    *///?} else {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        ScreenRecorder recorder = ScreenRecorder.getInstance();
        if (recorder.isRecording()) {
            recorder.onRenderFrame();
            MatrixStack matrices = new MatrixStack();
            RecordingOverlay.render(matrices);
        }
    }
    //?}
}
