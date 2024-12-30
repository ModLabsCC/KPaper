package de.joker.kpaper.main.inventory

import org.bukkit.inventory.Inventory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface ISerializer {
    @Throws(IOException::class)
    fun serialize(inventories: List<Inventory>, outputStream: OutputStream)

    @Throws(IOException::class)
    fun deserializeInventories(stream: InputStream): List<Inventory>
}