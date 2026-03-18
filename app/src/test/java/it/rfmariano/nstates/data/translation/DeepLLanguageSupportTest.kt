package it.rfmariano.nstates.data.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DeepLLanguageSupportTest {

    @Test
    fun normalizeOrDefault_returnsUppercaseSupportedCode() {
        assertEquals("IT", DeepLLanguageSupport.normalizeOrDefault("it"))
    }

    @Test
    fun normalizeOrDefault_fallsBackToEnglishWhenUnsupported() {
        assertEquals("EN", DeepLLanguageSupport.normalizeOrDefault("xx"))
    }

    @Test
    fun defaultTargetForLocale_mapsLocaleLanguage() {
        assertEquals("FR", DeepLLanguageSupport.defaultTargetForLocale(Locale("fr", "FR")))
    }

    @Test
    fun isTranslatableTarget_returnsFalseForEnglish() {
        assertFalse(DeepLLanguageSupport.isTranslatableTarget("EN"))
    }

    @Test
    fun targetLanguages_excludesEnglish() {
        assertTrue(DeepLLanguageSupport.targetLanguages.none { it.code == "EN" })
    }

    @Test
    fun systemLanguageInfo_marksUnsupportedLanguage() {
        val info = DeepLLanguageSupport.systemLanguageInfo(Locale("xx", "YY"))
        assertEquals("XX", info.code)
        assertFalse(info.isSupported)
        assertFalse(info.isTranslatable)
    }
}
