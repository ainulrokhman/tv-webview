package id.my.awebdev.tvbrowser

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Membuat bitmap QR dari teks. Dipakai panel koneksi TV supaya HP cukup
 * memindai untuk terhubung — tidak perlu membaca atau mengetik alamat
 * dan PIN. Hanya encoder ZXing; tidak ada kamera di sisi aplikasi ini.
 */
object QrCodeHelper {

    /** Bitmap QR hitam-putih, atau null bila teks gagal di-encode. */
    fun makeBitmap(text: String, sizePx: Int = 512): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
