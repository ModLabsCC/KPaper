package cc.modlabs.kpaper.file

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Unlike loadConfiguration, malformed input throws instead of becoming an empty file. */
internal fun readYaml(file: File) = YamlConfiguration().apply {
    if (file.exists()) load(file)
}

internal fun YamlConfiguration.saveAtomically(file: File) {
    val target = file.toPath().toAbsolutePath()
    Files.createDirectories(target.parent)
    val temporary = Files.createTempFile(target.parent, target.fileName.toString(), ".tmp")
    try {
        Files.writeString(temporary, saveToString())
        try {
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, target, REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temporary)
    }
}
