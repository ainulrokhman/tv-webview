package id.my.awebdev.tvbrowser

import android.content.Context

/**
 * Penyimpanan link favorit yang sederhana: satu baris per favorit dengan
 * format "nama<TAB>url", disimpan di SharedPreferences yang sama dengan PIN.
 * Sengaja tanpa database — daftar favorit browser rumahan ukurannya kecil.
 */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Favorite(val name: String, val url: String)

    fun all(): List<Favorite> {
        val raw = prefs.getString(KEY_FAVORITES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("\t", limit = 2)
            if (parts.size == 2 && parts[1].isNotBlank()) {
                Favorite(parts[0].ifBlank { parts[1] }, parts[1])
            } else {
                null
            }
        }
    }

    fun add(name: String, url: String) {
        val cleanName = name.replace("\t", " ").replace("\n", " ").trim()
        val cleanUrl = url.replace("\t", " ").replace("\n", " ").trim()
        if (cleanUrl.isBlank()) return
        val items = all().filterNot { it.url == cleanUrl } +
            Favorite(cleanName.ifBlank { cleanUrl }, cleanUrl)
        save(items)
    }

    fun remove(url: String) {
        save(all().filterNot { it.url == url })
    }

    private fun save(items: List<Favorite>) {
        val raw = items.joinToString("\n") { "${it.name}\t${it.url}" }
        prefs.edit().putString(KEY_FAVORITES, raw).apply()
    }

    companion object {
        const val PREFS_NAME = "tv_browser"
        private const val KEY_FAVORITES = "favorites"
    }
}
