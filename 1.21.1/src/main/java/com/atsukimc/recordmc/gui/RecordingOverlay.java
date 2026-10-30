package com.atsukimc.recordmc.gui;

import com.atsukimc.recordmc.config.RecordConfigs;
import com.atsukimc.recordmc.recorder.ScreenRecorder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * 録画中に画面右上に赤丸インジケーターと経過時間を描画するオーバーレイ (1.20+ DrawContext版)
 */
public class RecordingOverlay {

    public static void render() {
        ScreenRecorder recorder = ScreenRecorder.getInstance();
        if (!recorder.isRecording() || !RecordConfigs.Generic.SHOW_INDICATOR.getBooleanValue()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        int scaledWidth = client.getWindow().getScaledWidth();
        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - recorder.getRecordingStartTime();
        long totalSeconds = Math.max(0, elapsedMillis / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String timeStr;
        if (hours > 0) {
            timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeStr = String.format("%02d:%02d", minutes, seconds);
        }

        String label = "REC  " + timeStr;
        int textWidth = textRenderer.getWidth(label);
        int padding = 4;
        int boxWidth = textWidth + 24;
        int boxHeight = textRenderer.fontHeight + padding * 2;

        int x = scaledWidth - boxWidth - 10;
        int y = 10;

        DrawContext context = new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 半透明黒背景
        context.fill(x, y, x + boxWidth, y + boxHeight, 0x80000000);

        // 赤丸点滅
        boolean blinkOn = (elapsedMillis % 1000) < 600;
        int dotColor = blinkOn ? 0xFFFF2222 : 0x66AA0000;

        int dotX = x + padding + 3;
        int dotY = y + (boxHeight - 6) / 2;
        context.fill(dotX, dotY, dotX + 6, dotY + 6, dotColor);
        if (blinkOn) {
            context.fill(dotX + 1, dotY + 1, dotX + 5, dotY + 5, 0xFFFF5555);
        }

        // テキスト
        context.drawTextWithShadow(textRenderer, label, dotX + 10, y + padding + 1, 0xFFFFFFFF);

        RenderSystem.disableBlend();
    }
}
