package cc.modlabs.kpaper.translation

import cc.modlabs.kpaper.translation.interfaces.TranslationHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Translations {

    lateinit var manager: TranslationManager
    private val translationHooks = arrayListOf<TranslationHook>()

    private fun loadTranslations(callback: ((Map<String, Int>) -> Unit)? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            manager.loadTranslations(callback)
        }
    }

    fun registerTranslationHook(hook: TranslationHook) {
        translationHooks.add(hook)
    }

    fun getTranslation(language: String, key: String, placeholders: Map<String, Any?> = mapOf()): String? {
        if (!::manager.isInitialized) return null

        var translation = manager.get(language, key, placeholders)?.message ?: return null

        for (hook in translationHooks) {
            translation = hook.onHandleTranslation(language, key, placeholders, translation)
        }

        return translation
    }

    val translations = manager

    fun load(translationManager: TranslationManager) {
        manager = translationManager

        loadTranslations()
    }
}