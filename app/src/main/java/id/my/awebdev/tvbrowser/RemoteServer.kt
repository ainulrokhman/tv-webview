package id.my.awebdev.tvbrowser

import android.content.Context
import android.os.Handler
import android.os.Looper
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Server HTTP lokal (hanya LAN rumah). Melayani halaman remote untuk HP dan
 * endpoint perintah yang diteruskan ke [BrowserController] di thread utama.
 * Semua endpoint selain halaman wajib membawa PIN yang benar.
 */
class RemoteServer(
    private val context: Context,
    private val pinProvider: () -> String
) : NanoHTTPD(PORT) {

    private val favorites = FavoritesStore(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pageHtml: String by lazy {
        context.assets.open("remote.html").bufferedReader().use { it.readText() }
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            route(session)
        } catch (e: Exception) {
            json(Response.Status.INTERNAL_ERROR, """{"ok":false,"error":"Server error"}""")
        }
    }

    private fun route(session: IHTTPSession): Response {
        if (session.method == Method.GET && session.uri == "/") {
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, pageHtml)
        }
        // Body POST harus di-parse dulu agar parameter form terbaca.
        if (session.method == Method.POST) {
            session.parseBody(mutableMapOf<String, String>())
        }
        val params = session.parameters
        val pin = params["pin"]?.firstOrNull() ?: ""
        if (pin != pinProvider()) {
            return json(Response.Status.FORBIDDEN, """{"ok":false,"error":"PIN salah"}""")
        }
        return when {
            session.method == Method.GET && session.uri == "/state" -> stateResponse()
            session.method == Method.GET && session.uri == "/favorites" -> favoritesResponse()
            session.method == Method.POST && session.uri == "/cmd" -> command(params)
            else -> json(Response.Status.NOT_FOUND, """{"ok":false,"error":"Tidak ditemukan"}""")
        }
    }

    private fun command(params: Map<String, List<String>>): Response {
        fun param(name: String): String = params[name]?.firstOrNull() ?: ""
        return when (param("type")) {
            "fav_add" -> {
                favorites.add(param("name"), param("url"))
                ok()
            }
            "fav_del" -> {
                favorites.remove(param("url"))
                ok()
            }
            else -> {
                val controller = BrowserHolder.controller
                    ?: return json(
                        Response.Status.SERVICE_UNAVAILABLE,
                        """{"ok":false,"error":"Layar TV belum aktif — buka aplikasinya di box"}"""
                    )
                when (param("type")) {
                    "open" -> post { it.openUrl(param("url")) }
                    "nav" -> post { it.navigate(param("action")) }
                    "scroll" -> post { it.scrollBy(param("dy").toIntOrNull() ?: 0) }
                    "tap" -> post {
                        val x = (param("x").toIntOrNull() ?: 0).coerceIn(0, 1000) / 1000f
                        val y = (param("y").toIntOrNull() ?: 0).coerceIn(0, 1000) / 1000f
                        it.tapAt(x, y)
                    }
                    "key" -> post { it.key(param("code")) }
                    "text" -> post { it.typeText(param("value")) }
                    "ua" -> post { it.setUserAgent(param("mode")) }
                    "zoom" -> post { it.zoom(param("action")) }
                    else -> return json(
                        Response.Status.BAD_REQUEST,
                        """{"ok":false,"error":"Perintah tidak dikenal"}"""
                    )
                }
                ok()
            }
        }
    }

    private fun stateResponse(): Response {
        val controller = BrowserHolder.controller
            ?: return json(Response.Status.OK, """{"ok":true,"ready":false}""")
        // currentPage() membaca WebView — wajib dari thread utama, jadi
        // thread server menunggu sebentar lewat latch.
        var info: BrowserController.PageInfo? = null
        val latch = CountDownLatch(1)
        mainHandler.post {
            info = controller.currentPage()
            latch.countDown()
        }
        latch.await(2, TimeUnit.SECONDS)
        val page = info
            ?: return json(Response.Status.OK, """{"ok":true,"ready":false}""")
        return json(
            Response.Status.OK,
            """{"ok":true,"ready":true,"url":"${escape(page.url)}","title":"${escape(page.title)}"}"""
        )
    }

    private fun favoritesResponse(): Response {
        val items = favorites.all().joinToString(",") {
            """{"name":"${escape(it.name)}","url":"${escape(it.url)}"}"""
        }
        return json(Response.Status.OK, """{"ok":true,"items":[$items]}""")
    }

    private fun post(block: (BrowserController) -> Unit) {
        val controller = BrowserHolder.controller ?: return
        mainHandler.post { block(controller) }
    }

    private fun ok(): Response = json(Response.Status.OK, """{"ok":true}""")

    private fun json(status: Response.Status, body: String): Response =
        newFixedLengthResponse(status, "application/json", body)

    private fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", " ")

    companion object {
        const val PORT = 8765
    }
}
