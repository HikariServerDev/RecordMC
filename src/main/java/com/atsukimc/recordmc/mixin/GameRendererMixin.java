package com.atsukimc.recordmc.mixin;

import com.atsukimc.recordmc.recorder.ScreenRecorder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ScreenRecorder recorder = ScreenRecorder.getInstance();
        if (recorder.isRecording()) {
            recorder.onRenderFrame();
        }
    }
}
