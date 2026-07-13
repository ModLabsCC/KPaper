package cc.modlabs.kpaper.file

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TempStorageTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `files remain inside storage root`() {
        val storage = BaseTempStorage(directory.toString())
        storage.saveTempFile("nested/value.txt", "safe")
        assertEquals("safe", storage.readTempFileAsString("nested/value.txt"))
    }

    @Test
    fun `path traversal is rejected`() {
        val storage = BaseTempStorage(directory.toString())
        assertThrows(IllegalArgumentException::class.java) {
            storage.saveTempFile("../outside.txt", "unsafe")
        }
    }
}
