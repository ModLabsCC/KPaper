package cc.modlabs.kpaper.inventory.mineskin

import cc.modlabs.klassicx.tools.Environment
import cc.modlabs.kpaper.inventory.mineskin.MineSkinResponse
import cc.modlabs.kpaper.inventory.mineskin.MineSkinSingleSkinResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit

object MineSkinFetcher {

    private const val MAX_CACHE_ENTRIES = 512
    private val skinIdPattern = Regex("^[A-Za-z0-9_-]{1,128}$")

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val apiKey = Environment.getString("MINESKIN_API_KEY")

    private val gson = Gson()

    private val skinCache = object : LinkedHashMap<String, MineSkinResponse>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MineSkinResponse>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    fun fetchSkinSignature(skin: String): MineSkinResponse? {
        require(skinIdPattern.matches(skin)) { "Invalid MineSkin identifier" }
        synchronized(skinCache) { skinCache[skin]?.let { return it } }

        val request = Request.Builder()
            .url("https://api.mineskin.org/v2/skins/$skin")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "MineSkin-User-Agent")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string() ?: return@use null
            val skinResponse = gson.fromJson(body, MineSkinSingleSkinResponse::class.java).skin ?: return@use null
            synchronized(skinCache) { skinCache[skin] = skinResponse }
            skinResponse
        }
    }

    fun clearCache() = synchronized(skinCache) { skinCache.clear() }

    /**
     * Accept a textures.minecraft.net URL and return the texture ID segment.
     * If the input is already an ID, returns it unchanged.
     */
    fun fetchTexture(textureUrl: String): String {
        val idx = textureUrl.lastIndexOf('/')
        return if (idx >= 0) textureUrl.substring(idx + 1) else textureUrl
    }


}
