package cc.modlabs.kpaper.utils

import java.io.File

/**
 * The `TempStorage` class represents a utility for storing temporary files during runtime.
 */
object TempStorage {

    /**
     * Represents a temporary folder used for storing files during runtime.
     *
     * The `tempFolder` variable is a private property that holds a reference to a `File` object
     * representing the temporary folder. The folder is created if it does not already exist.
     *
     * Usage examples:
     *
     * - Saving a temporary file with given name and content:
     * ```kotlin
     * saveTempFile("filename.txt", "Hello, World!")*/
    private val tempFolder = File(".temp").also { it.mkdirs() }

    /**
     * Saves a temporary file with the given name and content.
     *
     * @param name The name of the temporary file.
     * @param content The content to be written to the temporary file, as a byte array.
     */
    fun saveTempFile(name: String, content: ByteArray) {
        val file = File(tempFolder, name).also { it.createFileIfNotExists() }
        file.writeBytes(content)
    }

    /**
     * Saves the content to a temporary file with the given name.
     *
     * @param name The name of the temporary file.
     * @param content The content to be written to the file.
     */
    fun saveTempFile(name: String, content: String) {
        val file = File(tempFolder, name).also { it.createFileIfNotExists() }
        file.writeText(content)
    }

    /**
     * Saves a temporary file with the specified name and content.
     *
     * @param name the name of the temporary file to be saved
     * @param content the content of the temporary file to be saved
     */
    fun saveTempFile(name: String, content: File) {
        val file = File(tempFolder, name).also { it.createFileIfNotExists() }
        content.copyTo(file, true)
    }

    /**
     * Deletes the temporary file with the specified name.
     *
     * @param name the name of the file to be deleted
     * @return true if the file was successfully deleted, false otherwise
     */
    fun deleteTempFile(name: String): Boolean {
        val file = File(tempFolder, name)
        return file.delete()
    }

    /**
     * Reads the content of a temporary file.
     *
     * @param name The name of the temporary file to read.
     * @return The content of the temporary file as a byte array.
     */
    fun readTempFile(name: String): ByteArray {
        val file = File(tempFolder, name)
        return file.readBytes()
    }

    /**
     * Reads the content of a temporary file as a string.
     *
     * @param name the name of the temporary file to read
     * @return the content of the temporary file as a string
     */
    fun readTempFileAsString(name: String): String {
        val file = File(tempFolder, name).also { it.createFileIfNotExists() }
        return file.readText()
    }

    /**
     * Reads the content of a temporary file as a string.
     *
     * @param file the file to read
     * @return the content of the temporary file as a string
     */
    fun readTempFileAsString(file: File): String {
        file.also { it.createFileIfNotExists() }
        return file.readText()
    }

    /**
     * Reads the content of a temporary file as a string, or returns null if the file does not exist.
     *
     * @param name the name of the temporary file to read
     * @return the content of the file as a string, or null if the file does not exist
     */
    fun readTempFileAsStringOrNull(name: String): String? {
        val file = File(tempFolder, name)

        if (!file.exists()) {
            return null
        }

        return file.readText()
    }

    /**
     * Retrieves a temporary file with the specified name.
     *
     * @param name the name of the temporary file
     * @return a [File] object representing the temporary file
     */
    fun getTempFile(name: String): File {
        return File(tempFolder, name)
    }

    /**
     * Retrieves the list of temporary files in the specified path.
     *
     * @param path The path of the temporary folder to retrieve files from.
     * @return A list of names of the temporary files in the specified path. An empty list is returned if the folder does not exist or if there are no files in the folder.
     */
    fun getTempFiles(path: String): List<File> {
        val folder = File(tempFolder, path).also { it.mkdirs() }
        return folder.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Creates a file if it does not already exist. If the parent directory does not exist, it will be created as well.
     */
    private fun File.createFileIfNotExists() {
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (!exists()) {
            createNewFile()
        }
    }
}