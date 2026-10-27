package com.evil19_96.tv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object PlaylistRepository {
    private val client = OkHttpClient.Builder().build()

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", "Evil19_96/1.0").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("فشل الاتصال: HTTP ${response.code}")
            response.body?.string() ?: error("الاستجابة فارغة")
        }
    }

    suspend fun xtream(serverRaw: String, username: String, password: String): List<Channel> {
        val server = serverRaw.trim().trimEnd('/')
        require(server.startsWith("http://") || server.startsWith("https://")) {
            "أدخل رابط السيرفر كاملاً مع http:// أو https://"
        }
        require(username.isNotBlank() && password.isNotBlank()) { "أدخل اسم المستخدم وكلمة المرور" }
        val user = URLEncoder.encode(username, "UTF-8")
        val pass = URLEncoder.encode(password, "UTF-8")
        val auth = JSONObject(get("$server/player_api.php?username=$user&password=$pass"))
        val userInfo = auth.optJSONObject("user_info")
        if (userInfo == null || userInfo.optString("auth", "1") == "0") error("بيانات Xtream غير صحيحة")
        val liveCategories = runCatching {
            val arr = org.json.JSONArray(get("$server/player_api.php?username=$user&password=$pass&action=get_live_categories"))
            (0 until arr.length()).associate {
                val o = arr.getJSONObject(it); o.optString("category_id") to o.optString("category_name", "غير مصنف")
            }
        }.getOrDefault(emptyMap())
        val arr = org.json.JSONArray(get("$server/player_api.php?username=$user&password=$pass&action=get_live_streams"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("stream_id")
            val name = o.optString("name", "قناة")
            if (id.isBlank()) null else Channel(
                name = name,
                url = "$server/live/$user/$pass/$id.${o.optString("container_extension", "ts").ifBlank { "ts" }}",
                group = liveCategories[o.optString("category_id")] ?: "مباشر",
                logo = o.optString("stream_icon").takeIf { it.startsWith("http") }
            )
        }
    }

    suspend fun m3u(url: String): List<Channel> {
        val content = get(url.trim())
        parseM3u(content).also { if (it.isEmpty()) error("لم يتم العثور على روابط صالحة في M3U") }
    }

    fun parseM3u(content: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = "قناة"
        var group = "غير مصنف"
        var logo: String? = null
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                name = line.substringAfter(',', "قناة").trim().ifBlank { "قناة" }
                group = Regex("""group-title=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1) ?: "غير مصنف"
                logo = Regex("""tvg-logo=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1)
            } else if (line.isNotEmpty() && !line.startsWith("#") &&
                (line.startsWith("http://", true) || line.startsWith("https://", true) ||
                 line.startsWith("rtsp://", true))) {
                result += Channel(name, line, group, logo)
                name = "قناة"; group = "غير مصنف"; logo = null
            }
        }
        return result
    }
}
