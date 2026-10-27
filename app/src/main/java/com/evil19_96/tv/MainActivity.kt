package com.evil19_96.tv

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evil19_96.tv.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db by lazy { FavoriteDatabase.get(this) }
    private val channels = mutableListOf<Channel>()
    private lateinit var adapter: ChannelAdapter
    private var favoritesOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ChannelAdapter(::play, ::toggleFavorite)
        binding.channelList.layoutManager = LinearLayoutManager(this)
        binding.channelList.adapter = adapter
        binding.xtreamButton.setOnClickListener { loadXtream() }
        binding.m3uButton.setOnClickListener { askM3uOrDirect() }
        binding.stalkerButton.setOnClickListener { stalkerInfo() }
        binding.favoritesButton.setOnClickListener {
            favoritesOnly = !favoritesOnly
            binding.favoritesButton.text = if (favoritesOnly) "عرض كل القنوات" else "عرض المفضلة"
            if (favoritesOnly) showFavorites() else refreshList()
        }
    }

    private fun loadXtream() {
        val server = binding.serverInput.text?.toString().orEmpty()
        val user = binding.userInput.text?.toString().orEmpty()
        val pass = binding.passInput.text?.toString().orEmpty()
        binding.statusText.text = "جارٍ الاتصال وتحميل القنوات…"
        binding.xtreamButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val loaded = PlaylistRepository.xtream(server, user, pass)
                channels.clear(); channels.addAll(loaded); favoritesOnly = false
                binding.favoritesButton.text = "عرض المفضلة"
                refreshList()
                binding.statusText.text = "تم تحميل ${loaded.size} قناة"
                if (loaded.isEmpty()) toast("السيرفر لم يُرجع قنوات مباشرة")
            } catch (e: Exception) {
                binding.statusText.text = "تعذّر التحميل"
                toast(e.message ?: "حدث خطأ أثناء الاتصال")
            } finally { binding.xtreamButton.isEnabled = true }
        }
    }

    private fun askM3uOrDirect() {
        val input = EditText(this).apply {
            hint = "رابط M3U أو رابط بث مباشر"
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        AlertDialog.Builder(this).setTitle("إضافة قائمة أو رابط").setView(input)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تشغيل / استيراد") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isBlank() || !(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("rtsp://"))) {
                    toast("أدخل رابطاً صحيحاً يبدأ بـ http:// أو https:// أو rtsp://"); return@setPositiveButton
                }
                if (url.substringBefore('?').endsWith(".m3u", true) ||
                    url.substringBefore('?').endsWith(".m3u8", true)) {
                    binding.statusText.text = "جارٍ تحميل قائمة M3U…"
                    lifecycleScope.launch {
                        try {
                            val loaded = PlaylistRepository.m3u(url)
                            channels.clear(); channels.addAll(loaded); favoritesOnly = false
                            refreshList()
                            binding.statusText.text = "تم استيراد ${loaded.size} قناة"
                        } catch (e: Exception) { toast(e.message ?: "تعذر تحميل القائمة") }
                    }
                } else play(Channel(url.substringAfterLast('/').substringBefore('?').ifBlank { "بث مباشر" }, url, "رابط مباشر"))
            }.show()
    }

    private fun stalkerInfo() {
        AlertDialog.Builder(this).setTitle("Stalker Portal")
            .setMessage("دعم Stalker/MAG يحتاج تنفيذ مصادقة بوابة خاصة (handshake/token) وتختلف حسب إصدار Middleware. هذه النسخة لا تدّعي اتصالاً غير مكتمل. أدخل رابط البث أو استخدم Xtream/M3U حالياً.")
            .setPositiveButton("حسناً", null).show()
    }

    private fun play(channel: Channel) {
        startActivity(Intent(this, PlayerActivity::class.java).putExtra("STREAM_URL", channel.url).putExtra("CHANNEL_NAME", channel.name))
    }

    private fun toggleFavorite(channel: Channel) {
        lifecycleScope.launch {
            val dao = db.favorites()
            if (dao.contains(channel.url)) dao.remove(channel.url)
            else dao.add(FavoriteEntity(channel.url, channel.name, channel.group))
            if (favoritesOnly) showFavorites() else refreshList()
        }
    }

    private fun showFavorites() {
        lifecycleScope.launch {
            val list = db.favorites().all().map { Channel(it.name, it.url, it.groupName) }
            adapter.submit(list, list.map { it.url }.toSet())
            binding.statusText.text = "المفضلة: ${list.size} قناة"
        }
    }

    private fun refreshList() {
        lifecycleScope.launch {
            val favoriteUrls = db.favorites().all().map { it.url }.toSet()
            adapter.submit(channels, favoriteUrls)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
