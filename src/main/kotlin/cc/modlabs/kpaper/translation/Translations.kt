package cc.modlabs.kpaper.translation

import cc.modlabs.kpaper.translation.interfaces.TranslationHook
import cc.modlabs.kpaper.translation.interfaces.TranslationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Translations {

    lateinit var manager: TranslationManager
    private val translationHooks = arrayListOf<TranslationHook>()

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

    val translations get() = manager

    fun load(source: TranslationSource, callback: ((Map<String, Int>) -> Unit)? = null) {
        manager = TranslationManager(source)

        CoroutineScope(Dispatchers.Default).launch {
            manager.loadTranslations(callback)
        }
    }
}