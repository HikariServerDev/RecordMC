package com.atsukimc.recordmc.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登録言語以外の言語設定が選択された際に、
 * 言語系統・地域・方言グループから最も近い登録言語を自動判定・解決するヘルパークラス
 */
public class LanguageGroupHelper {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/LanguageGroup");
    private static final Gson GSON = new Gson();

    // 直接提供されている登録言語コード一覧 (全14言語)
    public static final Set<String> SUPPORTED_LANGUAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "ja_jp", "en_us", "zh_cn", "zh_tw", "ko_kr", "ru_ru",
            "de_de", "fr_fr", "es_es", "es_mx", "pt_br", "it_it", "pl_pl", "uk_ua"
    )));

    // スペイン語圏（ラテンアメリカ地域コード一覧）
    private static final Set<String> LATIN_AMERICA_COUNTRIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "ar", "bo", "cl", "co", "cr", "cu", "do", "ec", "gt", "hn",
            "mx", "ni", "pa", "pe", "pr", "py", "sv", "uy", "ve", "us"
    )));

    // キャッシュされた言語翻訳データ
    private static final Map<String, Map<String, String>> TRANSLATION_CACHE = new ConcurrentHashMap<>();

    /**
     * 指定された言語コードが直接サポートされているか確認
     */
    public static boolean isDirectlySupported(String code) {
        if (code == null) return false;
        return SUPPORTED_LANGUAGES.contains(code.toLowerCase(Locale.ROOT));
    }

    /**
     * 与えられた言語コードに対して、最も近い言語グループの登録言語コードを判定して返す
     *
     * @param requestedCode ユーザーが選択した言語コード (例: "es_ar", "fr_ca", "zh_hk")
     * @return 最も近い登録言語コード (例: "es_mx", "fr_fr", "zh_tw")
     */
    public static String getClosestLanguage(String requestedCode) {
        if (requestedCode == null || requestedCode.trim().isEmpty()) {
            return "en_us";
        }

        String code = requestedCode.toLowerCase(Locale.ROOT).trim();

        // 1. 完全一致
        if (SUPPORTED_LANGUAGES.contains(code)) {
            return code;
        }

        // 言語コードの分解 (言語プレフィックスと国/地域サフィックス)
        String prefix = code;
        String region = "";
        int sepIdx = code.indexOf('_');
        if (sepIdx < 0) {
            sepIdx = code.indexOf('-');
        }
        if (sepIdx > 0) {
            prefix = code.substring(0, sepIdx);
            region = code.substring(sepIdx + 1);
        }

        // 2. スペイン語圏の判定 (欧州 vs ラテンアメリカ)
        if (prefix.equals("es")) {
            if (LATIN_AMERICA_COUNTRIES.contains(region)) {
                return "es_mx";
            } else {
                return "es_es";
            }
        }

        // 3. 中国語圏の判定 (繁体字 vs 簡体字)
        if (prefix.equals("zh")) {
            if (region.equals("tw") || region.equals("hk") || region.equals("mo") || code.contains("hant")) {
                return "zh_tw";
            } else {
                return "zh_cn";
            }
        }

        // 4. 同一言語プレフィックスの判定
        switch (prefix) {
            case "ja": return "ja_jp";
            case "ko": return "ko_kr";
            case "ru": return "ru_ru";
            case "uk": return "uk_ua";
            case "pt": return "pt_br";
            case "fr": return "fr_fr";
            case "de": return "de_de";
            case "it": return "it_it";
            case "pl": return "pl_pl";
            case "en": return "en_us";
        }

        // 5. 語族・近縁言語グループの判定

        // 東スラブ語群 (East Slavic): ベラルーシ語など
        if (prefix.equals("be")) {
            return "ru_ru";
        }

        // 西スラブ語群 (West Slavic): チェコ語、スロバキア語、シレジア語、カシューブ語、ソルブ語
        if (prefix.equals("cs") || prefix.equals("sk") || prefix.equals("szl") || prefix.equals("csb") || prefix.startsWith("hsb") || prefix.startsWith("dsb")) {
            return "pl_pl";
        }

        // 南スラブ語群 (South Slavic): ブルガリア語、マケドニア語、セルビア語、ボスニア語、クロアチア語、スロベニア語
        if (prefix.equals("bg") || prefix.equals("mk") || prefix.equals("sr") || prefix.equals("bs") || prefix.equals("hr") || prefix.equals("sl") || prefix.equals("sh")) {
            return "ru_ru";
        }

        // イベロ・ロマンス語群 / スペイン地域言語: カタルーニャ語、ガリシア語、アストゥリアス語、アラゴン語、バスク語
        if (prefix.equals("ca") || prefix.equals("gl") || prefix.equals("ast") || prefix.equals("an") || prefix.equals("eu")) {
            return "es_es";
        }

        // フランス地域言語: オック語、ブルトン語
        if (prefix.equals("oc") || prefix.equals("br")) {
            return "fr_fr";
        }

        // その他のロマンス語族: ルーマニア語、コルシカ語、サルデーニャ語、ラテン語
        if (prefix.equals("ro") || prefix.equals("mo") || prefix.equals("co") || prefix.equals("sc") || prefix.equals("la")) {
            return "it_it";
        }

        // 西ゲルマン語群 (West Germanic): オランダ語、アフリカーンス語、フリジア語、ルクセンブルク語、低地ドイツ語、イディッシュ語
        if (prefix.equals("nl") || prefix.equals("af") || prefix.equals("fy") || prefix.equals("lb") || prefix.equals("nds") || prefix.equals("li") || prefix.equals("yi")) {
            return "de_de";
        }

        // 北ゲルマン語群 / 北欧 (North Germanic): デンマーク語、ノルウェー語、スウェーデン語、アイスランド語、フェロー語
        if (prefix.equals("da") || prefix.equals("no") || prefix.equals("nb") || prefix.equals("nn") || prefix.equals("sv") || prefix.equals("is") || prefix.equals("fo")) {
            return "de_de";
        }

        // チュルク語族・中央アジア・旧ソ連圏: カザフ語、キルギス語、ウズベク語、タタール語、バシキール語、サハ語、アゼルバイジャン語、エストニア、ラトビア、リトアニア
        if (prefix.equals("kk") || prefix.equals("ky") || prefix.equals("uz") || prefix.equals("tt") || prefix.equals("ba") || prefix.equals("sah") || prefix.equals("az")
                || prefix.equals("et") || prefix.equals("lv") || prefix.equals("lt")) {
            return "ru_ru";
        }

        // 6. いずれにも該当しない場合は国際共通語として英語 (en_us) にフォールバック
        return "en_us";
    }

    /**
     * 指定された言語コードのRecordMC翻訳データをロードする
     */
    public static Map<String, String> loadTranslations(String langCode) {
        return TRANSLATION_CACHE.computeIfAbsent(langCode, code -> {
            Map<String, String> map = new HashMap<>();
            String path = "/assets/recordmc/lang/" + code + ".json";
            try (InputStream in = LanguageGroupHelper.class.getResourceAsStream(path)) {
                if (in != null) {
                    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                        if (obj != null) {
                            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                                if (entry.getValue().isJsonPrimitive()) {
                                    map.put(entry.getKey(), entry.getValue().getAsString());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load language file: {}", path, e);
            }
            return Collections.unmodifiableMap(map);
        });
    }
}
