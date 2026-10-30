package com.atsukimc.recordmc;

import com.atsukimc.recordmc.util.LanguageGroupHelper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageGroupTest {

    @Test
    public void testDirectlySupportedLanguages() {
        for (String lang : LanguageGroupHelper.SUPPORTED_LANGUAGES) {
            assertTrue(LanguageGroupHelper.isDirectlySupported(lang), "Should directly support: " + lang);
            assertEquals(lang, LanguageGroupHelper.getClosestLanguage(lang), "Direct match for: " + lang);
        }
    }

    @Test
    public void testSpanishRegionalMatching() {
        // ラテンアメリカ系スペイン語 -> es_mx
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_ar")); // アルゼンチン
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_cl")); // チリ
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_co")); // コロンビア
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_pe")); // ペルー
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_ve")); // ベネズエラ
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_uy")); // ウルグアイ
        assertEquals("es_mx", LanguageGroupHelper.getClosestLanguage("es_ec")); // エクアドル

        // 欧州・スペイン本国系 -> es_es
        assertEquals("es_es", LanguageGroupHelper.getClosestLanguage("es_es"));
        assertEquals("es_es", LanguageGroupHelper.getClosestLanguage("es_gq")); // 赤道ギニア
    }

    @Test
    public void testChineseRegionalMatching() {
        // 繁体字系 (台湾, 香港, マカオ) -> zh_tw
        assertEquals("zh_tw", LanguageGroupHelper.getClosestLanguage("zh_tw"));
        assertEquals("zh_tw", LanguageGroupHelper.getClosestLanguage("zh_hk"));
        assertEquals("zh_tw", LanguageGroupHelper.getClosestLanguage("zh_mo"));
        assertEquals("zh_tw", LanguageGroupHelper.getClosestLanguage("zh_hant"));

        // 簡体字系 (中国大陸, シンガポール, マレーシア) -> zh_cn
        assertEquals("zh_cn", LanguageGroupHelper.getClosestLanguage("zh_cn"));
        assertEquals("zh_cn", LanguageGroupHelper.getClosestLanguage("zh_sg"));
        assertEquals("zh_cn", LanguageGroupHelper.getClosestLanguage("zh_my"));
    }

    @Test
    public void testLanguagePrefixMatching() {
        // カナダフランス語 -> fr_fr
        assertEquals("fr_fr", LanguageGroupHelper.getClosestLanguage("fr_ca"));
        assertEquals("fr_fr", LanguageGroupHelper.getClosestLanguage("fr_be"));
        assertEquals("fr_fr", LanguageGroupHelper.getClosestLanguage("fr_ch"));

        // オーストリア/スイスドイツ語 -> de_de
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("de_at"));
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("de_ch"));

        // ポルトガル本国ポルトガル語 -> pt_br
        assertEquals("pt_br", LanguageGroupHelper.getClosestLanguage("pt_pt"));

        // イギリス英語/カナダ英語/豪州英語/海賊語 -> en_us
        assertEquals("en_us", LanguageGroupHelper.getClosestLanguage("en_gb"));
        assertEquals("en_us", LanguageGroupHelper.getClosestLanguage("en_ca"));
        assertEquals("en_us", LanguageGroupHelper.getClosestLanguage("en_au"));
        assertEquals("en_us", LanguageGroupHelper.getClosestLanguage("en_7s"));
    }

    @Test
    public void testSlavicLanguageGroup() {
        // ベラルーシ語 -> ru_ru
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("be_by"));

        // 西スラブ (チェコ, スロバキア, シレジア) -> pl_pl
        assertEquals("pl_pl", LanguageGroupHelper.getClosestLanguage("cs_cz"));
        assertEquals("pl_pl", LanguageGroupHelper.getClosestLanguage("sk_sk"));
        assertEquals("pl_pl", LanguageGroupHelper.getClosestLanguage("szl_pl"));

        // 南スラブ (ブルガリア, マケドニア, セルビア) -> ru_ru
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("bg_bg"));
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("mk_mk"));
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("sr_sp"));
    }

    @Test
    public void testRomanceLanguageGroup() {
        // カタルーニャ語, ガリシア語, アストゥリアス語, バスク語 -> es_es
        assertEquals("es_es", LanguageGroupHelper.getClosestLanguage("ca_es"));
        assertEquals("es_es", LanguageGroupHelper.getClosestLanguage("gl_es"));
        assertEquals("es_es", LanguageGroupHelper.getClosestLanguage("ast_es"));
        assertEquals("es_es", LanguageGroupHelper.getClosestLanguage("eu_es"));

        // オック語, ブルトン語 -> fr_fr
        assertEquals("fr_fr", LanguageGroupHelper.getClosestLanguage("oc_fr"));
        assertEquals("fr_fr", LanguageGroupHelper.getClosestLanguage("br_fr"));

        // ルーマニア語, ラテン語 -> it_it
        assertEquals("it_it", LanguageGroupHelper.getClosestLanguage("ro_ro"));
        assertEquals("it_it", LanguageGroupHelper.getClosestLanguage("la_la"));
    }

    @Test
    public void testGermanicLanguageGroup() {
        // オランダ語, アフリカーンス語, フリジア語, ルクセンブルク語 -> de_de
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("nl_nl"));
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("af_za"));
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("lb_lu"));

        // デンマーク語, ノルウェー語, スウェーデン語, アイスランド語 -> de_de
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("da_dk"));
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("no_no"));
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("sv_se"));
        assertEquals("de_de", LanguageGroupHelper.getClosestLanguage("is_is"));
    }

    @Test
    public void testTurkicAndOthers() {
        // 中央アジア・旧ソ連圏 -> ru_ru
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("kk_kz"));
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("ky_kg"));
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("uz_uz"));
        assertEquals("ru_ru", LanguageGroupHelper.getClosestLanguage("tt_ru"));

        // 未知の言語 -> en_us
        assertEquals("en_us", LanguageGroupHelper.getClosestLanguage("xyz_unknown"));
    }

    @Test
    public void testLoadTranslations() {
        Map<String, String> ja = LanguageGroupHelper.loadTranslations("ja_jp");
        assertNotNull(ja);
        assertTrue(ja.containsKey("recordmc.gui.title.config"));
        assertEquals("RecordMC 設定メニュー", ja.get("recordmc.gui.title.config"));

        Map<String, String> esMx = LanguageGroupHelper.loadTranslations("es_mx");
        assertNotNull(esMx);
        assertTrue(esMx.containsKey("recordmc.gui.title.config"));
        assertEquals("Configuración de RecordMC", esMx.get("recordmc.gui.title.config"));
    }
}
