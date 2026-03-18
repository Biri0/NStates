package it.rfmariano.nstates.data.translation

import java.util.Locale

data class DeepLLanguage(
    val code: String,
    val name: String
)

object DeepLLanguageSupport {
    val supportedLanguages: List<DeepLLanguage> = listOf(
        DeepLLanguage("AR", "Arabic"),
        DeepLLanguage("BG", "Bulgarian"),
        DeepLLanguage("CS", "Czech"),
        DeepLLanguage("DA", "Danish"),
        DeepLLanguage("DE", "German"),
        DeepLLanguage("EL", "Greek"),
        DeepLLanguage("EN", "English"),
        DeepLLanguage("ES", "Spanish"),
        DeepLLanguage("ET", "Estonian"),
        DeepLLanguage("FI", "Finnish"),
        DeepLLanguage("FR", "French"),
        DeepLLanguage("HE", "Hebrew"),
        DeepLLanguage("HU", "Hungarian"),
        DeepLLanguage("ID", "Indonesian"),
        DeepLLanguage("IT", "Italian"),
        DeepLLanguage("JA", "Japanese"),
        DeepLLanguage("KO", "Korean"),
        DeepLLanguage("LT", "Lithuanian"),
        DeepLLanguage("LV", "Latvian"),
        DeepLLanguage("NB", "Norwegian"),
        DeepLLanguage("NL", "Dutch"),
        DeepLLanguage("PL", "Polish"),
        DeepLLanguage("PT", "Portuguese"),
        DeepLLanguage("RO", "Romanian"),
        DeepLLanguage("RU", "Russian"),
        DeepLLanguage("SK", "Slovak"),
        DeepLLanguage("SL", "Slovenian"),
        DeepLLanguage("SV", "Swedish"),
        DeepLLanguage("TH", "Thai"),
        DeepLLanguage("TR", "Turkish"),
        DeepLLanguage("UK", "Ukrainian"),
        DeepLLanguage("VI", "Vietnamese"),
        DeepLLanguage("ZH", "Chinese")
    )

    private val supportedCodes = supportedLanguages.map { it.code }.toSet()
    val targetLanguages: List<DeepLLanguage> = supportedLanguages.filterNot { it.code == "EN" }

    data class SystemLanguageInfo(
        val code: String,
        val name: String,
        val isSupported: Boolean,
        val isEnglish: Boolean
    ) {
        val isTranslatable: Boolean
            get() = isSupported && !isEnglish
    }

    fun normalizeOrDefault(code: String?): String {
        val normalized = code?.trim()?.uppercase(Locale.ROOT)
        return if (normalized != null && normalized in supportedCodes) {
            normalized
        } else {
            "EN"
        }
    }

    fun defaultTargetForLocale(locale: Locale = Locale.getDefault()): String {
        return normalizeOrDefault(locale.language)
    }

    fun isTranslatableTarget(code: String?): Boolean {
        return normalizeOrDefault(code) != "EN"
    }

    fun systemLanguageInfo(locale: Locale = Locale.getDefault()): SystemLanguageInfo {
        val normalized = locale.language.trim().uppercase(Locale.ROOT).ifBlank { "EN" }
        val supportedLanguage = supportedLanguages.firstOrNull { it.code == normalized }
        val displayName = supportedLanguage?.name ?: locale.getDisplayLanguage(locale).ifBlank { normalized }
        return SystemLanguageInfo(
            code = normalized,
            name = displayName,
            isSupported = supportedLanguage != null,
            isEnglish = normalized == "EN"
        )
    }
}
