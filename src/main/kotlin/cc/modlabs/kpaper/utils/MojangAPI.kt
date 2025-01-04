package cc.modlabs.kpaper.utils

import com.google.gson.Gson
import java.net.URL

class MojangAPI {
    private val gson = Gson()

    fun getUUID(name: String): String {
        val baseUrl = "https://api.mojang.com/users/profiles/minecraft/$name"
        val response = URL(baseUrl).readText()
        val uuidResponse = gson.fromJson(response, MojangUUIDResponse::class.java)
        return uuidResponse.id.replace("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(), "$1-$2-$3-$4-$5")
    }

    fun getUsername(uuid: String): String {
        // http request to mojang api to get username
        val responseUrl = "https://sessionserver.mojang.com/session/minecraft/profile/$uuid"
        val response = URL(responseUrl).readText()
        val skinResponse = gson.fromJson(response, SkinResponse::class.java)
        return skinResponse.name
    }
}

data class SkinResponse(val id: String, val name: String, val properties: List<MojangProperty>)
data class MojangProperty(val name: String, val value: String)

data class MojangUUIDResponse(val name: String, val id: String)