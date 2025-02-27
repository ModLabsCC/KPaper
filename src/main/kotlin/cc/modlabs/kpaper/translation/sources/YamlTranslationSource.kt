package cc.modlabs.kpaper.translation.sources

import cc.modlabs.kpaper.translation.Translation
import cc.modlabs.kpaper.translation.interfaces.TranslationSource
import org.yaml.snakeyaml.Yaml
import java.io.File

class YamlTranslationSource(private val directory: File) : TranslationSource {
    override suspend fun getLanguages(): List<String> {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory.listFiles { file -> file.extension == "yml" }?.map { it.nameWithoutExtension } ?: emptyList()
    }

    override suspend fun getTranslations(language: String): List<Translation> {
        val langFile = File(directory, "$language.yml")
        if (!langFile.exists()) return emptyList()

        val yaml = Yaml()
        val data = yaml.load<Map<String, String>>(langFile.reader())
        return data.map { (key, value) -> Translation(
            language,
            key,
            value
        ) }
    }

}