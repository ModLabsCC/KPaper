package cc.modlabs.kpaper.file.gson

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemFactory
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.lang.reflect.Type

class ItemStackTypeAdapter : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    override fun serialize(src: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("item", src.type.key.toString())
        jsonObject.addProperty("count", src.amount)

        if (src.hasItemMeta()) {
            val metaMap = src.itemMeta?.serialize() ?: emptyMap()
            val metaJson = context.serialize(metaMap)
            jsonObject.add("meta", metaJson)
        }

        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        val jsonObject = json.asJsonObject
        val item = Material.matchMaterial(jsonObject["item"].asString) ?: throw JsonParseException("Invalid material")
        val count = jsonObject["count"].asInt

        val stack = ItemStack(item, count)

        if (jsonObject.has("meta")) {
            val metaJson = jsonObject["meta"]
            val metaMap = context.deserialize<Map<String, Any>>(metaJson, Map::class.java)

            val itemFactory: ItemFactory = Bukkit.getItemFactory()
            val meta = itemFactory.getItemMeta(item)
            if (meta is ConfigurationSerializable) {
                metaMap.forEach { (key, value) -> meta.javaClass.getMethod("set$key", value::class.java)?.invoke(meta, value) }
            }
            stack.itemMeta = meta
        }

        return stack
    }
}
