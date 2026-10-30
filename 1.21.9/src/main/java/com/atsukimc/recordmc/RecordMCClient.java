package com.atsukimc.recordmc;

import com.atsukimc.recordmc.gui.RecordingOverlay;
import com.atsukimc.recordmc.input.InputHandler;
import com.atsukimc.recordmc.recorder.ScreenRecorder;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RecordMCClient implements ClientModInitializer {
    public static final String MOD_ID = "recordmc";
    public static final Logger LOGGER = LogManager.getLogger("RecordMC");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing RecordMC by AtsukiMC...");

        // Malilib の初期化ハンドラーを登録
        InitializationHandler.getInstance().registerInitializationHandler(InputHandler.getInstance());

        // ゲーム終了時の安全停止
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (ScreenRecorder.getInstance().isRecording()) {
                LOGGER.info("Game closing while recording, stopping recording...");
                ScreenRecorder.getInstance().toggleRecording();
            }
        });

        // HUD オーバーレイ描画
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            RecordingOverlay.render(context);
        });

        LOGGER.info("RecordMC initialized successfully with Malilib support.");
    }
}
