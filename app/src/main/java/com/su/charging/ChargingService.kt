package com.su.charging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.su.charging.receiver.BatteryBroadCastReceiver
import com.su.charging.util.PhoneUtils
import com.su.charging.view.preference.SoundPreference
import com.su.charging.view.fragment.SettingsFragmentCompat


class ChargingService : Service() {

    companion object {
        const val CHANNEL_NAME = "Charging Guardian Service"
        const val CHANNEL_ID = "CHANNEL_ID"
        var isOpen = false
    }

    private var bbcr: BatteryBroadCastReceiver? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (SettingsFragmentCompat.isForegroundService) {
            val nor = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
                setContentTitle(CHANNEL_NAME)
            }.build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(notificationChannel)
            }
            startForeground(110, nor)
        }
        configBattery()
        isOpen = true
        if (SettingsFragmentCompat.isNotOpenOnFull)
            PhoneUtils.checkIsOnFullScreen(this)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun configBattery() {
        bbcr = BatteryBroadCastReceiver()
        bbcr?.register(this)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        bbcr?.also {
            unregisterReceiver(it)
        }

        isOpen = false

        ChargeAudioManager.INS.release()

        SoundPreference.releaseMedia()

        PhoneUtils.closeFullCheckView(this)
        super.onDestroy()
    }
}