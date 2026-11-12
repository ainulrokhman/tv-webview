package id.my.awebdev.tvbrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Menjaga [RemoteServer] tetap hidup selama box menyala (foreground service
 * + mulai otomatis setelah booting lewat BootReceiver). PIN dibuat sekali
 * dan disimpan; alamat + PIN juga tampil di info bar MainActivity.
 */
class ServerService : Service() {

    private var server: RemoteServer? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val ip = localIpAddress() ?: "?"
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TV Browser Remote aktif")
            .setContentText("Buka http://$ip:${RemoteServer.PORT} di HP - PIN ${pin(this)}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)

        val started = RemoteServer(applicationContext) { pin(this) }
        started.start()
        server = started
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Server Remote",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "browser_server"
        private const val NOTIF_ID = 8766

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ServerService::class.java)
            )
        }

        /** PIN 4 digit, dibuat sekali lalu dipakai seterusnya. */
        fun pin(context: Context): String {
            val prefs = context.getSharedPreferences(
                FavoritesStore.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            var value = prefs.getString("pin", null)
            if (value == null) {
                value = (1000..9999).random().toString()
                prefs.edit().putString("pin", value).apply()
            }
            return value
        }

        /** Alamat IPv4 LAN pertama yang aktif (WiFi atau LAN kabel). */
        fun localIpAddress(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (!iface.isUp || iface.isLoopback) continue
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                return null
            }
            return null
        }
    }
}
