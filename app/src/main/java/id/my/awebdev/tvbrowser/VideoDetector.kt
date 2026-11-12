package id.my.awebdev.tvbrowser

import android.net.Uri

/** Satu video yang terdeteksi di halaman yang sedang terbuka. */
data class DetectedVideo(
    val url: String,
    /** hls | dash | file | dom (dom = hanya terlihat di halaman; alamatnya tidak bisa diputar langsung). */
    val kind: String,
    /** true bila alamatnya bisa diserahkan ke pemutar khusus (stream langsung). */
    val playable: Boolean,
    val label: String,
    val pageUrl: String,
    /** Indeks elemen <video> di halaman; -1 bila hanya terlihat dari jaringan. */
    val domIndex: Int = -1
)

/**
 * Gudang hasil deteksi video untuk halaman yang sedang terbuka.
 *
 * Dua sumber: (1) lalu lintas jaringan yang diamati WebViewClient —
 * menangkap stream yang benar-benar dimuat termasuk dari pemutar
 * blob/MSE dan iframe; (2) pindai DOM elemen <video> dari MainActivity —
 * tahu video apa yang tampil dan menyediakan indeks untuk layar penuh.
 *
 * Aman diakses dari thread server maupun thread utama.
 */
class VideoDetector {

    private val lock = Any()
    private val networkItems = mutableListOf<DetectedVideo>()

    @Volatile
    private var domItems: List<DetectedVideo> = emptyList()

    fun clear() {
        synchronized(lock) { networkItems.clear() }
        domItems = emptyList()
    }

    /** Catat satu permintaan jaringan; hanya stream langsung yang disimpan. */
    fun noteRequest(rawUrl: String, pageUrl: String) {
        val (kind, playable) = classify(rawUrl) ?: return
        if (!playable) return
        synchronized(lock) {
            if (networkItems.any { it.url == rawUrl }) return
            networkItems.add(DetectedVideo(rawUrl, kind, true, labelOf(rawUrl), pageUrl))
            while (networkItems.size > MAX_ITEMS) networkItems.removeAt(0)
        }
    }

    /** Bangun entri untuk satu elemen <video> hasil pindai DOM. */
    fun domVideo(src: String, pageUrl: String, domIndex: Int): DetectedVideo {
        val (kind, playable) = classify(src) ?: ("dom" to false)
        return DetectedVideo(src, kind, playable, labelOf(src), pageUrl, domIndex)
    }

    fun setDomVideos(list: List<DetectedVideo>) {
        domItems = list
    }

    /** Gabungan: entri DOM dulu (yang terlihat), lalu stream jaringan yang belum terwakili. */
    fun snapshot(): List<DetectedVideo> {
        val net = synchronized(lock) { networkItems.toList() }
        val domUrls = domItems.map { it.url }.toSet()
        return domItems + net.filter { it.url !in domUrls }
    }

    companion object {
        private const val MAX_ITEMS = 20

        /**
         * Klasifikasi alamat: (jenis, bisa diputar langsung?).
         * null = bukan kandidat video sama sekali.
         */
        fun classify(url: String): Pair<String, Boolean>? {
            if (url.startsWith("blob:") || url.startsWith("data:")) return "dom" to false
            val path = url.substringBefore('?').substringBefore('#').lowercase()
            return when {
                path.endsWith(".m3u8") -> "hls" to true
                path.endsWith(".mpd") -> "dash" to true
                path.endsWith(".mp4") || path.endsWith(".webm") ||
                    path.endsWith(".mov") || path.endsWith(".mkv") -> "file" to true
                url.startsWith("http") -> "dom" to false
                else -> null
            }
        }

        private fun labelOf(url: String): String {
            val uri = Uri.parse(url)
            val tail = uri.lastPathSegment
            val host = uri.host ?: ""
            return when {
                !tail.isNullOrBlank() -> tail
                host.isNotBlank() -> host
                else -> url.take(60)
            }
        }
    }
}
