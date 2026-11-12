package id.my.awebdev.tvbrowser

/**
 * Jembatan antara server remote (berjalan di thread server) dan WebView
 * (hanya boleh disentuh dari thread utama). MainActivity adalah satu-satunya
 * implementasi; server tidak pernah memegang WebView secara langsung.
 */
interface BrowserController {
    /** Buka alamat atau kata pencarian (dinormalisasi di implementasi). */
    fun openUrl(raw: String)

    /** Aksi navigasi: back | forward | reload | stop | home. */
    fun navigate(action: String)

    /** Gulir halaman sebesar [dy] piksel (boleh negatif). */
    fun scrollBy(dy: Int)

    /** Ketuk pada posisi relatif 0f..1f terhadap ukuran WebView. */
    fun tapAt(xFraction: Float, yFraction: Float)

    /** Tombol arah & kawan-kawan: up | down | left | right | ok | back | space. */
    fun key(code: String)

    /** Ketik teks ke elemen halaman yang sedang fokus. */
    fun typeText(text: String)

    /** Ganti User-Agent: default | desktop. */
    fun setUserAgent(mode: String)

    /** Zoom: in | out. */
    fun zoom(action: String)

    fun currentPage(): PageInfo

    data class PageInfo(val url: String, val title: String)
}

/** Pemegang implementasi aktif; diisi MainActivity saat tampil, dikosongkan saat ditutup. */
object BrowserHolder {
    @Volatile
    var controller: BrowserController? = null
}
