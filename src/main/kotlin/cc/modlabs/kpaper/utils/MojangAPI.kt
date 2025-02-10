package cc.modlabs.kpaper.utils

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.URI

class MojangAPI {
    private val gson = Gson()

    fun getUser(user: String): MclSuccessResponse? {
        try {
            CoroutineScope(Dispatchers.Default).run {
                val url = "https://mcl.flawcra.cc/$user"
                val response = URI.create(url).toURL().readText()

                val errorResponse = gson.fromJson(response, MclErrorResponse::class.java)
                if (errorResponse.error != null) {
                    return null
                }

                return gson.fromJson(response, MclSuccessResponse::class.java)
            }
        } catch (e: Exception) {
            return null
        }
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