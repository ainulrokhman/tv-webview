package id.my.awebdev.tvbrowser

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

/**
 * Pemutar khusus untuk stream langsung yang terdeteksi (HLS/DASH/berkas).
 * Memutar di luar WebView biasanya lebih mulus di box berspesifikasi
 * kecil dan bebas dari iklan halaman. Stream ber-DRM akan gagal di sini —
 * pesan ditampilkan dan pengguna kembali memutar lewat halamannya.
 */
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val playerView = findViewById<PlayerView>(R.id.playerView)
        errorText = findViewById(R.id.playerError)

        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        if (url.isEmpty()) {
            Toast.makeText(this, "Alamat video kosong", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val referer = intent.getStringExtra(EXTRA_REFERER) ?: ""
        val userAgent = intent.getStringExtra(EXTRA_UA) ?: "TVBrowser"

        // Banyak server stream memeriksa Referer/User-Agent halaman
        // asalnya — tiru persis seperti saat WebView memuatnya.
        val headers = if (referer.isNotEmpty()) mapOf("Referer" to referer) else emptyMap()
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)

        val mediaItem = MediaItem.fromUri(url)
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        val mediaSource: MediaSource = when {
            path.endsWith(".m3u8") ->
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            path.endsWith(".mpd") ->
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else ->
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        val exo = ExoPlayer.Builder(this).build()
        player = exo
        playerView.player = exo
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText.text =
                    "Video tidak bisa diputar di pemutar khusus (kemungkinan DRM, " +
                        "alamat kedaluwarsa, atau server menolak). Tekan Kembali dan " +
                        "putar lewat halaman webnya."
                errorText.visibility = View.VISIBLE
            }
        })
        exo.setMediaSource(mediaSource)
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "video_url"
        const val EXTRA_REFERER = "video_referer"
        const val EXTRA_UA = "video_ua"
    }
}
