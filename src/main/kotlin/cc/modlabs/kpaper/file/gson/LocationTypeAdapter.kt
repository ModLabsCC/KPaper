package cc.modlabs.kpaper.file.gson

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.lang.reflect.Type

class LocationTypeAdapter : JsonSerializer<Location>, JsonDeserializer<Location> {
    override fun serialize(src: Location, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("world", src.world?.name ?: "world")
        jsonObject.addProperty("x", src.x)
        jsonObject.addProperty("y", src.y)
        jsonObject.addProperty("z", src.z)
        jsonObject.addProperty("yaw", src.yaw)
        jsonObject.addProperty("pitch", src.pitch)
        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Location {
        val jsonObject = json.asJsonObject
        val worldName = jsonObject["world"].asString
        val world: World = Bukkit.getWorld(worldName) ?: throw JsonParseException("World '$worldName' not found")

        val x = jsonObject["x"].asDouble
        val y = jsonObject["y"].asDouble
        val z = jsonObject["z"].asDouble
        val yaw = jsonObject["yaw"].asFloat
        val pitch = jsonObject["pitch"].asFloat

        return Location(world, x, y, z, yaw, pitch)
    }
}