package cc.modlabs.kpaper.language

import cc.modlabs.kpaper.translation.Translations
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.ints.shouldBeGreaterThan

class TranslationsTest : FunSpec() {

    init {
        coroutineTestScope = true
        test("loadTranslations") {
            Translations.load(TestTranslationSource()) { loadedTranslations ->
                loadedTranslations.size shouldBeGreaterThan 0
            }
            testCoroutineScheduler.runCurrent()
        }
    }
}