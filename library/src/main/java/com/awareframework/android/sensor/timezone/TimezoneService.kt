package com.awareframework.android.sensor.timezone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.awareframework.android.sensor.timezone.model.TimezoneData
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.db.Engine
import com.awareframework.android.core.model.SensorConfig
import com.google.gson.Gson
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
         * Broadcast event: when there is new timezone information
         */
        const val ACTION_AWARE_TIMEZONE = "ACTION_AWARE_TIMEZONE"
        const val EXTRA_DATA = "data"

        const val ACTION_AWARE_TIMEZONE_START = "com.aware.android.sensor.timezone.SENSOR_START"
        const val ACTION_AWARE_TIMEZONE_STOP = "com.aware.android.sensor.timezone.SENSOR_STOP"

        const val ACTION_AWARE_TIMEZONE_SET_LABEL = "com.aware.android.sensor.timezone.SET_LABEL"
        const val ACTION_AWARE_TIMEZONE_SYNC = "com.aware.android.sensor.timezone.SYNC"
        const val EXTRA_LABEL = "label"

        fun startService(context: Context, config: TimezoneConfig? = null) {
            if (config != null)
                CONFIG.replaceWith(config)
            context.startService(Intent(context, TimezoneService::class.java))
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, TimezoneService::class.java))
        }

        val CONFIG: TimezoneConfig = TimezoneConfig()
    }

    private var lastTimezone: String = ""

    private val timezoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                Intent.ACTION_TIMEZONE_CHANGED -> retrieveTimezone()

                ACTION_AWARE_TIMEZONE_SET_LABEL -> {
                    intent.getStringExtra(EXTRA_LABEL)?.let {
                        CONFIG.label = it
                    }
                }

                ACTION_AWARE_TIMEZONE_SYNC -> onSync(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        dbEngine = Engine.Builder(this)
                .setType(CONFIG.dbType)
                .setPath(CONFIG.dbPath)
                .setHost(CONFIG.dbHost)
                .setEncryptionKey(CONFIG.dbEncryptionKey)
                .build()

        registerReceiver(timezoneReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(ACTION_AWARE_TIMEZONE_SET_LABEL)
            addAction(ACTION_AWARE_TIMEZONE_SYNC)
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

        val data = TimezoneData(
                timezoneId = lastTimezone
        ).apply {
            timestamp = System.currentTimeMillis()
            deviceId = CONFIG.deviceId
        }

        dbEngine?.save(data, TimezoneData.TABLE_NAME)

        CONFIG.sensorObserver?.onTimezoneChanged(data)

        sendBroadcast(Intent(ACTION_AWARE_TIMEZONE).apply {
            putExtra(EXTRA_DATA, lastTimezone)
        })
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(TimezoneData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    class TimezoneServiceBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                AwareSensor.SensorBroadcastReceiver.SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        startService(context)
                    }
                }

                ACTION_AWARE_TIMEZONE_STOP,
                AwareSensor.SensorBroadcastReceiver.SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    stopService(context)
                }

                ACTION_AWARE_TIMEZONE_START -> {
                    startService(context)
                }
            }
        }
    }

    data class TimezoneConfig(
            var sensorObserver: TimezoneObserver? = null
    ) : SensorConfig(dbPath = "aware_timezone") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is TimezoneConfig) {
                sensorObserver = config.sensorObserver
            }
        }

        fun replaceWith(json: String) {
            replaceWith(Gson().fromJson(json, TimezoneConfig::class.java) ?: return)
        }
    }

    interface TimezoneObserver {
        fun onTimezoneChanged(data: TimezoneData)
    }
}

private fun logd(text: String) {
    if (TimezoneService.CONFIG.debug)
        Log.d(TimezoneService.TAG, text)
}
