package cc.modlabs.kpaper.utils

import com.google.gson.Gson
import java.net.URI

class MojangAPI {
    private val gson = Gson()

    fun getUser(user: String): MclSuccessResponse? {
        val url = "https://mcl.flawcra.cc/$user"
        val response = URI.create(url).toURL().readText()

        val errorResponse = gson.fromJson(response, MclErrorResponse::class.java)
        if (errorResponse.error != null) {
            return null
        }

        return gson.fromJson(response, MclSuccessResponse::class.java)
    }
}

data class MclErrorResponse(
    val code: String? = null,
    val error: String? = null
)

data class MclSuccessResponse(
    val username: String,
    val id: String,      // already hyphenated
    val avatar: String,
    val skin_texture: String,
)