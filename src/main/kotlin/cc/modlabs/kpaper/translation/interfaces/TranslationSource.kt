package cc.modlabs.kpaper.translation.interfaces

import cc.modlabs.kpaper.translation.Translation

interface TranslationSource {
    suspend fun getLanguages(): List<String>

    suspend fun getTranslations(language: String): List<Translation>
}