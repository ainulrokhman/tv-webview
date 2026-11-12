package id.my.awebdev.tvbrowser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Menyalakan server remote setelah box selesai booting. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ServerService.start(context)
        }
    }
}
