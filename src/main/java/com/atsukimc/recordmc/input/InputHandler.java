package com.atsukimc.recordmc.input;

import com.atsukimc.recordmc.RecordMCClient;
import com.atsukimc.recordmc.config.RecordConfigHandler;
import com.atsukimc.recordmc.config.RecordConfigs;
import com.atsukimc.recordmc.gui.RecordConfigGui;
import com.atsukimc.recordmc.recorder.ScreenRecorder;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

/**
 * Malilibによるホットキー入力（H+RでのGUI表示、F9での録画トグル）を処理するハンドラー
 */
public class InputHandler implements IKeybindProvider, IInitializationHandler {
    private static final InputHandler INSTANCE = new InputHandler();

    public static InputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(RecordMCClient.MOD_ID, RecordConfigHandler.getInstance());
        InputEventHandler.getKeybindManager().registerKeybindProvider(this);

        RecordConfigs.Hotkeys.OPEN_GUI.getKeybind().setCallback((action, key) -> {
            if (action == KeyAction.PRESS) {
                GuiBase.openGui(new RecordConfigGui());
            }
            return true;
        });

        RecordConfigs.Hotkeys.TOGGLE_RECORDING.getKeybind().setCallback((action, key) -> {
            if (action == KeyAction.PRESS) {
                ScreenRecorder.getInstance().toggleRecording();
            }
            return true;
        });
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (fi.dy.masa.malilib.config.options.ConfigHotkey hotkey : RecordConfigs.getHotkeyOptions()) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory("recordmc", "recordmc.hotkeys.category.generic", RecordConfigs.getHotkeyOptions());
    }
}
