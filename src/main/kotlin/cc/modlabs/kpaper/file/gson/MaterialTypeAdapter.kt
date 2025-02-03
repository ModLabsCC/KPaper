package cc.modlabs.kpaper.file.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.bukkit.Material

class MaterialTypeAdapter : TypeAdapter<Material>() {
    override fun write(out: JsonWriter?, value: Material?) {
        if (value == null) {
            out?.nullValue()
            return
        }
        out?.value(value.name.lowercase())
    }

    override fun read(`in`: JsonReader?): Material {
        if (`in`?.peek() == JsonToken.NULL) return Material.STONE

        return Material.valueOf(`in`!!.nextString().uppercase())
    }
}