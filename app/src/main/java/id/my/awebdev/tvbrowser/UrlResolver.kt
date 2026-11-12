package id.my.awebdev.tvbrowser

import java.net.URLEncoder

/**
 * Mengubah teks dari kolom alamat remote menjadi URL yang dimuat WebView.
 *
 * Kolomnya tetap satu untuk dua maksud, dengan aturan tegas: bila di
 * dalam teks ada link — atau teksnya sendiri berbentuk alamat — link itu
 * yang dibuka langsung. Pencarian hanya untuk teks yang jelas bukan
 * alamat (kata kunci atau kalimat biasa). Aturan ini menjawab keluhan
 * "link yang ditempel dari aplikasi lain malah jadi pencarian":
 * tempelan hasil bagikan sering berbentuk judul + URL, atau alamat
 * tanpa skema yang bercampur teks lain.
 */
object UrlResolver {

    // URL lengkap atau berawalan www. di mana pun di dalam teks.
    private val explicitUrl = Regex("""(?i)(?:https?://|www\.)[^\s<>"']+""")

    // Alamat host tanpa skema: label domain bertitik diakhiri TLD huruf,
    // boleh diikuti port dan path/query. Juga menemukan alamat yang
    // tertanam di tengah teks tempelan.
    private val hostLike =
        Regex("""(?i)[a-z0-9-]+(?:\.[a-z0-9-]+)*\.[a-z]{2,}(?::\d+)?(?:/[^\s<>"']*)?""")

    // Tanda baca yang lazim menempel di ujung URL hasil salin-tempel.
    private const val TRAILING = ".,;:!?)]}>\"'"

    /** URL final untuk dimuat, atau null bila teks kosong. */
    fun resolve(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val candidate = findLink(text)
        if (candidate != null) return withScheme(candidate)
        return "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(text, "UTF-8")
    }

    /** Link pertama di dalam teks (sudah dibersihkan), atau null. */
    fun findLink(text: String): String? {
        val match = explicitUrl.find(text) ?: hostLike.find(text) ?: return null
        val cleaned = match.value.trim().trimEnd { it in TRAILING }
        return cleaned.ifEmpty { null }
    }

    private fun withScheme(candidate: String): String = when {
        candidate.startsWith("https://", ignoreCase = true) ->
            "https://" + candidate.substring(8)
        candidate.startsWith("http://", ignoreCase = true) ->
            "http://" + candidate.substring(7)
        else -> "https://$candidate"
    }
}
