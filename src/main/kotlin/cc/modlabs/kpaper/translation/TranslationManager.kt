package cc.modlabs.kpaper.translation

import cc.modlabs.kpaper.extensions.getLogger
import cc.modlabs.kpaper.translation.interfaces.TranslationSource
import cc.modlabs.kpaper.utils.TempStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class TranslationCache(
    private val source: TranslationSource
) {

    /**
     * The fallback language to be used when a translation is not found for a given language code.
     * The fallback language is represented by a dash-combined ISO-639 (language) and ISO-3166 (country) code.
     * If no translation is found for the specified language code, the system will attempt to find the translation in the fallback language.
     */
    private val fallbackLanguage = "en_US"

    /**
     * This variable represents a ReadWriteLock object named 'lock'. It is used for managing concurrent access to shared resources.
     * The ReadWriteLock allows multiple threads to read the resource concurrently, but only one thread can write to the resource at a time.
     */
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * Represents a cache for storing translations.
     *
     * The cache is a map where the language code is the key and a list of translations is the value.
     *
     * @property cache The map representing the cache. It is initially empty.
     */
    private var cache: Map<String, List<Translation>> = emptyMap()

    private val notFoundTranslations = mutableListOf<String>()
    /**
     * Retrieves all translations from the cache.
     *
     * @return A map containing language code as the key and the number of translations as the value.
     */
    private fun getAll(): Map<String, Int> {
        try {
            lock.readLock().lock()
            return cache.mapValues { it.value.size }
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getAllTranslations(): Map<String, List<Translation>> {
        return cache
    }

    suspend fun loadTranslations(callback: ((Map<String, Int>) -> Unit)? = null) {
        getLogger().info("Retrieving all translations from backend")
        val all = source.getLanguages()

        cache = emptyMap()

        var processed = 0
        all.forEach {
            val languageCode = it
            CoroutineScope(Dispatchers.Default).launch {
                loadTranslationsForLanguage(languageCode)
                processed++

                if (processed == all.size) {
                    delay(1000)
                    callback?.invoke(getAll())
                }
            }
        }
    }

    private suspend fun loadTranslationsForLanguage(languageCode: String) {
        val all = source.getTranslations(languageCode)

        val previousTranslations = cache[languageCode] ?: emptyList()
        put(languageCode, (previousTranslations + all.map { translation ->
            Translation(languageCode, translation.messageKey, translation.message)
        }))
    }

    /**
     * Retrieves the translation for the given language code and message key.
     *
     * @param languageCode The language code of the translation as dash-combined ISO-639 (language) and ISO-3166 (country).
     * @param messageKey The key identifying the message.
     * @param placeholders The placeholders to be replaced in the message.
     * @return The translation matching the language code and message key, or null if not found.
     */
    fun get(languageCode: String, messageKey: String, placeholders: Map<String, Any?> = emptyMap()): Translation? {
        try {
            lock.readLock().lock()
            var message = cache[languageCode]?.find { it.messageKey == messageKey }
                ?: cache[fallbackLanguage]?.find { it.messageKey == messageKey }

            if (message == null) {
                getLogger().info("No translation found for $languageCode:$messageKey")
                if (languageCode != fallbackLanguage) return null
                notFoundTranslations.add(
                    messageKey + "||" + placeholders.toList()
                        .joinToString("|") { "${it.first}::${it.second?.let { it::class.java.simpleName } ?: "null"}" })
                writeFailedTranslations()
                return null
            }

            for ((key, value) in placeholders) {
                message = message?.copy(message = message.message.replace("%$key%", value.toString()))
            }

            return message
        } finally {
            lock.readLock().unlock()
        }
    }

    private fun writeFailedTranslations() {
        if (notFoundTranslations.isEmpty()) return
        TempStorage.saveTempFile("not-found-translations.txt", notFoundTranslations.distinct().joinToString("\n"))
    }

    /**
     * Adds a translation to the cache for a given name.
     *
     * @param name The name of the translation.
     * @param translation The list of Translation objects representing the translations of a message in different languages.
     */
    private fun put(name: String, translation: List<Translation>) {
        try {
            lock.writeLock().lock()
            cache = cache + (name to translation)
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * Checks if the given name exists in the cache.
     *
     * @param name the name to check for existence in the cache
     * @return true if the name exists in the cache, false otherwise
     */
    fun contains(name: String): Boolean {
        try {
            lock.readLock().lock()
            return cache.containsKey(name)
        } finally {
            lock.readLock().unlock()
        }
    }
}