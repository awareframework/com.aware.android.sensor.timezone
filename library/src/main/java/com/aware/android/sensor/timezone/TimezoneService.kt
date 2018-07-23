package com.aware.android.sensor.timezone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.aware.android.sensor.timezone.model.TimezoneData
import com.awareframework.android.core.AwareSensor
import java.util.*


/**
 * Timezone module. Keeps track of changes in the device Timezone.
 *
 * @author  sercant
 * @date 23/07/2018
 */
class TimezoneService : AwareSensor() {

    companion object {

        const val TAG = "AwareTimezoneService"

        /**
         * Broadcasted event: when there is new timezone information
         */
        const val ACTION_AWARE_TIMEZONE = "ACTION_AWARE_TIMEZONE"
        const val EXTRA_DATA = "data"

        val config: Map<String, String> = mapOf("deviceId" to "")
    }

    private var lastTimezone = ""

    private val timezoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                Intent.ACTION_TIMEZONE_CHANGED -> retrieveTimezone()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(timezoneReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })

        logd("Timezone service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (lastTimezone.isBlank()) retrieveTimezone()

        logd("Timezone service is active.")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(timezoneReceiver)

        dbEngine?.close()

        logd("Timezone service terminated.")
    }

    /**
     * Logs the current timezone
     */
    private fun retrieveTimezone() {
        if (lastTimezone.toLowerCase() == TimeZone.getDefault().id.toLowerCase())
            return

        lastTimezone = TimeZone.getDefault().id

        val timezoneData = TimezoneData(
                timezoneId = lastTimezone
        ).apply {
            timestamp = System.currentTimeMillis()
            deviceId = config["deviceId"]!!
        }

        dbEngine?.save(timezoneData, TimezoneData.TABLE_NAME)

        sendBroadcast(Intent(ACTION_AWARE_TIMEZONE).apply {
            putExtra(EXTRA_DATA, lastTimezone)
        })
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(TimezoneData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private fun logd(text: String) {
    if (TimezoneService.config["debug"] == "")
        Log.d(TimezoneService.TAG, text)
}
