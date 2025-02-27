package cc.modlabs.kpaper.translation.interfaces

fun interface TranslationHook {

    fun onHandleTranslation(language: String, key: String, placeholders: Map<String, Any?>, result: String): String

}