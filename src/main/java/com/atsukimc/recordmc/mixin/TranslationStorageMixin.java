package com.atsukimc.recordmc.mixin;

import com.atsukimc.recordmc.util.LanguageGroupHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.resource.language.TranslationStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

/**
 * 登録言語以外の言語設定が選択された場合に、
 * 最も近い言語グループの翻訳を補完・注入するMixin
 */
@Mixin(TranslationStorage.class)
public abstract class TranslationStorageMixin {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/TranslationStorageMixin");

    @ModifyArg(
            method = "load",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;", remap = false),
            index = 0
    )
    private static Map<String, String> injectClosestLanguage(Map<String, String> map) {
        try {
            String currentCode = null;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                LanguageManager lm = client.getLanguageManager();
                if (lm != null) {
                    currentCode = lm.getLanguage();
                }
            }

            if (currentCode == null) {
                return map;
            }

            // 直接サポートされている言語ならスキップ
            if (LanguageGroupHelper.isDirectlySupported(currentCode)) {
                return map;
            }

            // 最も近い言語グループのコードを取得
            String closestCode = LanguageGroupHelper.getClosestLanguage(currentCode);
            if (closestCode.equals("en_us")) {
                return map; // en_usはすでにロード済み
            }

            // 最も近い言語の翻訳データをマップに注入
            Map<String, String> fallbackTranslations = LanguageGroupHelper.loadTranslations(closestCode);
            if (!fallbackTranslations.isEmpty()) {
                LOGGER.info("Language '{}' not directly supported by RecordMC. Fallback to closest group '{}'", currentCode, closestCode);
                for (Map.Entry<String, String> entry : fallbackTranslations.entrySet()) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to inject closest language translations", t);
        }

        return map;
    }
}
