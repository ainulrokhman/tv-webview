package id.my.awebdev.tvbrowser

/**
 * Jembatan antara server remote (berjalan di thread server) dan WebView
 * (hanya boleh disentuh dari thread utama). MainActivity adalah satu-satunya
 * implementasi; server tidak pernah memegang WebView secara langsung.
 */
interface BrowserController {
    /**
     * Buka teks dari kolom alamat remote. Link selalu dibuka langsung
     * (lihat [UrlResolver]); teks yang bukan alamat dianggap pencarian.
     */
    fun openUrl(raw: String)

    /** Ke halaman awal — di sanalah blok QR + link tampil. */
    fun showInfo()

    /** Aksi navigasi: back | forward | reload | stop | home. */
    fun navigate(action: String)

    /** Gulir halaman sebesar [dy] piksel (boleh negatif). */
    fun scrollBy(dy: Int)

    /** Ketuk pada posisi relatif 0f..1f terhadap ukuran WebView. */
    fun tapAt(xFraction: Float, yFraction: Float)

    /** Geser dari satu posisi relatif ke posisi lain (pecahan 0f..1f). */
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float)

    /** Tangkapan layar WebView saat ini sebagai JPEG; null bila belum siap. */
    fun screenshotJpeg(): ByteArray?

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
