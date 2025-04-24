package cc.modlabs.kpaper.inventory.mineskin

import cc.modlabs.klassicx.tools.Environment
import cc.modlabs.kpaper.inventory.mineskin.models.texture.MineSkinResponse
import cc.modlabs.kpaper.inventory.mineskin.models.texture.MineSkinSingleSkinResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

object MineSkinFetcher {

    private val client = OkHttpClient()

    private val apiKey = Environment.getString("MINESKIN_API_KEY")

    private val gson = Gson()

    private val skinCache = mutableMapOf<String, MineSkinResponse>()

    fun fetchSkinSignature(skin: String): MineSkinResponse? {
        if (skinCache.containsKey(skin)) {
            return skinCache[skin]
        }

        val request = Request.Builder()
            .url("https://api.mineskin.org/v2/skins/$skin")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "MineSkin-User-Agent")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        val response = client.newCall(request).execute()
        val sR = gson.fromJson(response.body!!.string(), MineSkinSingleSkinResponse::class.java)
        if (sR.skin == null) {
            return null
        }

        skinCache[skin] = sR.skin

        return sR.skin
    }


}