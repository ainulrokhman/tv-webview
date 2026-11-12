package id.my.awebdev.tvbrowser

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Layar TV: satu WebView layar penuh yang seluruh kendalinya datang dari
 * halaman remote di HP lewat [BrowserController]. Activity ini juga pemilik
 * satu-satunya WebView — server tidak pernah menyentuhnya langsung.
 */
class MainActivity : AppCompatActivity(), BrowserController {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var connectPanel: LinearLayout
    private lateinit var qrImage: ImageView
    private lateinit var infoText: TextView
    private lateinit var urlText: TextView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var chromeClient: WebChromeClient

    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var defaultUa: String = ""

    private val infoHandler = Handler(Looper.getMainLooper())
    private val hideInfo = Runnable { connectPanel.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Browser TV: layar tidak tidur selama aplikasi di depan.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = findViewById(R.id.webView)
        progress = findViewById(R.id.progress)
        connectPanel = findViewById(R.id.connectPanel)
        qrImage = findViewById(R.id.qrImage)
        infoText = findViewById(R.id.infoText)
        urlText = findViewById(R.id.urlText)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        configureWebView()
        BrowserHolder.controller = this
        ServerService.start(this)

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
        webView.requestFocus()
        showConnectPanel()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Video HTML5 harus bisa memutar tanpa ketukan fisik di box.
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        defaultUa = settings.userAgentString ?: ""

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val scheme = request.url.scheme ?: return true
                // Hanya web yang dibuka di dalam aplikasi. Skema lain
                // (intent://, market://, tel:, unduhan) ditahan di sini —
                // sesuai rencana Tahap 4, jangan lompat keluar diam-diam.
                if (scheme == "http" || scheme == "https") return false
                Toast.makeText(
                    this@MainActivity,
                    "Link eksternal diblokir: $scheme",
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE
                updateInfoText()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                updateInfoText()
            }
        }

        chromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                updateInfoText()
            }

            // Video HTML5 yang meminta layar penuh (tombol fullscreen di
            // pemutar web) ditampung di wadah khusus menutupi WebView.
            override fun onShowCustomView(
                view: View,
                callback: CustomViewCallback
            ) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customCallback = callback
                fullscreenContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.INVISIBLE
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }
        webView.webChromeClient = chromeClient
    }

    private fun hideCustomView() {
        val view = customView ?: return
        fullscreenContainer.removeView(view)
        fullscreenContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
        customView = null
        customCallback?.onCustomViewHidden()
        customCallback = null
    }

    // ---- Panel koneksi (QR + alamat remote + PIN + URL aktif) ----

    private fun updateInfoText() {
        val ip = ServerService.localIpAddress() ?: "?"
        infoText.text =
            "Remote HP: http://$ip:${RemoteServer.PORT}  •  PIN ${ServerService.pin(this)}"
        // URL aktif ditampilkan utuh (bukan judul halaman) supaya bisa
        // dibaca persis — inilah "link" yang dulu tidak sempat terbaca.
        urlText.text = webView.url ?: ""
    }

    private fun showConnectPanel() {
        updateInfoText()
        // QR dibuat ulang setiap panel tampil, jadi selalu memakai
        // alamat IP box yang berlaku saat itu juga.
        val ip = ServerService.localIpAddress()
        val qr = ip?.let {
            QrCodeHelper.makeBitmap(
                "http://$it:${RemoteServer.PORT}/?pin=${ServerService.pin(this)}"
            )
        }
        if (qr != null) {
            qrImage.setImageBitmap(qr)
            qrImage.visibility = View.VISIBLE
        } else {
            qrImage.visibility = View.GONE
        }
        connectPanel.visibility = View.VISIBLE
        infoHandler.removeCallbacks(hideInfo)
        infoHandler.postDelayed(hideInfo, PANEL_TIMEOUT_MS)
    }

    private fun hideConnectPanel() {
        infoHandler.removeCallbacks(hideInfo)
        connectPanel.visibility = View.GONE
    }

    // ---- BrowserController (dipanggil server lewat thread utama) ----

    override fun openUrl(raw: String) {
        val url = UrlResolver.resolve(raw) ?: return
        webView.loadUrl(url)
    }

    override fun showInfo() {
        showConnectPanel()
    }

    override fun navigate(action: String) {
        when (action) {
            "back" -> handleBack()
            "forward" -> if (webView.canGoForward()) webView.goForward()
            "reload" -> webView.reload()
            "stop" -> webView.stopLoading()
            "home" -> webView.loadUrl(HOME_URL)
        }
    }

    override fun scrollBy(dy: Int) {
        webView.scrollBy(0, dy)
    }

    override fun tapAt(xFraction: Float, yFraction: Float) {
        val x = xFraction * webView.width
        val y = yFraction * webView.height
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(
            downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0
        )
        val up = MotionEvent.obtain(
            downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0
        )
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    override fun key(code: String) {
        when (code) {
            "back" -> handleBack()
            "up" -> sendKey(KeyEvent.KEYCODE_DPAD_UP)
            "down" -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
            "left" -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
            "right" -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            "ok" -> sendKey(KeyEvent.KEYCODE_DPAD_CENTER)
            "space" -> sendKey(KeyEvent.KEYCODE_SPACE)
        }
    }

    override fun typeText(text: String) {
        // KeyCharacterMap menghasilkan rangkaian KeyEvent yang benar
        // (termasuk Shift untuk huruf besar/simbol) untuk teks apa pun.
        val map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val events = map.getEvents(text.toCharArray())
        events?.forEach { webView.dispatchKeyEvent(it) }
    }

    override fun setUserAgent(mode: String) {
        webView.settings.userAgentString = if (mode == "desktop") DESKTOP_UA else defaultUa
        webView.reload()
    }

    override fun zoom(action: String) {
        if (action == "in") webView.zoomIn() else webView.zoomOut()
    }

    override fun currentPage(): BrowserController.PageInfo =
        BrowserController.PageInfo(webView.url ?: "", webView.title ?: "")

    // ---- Bantuan internal ----

    private fun sendKey(keyCode: Int) {
        webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun handleBack() {
        if (connectPanel.visibility == View.VISIBLE) {
            hideConnectPanel()
        } else if (customView != null) {
            hideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBack()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showConnectPanel()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        infoHandler.removeCallbacks(hideInfo)
        if (BrowserHolder.controller === this) {
            BrowserHolder.controller = null
        }
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        // Panel koneksi tampil cukup lama untuk sempat dipindai/dibaca.
        private const val PANEL_TIMEOUT_MS = 60_000L
        private const val HOME_URL = "https://html.duckduckgo.com/html/"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
