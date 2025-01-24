package cc.modlabs.kpaper.language

import cc.modlabs.kpaper.translation.Translation
import cc.modlabs.kpaper.translation.interfaces.TranslationSource

class TestTranslationSource: TranslationSource {
    override suspend fun getLanguages(): List<String> {
        return listOf("en_US")
    }

    override suspend fun getTranslations(language: String): List<Translation> {
        return listOf(
            Translation(
                languageCode = "en_US",
                messageKey = "test",
                message = "Test"
            )
        )
    }
}