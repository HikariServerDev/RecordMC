package com.atsukimc.recordmc.mixin;

import com.atsukimc.recordmc.gui.RecordingOverlay;
import com.atsukimc.recordmc.recorder.ScreenRecorder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 画面レンダリングの末尾にフックし、インジケーター描画とフレームキャプチャを実行するMixin
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        ScreenRecorder recorder = ScreenRecorder.getInstance();
        if (recorder.isRecording()) {
            // フレームバッファの内容をキャプチャ（インジケーター描画前に実行）
            recorder.onRenderFrame();

            // 右上に録画中インジケーターを描画（動画には含まれない）
            MatrixStack matrices = new MatrixStack();
            RecordingOverlay.render(matrices);
        }
    }
}
