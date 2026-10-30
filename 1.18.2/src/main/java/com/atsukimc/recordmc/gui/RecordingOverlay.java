package com.atsukimc.recordmc.gui;

import com.atsukimc.recordmc.config.RecordConfigs;
import com.atsukimc.recordmc.recorder.ScreenRecorder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * 録画中に画面右上に赤丸インジケーターと経過時間を描画するオーバーレイ
 */
public class RecordingOverlay {

    /**
     * 録画インジケーターを描画する
     *
     * @param matrices 描画用マトリクススタック
     */
    public static void render(MatrixStack matrices) {
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

        // 経過時間の計算 (mm:ss または hh:mm:ss)
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
        int boxWidth = textWidth + 24; // 赤丸アイコン分 + マージン
        int boxHeight = textRenderer.fontHeight + padding * 2;

        int x = scaledWidth - boxWidth - 10;
        int y = 10;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 半透明黒背景の描画
        DrawableHelper.fill(matrices, x, y, x + boxWidth, y + boxHeight, 0x80000000);

        // 赤丸の点滅（1秒周期: 500ms 点灯 / 500ms 消灯）
        boolean blinkOn = (elapsedMillis % 1000) < 600;
        int dotColor = blinkOn ? 0xFFFF2222 : 0x66AA0000;

        // 赤丸アイコンの描画（3x3または4x4の正方形/ドット）
        int dotX = x + padding + 3;
        int dotY = y + (boxHeight - 6) / 2;
        DrawableHelper.fill(matrices, dotX, dotY, dotX + 6, dotY + 6, dotColor);
        // 内側のハイライト
        if (blinkOn) {
            DrawableHelper.fill(matrices, dotX + 1, dotY + 1, dotX + 5, dotY + 5, 0xFFFF5555);
        }

        // テキストの描画
        textRenderer.drawWithShadow(matrices, label, dotX + 10, y + padding + 1, 0xFFFFFFFF);

        RenderSystem.disableBlend();
    }
}
