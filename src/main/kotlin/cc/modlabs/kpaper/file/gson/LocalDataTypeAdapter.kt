package cc.modlabs.kpaper.file.gson

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    override fun serialize(
        src: LocalDate?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        // If you don't want null to appear at all, you can handle it differently
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src.toString())
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDate? {
        if (json == null || json.isJsonNull) return null
        return LocalDate.parse(json.asString)
    }
}