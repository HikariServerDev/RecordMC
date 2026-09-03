package com.atsukimc.recordmc.gui;

import com.atsukimc.recordmc.config.RecordConfigHandler;
import com.atsukimc.recordmc.config.RecordConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.StringRenderable;
import net.minecraft.text.Style;

import java.util.List;

/**
 * TweakerooスタイルのRecordMC設定GUI画面（レスポンシブ・小ウィンドウ対応）
 */
public class RecordConfigGui extends GuiConfigsBase {
    private static ConfigGuiTab currentTab = ConfigGuiTab.GENERIC;

    public RecordConfigGui() {
        this(null);
    }

    public RecordConfigGui(Screen parent) {
        super(10, 50, "recordmc", parent, I18n.translate("recordmc.gui.title.config"));
        this.setParent(parent);
        this.setHoverInfoProvider(this::getWrappedHoverInfo);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createTabButton(x, y, -1, tab);
        }
    }

    private int createTabButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(currentTab != tab);
        this.addButton(button, new TabButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    /**
     * 小ウィンドウ時にもはみ出さないよう、現在の画面幅とラベル幅からコントロール幅を動的に計算する
     */
    @Override
    protected int getConfigWidth() {
        int browserWidth = this.getBrowserWidth();
        int maxLabel = this.getMaxLabelWidth();
        // 余白: 左余白(10) + 間隔(10) + リセットボタン(約45) + スクロールバー・右マージン(30) = 約95px
        int available = browserWidth - maxLabel - 95;

        int target = currentTab == ConfigGuiTab.HOTKEYS ? 180 : 200;
        return Math.max(70, Math.min(target, available));
    }

    private int getMaxLabelWidth() {
        MinecraftClient mc = this.client != null ? this.client : MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return 130;
        }

        int max = 0;
        for (ConfigOptionWrapper wrapper : this.getConfigs()) {
            if (wrapper.getType() == ConfigOptionWrapper.Type.CONFIG) {
                String label = wrapper.getConfig().getConfigGuiDisplayName();
                int w = mc.textRenderer.getWidth(label);
                if (w > max) {
                    max = w;
                }
            }
        }
        return max > 0 ? max : 130;
    }

    /**
     * ホバーツールチップのテキストを画面幅に合わせて自動折り返し（Word Wrap）する
     */
    private String getWrappedHoverInfo(IConfigBase config) {
        String comment = config.getComment();
        if (comment == null || comment.trim().isEmpty()) {
            return null;
        }

        MinecraftClient mc = this.client != null ? this.client : MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return comment;
        }

        // ツールチップが画面端を突き抜けないよう最大幅を計算（画面幅 - 40px、最大280px）
        int maxTooltipWidth = Math.max(160, Math.min(280, this.width - 40));

        StringBuilder sb = new StringBuilder();
        String[] originalLines = comment.split("\n");
        boolean first = true;

        for (String line : originalLines) {
            if (line.isEmpty()) {
                sb.append("\n");
                continue;
            }

            List<StringRenderable> wrappedLines = mc.textRenderer.getTextHandler().wrapLines(line, maxTooltipWidth, Style.EMPTY);
            for (StringRenderable wrapped : wrappedLines) {
                if (!first) {
                    sb.append("\n");
                }
                sb.append(wrapped.getString());
                first = false;
            }
        }

        return sb.toString();
    }

    @Override
    protected boolean useKeybindSearch() {
        return currentTab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends fi.dy.masa.malilib.config.IConfigBase> options;
        if (currentTab == ConfigGuiTab.GENERIC) {
            options = RecordConfigs.getGenericOptions();
        } else {
            options = RecordConfigs.getHotkeyOptions();
        }

        return ConfigOptionWrapper.createFor(options);
    }

    @Override
    public void removed() {
        super.removed();
        RecordConfigHandler.getInstance().save();
    }

    @Override
    protected void onSettingsChanged() {
        super.onSettingsChanged();
        RecordConfigHandler.getInstance().save();
    }

    private static class TabButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final RecordConfigGui gui;

        public TabButtonListener(ConfigGuiTab tab, RecordConfigGui gui) {
            this.tab = tab;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            currentTab = this.tab;
            this.gui.reCreateListWidget();
            if (this.gui.getListWidget() != null) {
                this.gui.getListWidget().resetScrollbarPosition();
            }
            this.gui.initGui();
        }
    }

    public enum ConfigGuiTab {
        GENERIC("recordmc.gui.tab.generic"),
        HOTKEYS("recordmc.gui.tab.hotkeys");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return I18n.translate(this.translationKey);
        }
    }
}
