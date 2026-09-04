package com.atsukimc.recordmc.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;

import java.io.File;

/**
 * Malilibによる設定ファイルの保存と読み込みを管理するハンドラー
 */
public class RecordConfigHandler implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = "recordmc.json";
    private static final RecordConfigHandler INSTANCE = new RecordConfigHandler();

    public static RecordConfigHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void load() {
        File configFile = FileUtils.getConfigDirectory();
        if (configFile == null) {
            configFile = new File("config");
        }
        File file = new File(configFile, CONFIG_FILE_NAME);

        if (file.exists() && file.isFile() && file.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(file);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", RecordConfigs.getGenericOptions());
                ConfigUtils.readHotkeys(root, "Hotkeys", RecordConfigs.getHotkeyOptions());
            }
        }
    }

    @Override
    public void save() {
        File configFile = FileUtils.getConfigDirectory();
        if (configFile == null) {
            configFile = new File("config");
        }
        if (!configFile.exists()) {
            configFile.mkdirs();
        }
        File file = new File(configFile, CONFIG_FILE_NAME);

        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "Generic", RecordConfigs.getGenericOptions());
        ConfigUtils.writeHotkeys(root, "Hotkeys", RecordConfigs.getHotkeyOptions());

        JsonUtils.writeJsonToFile(root, file);
    }
}
