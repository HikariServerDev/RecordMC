package com.atsukimc.recordmc.compat.modmenu;

import com.atsukimc.recordmc.gui.RecordConfigGui;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenuの歯車アイコン連携クラス
 */
public class RecordModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RecordConfigGui::new;
    }
}
