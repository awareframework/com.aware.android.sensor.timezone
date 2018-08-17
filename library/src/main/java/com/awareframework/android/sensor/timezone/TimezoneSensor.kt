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
class TimezoneSensor : AwareSensor() {

    companion object {

        const val TAG = "Aware::Timezone"

        /**
         * Broadcast event: when there is new timezone information
         */
        const val ACTION_AWARE_TIMEZONE = "ACTION_AWARE_TIMEZONE"
        const val EXTRA_DATA = "data"

        /**
         * Received event: Fire it to start the timezone sensor.
         */
        const val ACTION_AWARE_TIMEZONE_START = "com.aware.android.sensor.timezone.SENSOR_START"

        /**
         * Received event: Fire it to stop the timezone sensor.
         */
        const val ACTION_AWARE_TIMEZONE_STOP = "com.aware.android.sensor.timezone.SENSOR_STOP"

        /**
         * Received event: Fire it to sync the data with the server.
         */
        const val ACTION_AWARE_TIMEZONE_SYNC = "com.aware.android.sensor.timezone.SYNC"

        /**
         * Received event: Fire it to set the data label.
         * Use [EXTRA_LABEL] to send the label string.
         */
        const val ACTION_AWARE_TIMEZONE_SET_LABEL = "com.aware.android.sensor.timezone.SET_LABEL"

        /**
         * Label string sent in the intent extra.
         */
        const val EXTRA_LABEL = "label"

        /**
         * Start the sensor with the given optional configuration.
         */
        fun start(context: Context, config: Config? = null) {
            if (config != null)
                CONFIG.replaceWith(config)
            context.startService(Intent(context, TimezoneSensor::class.java))
        }

        /**
         * Stop the service if it's currently running.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, TimezoneSensor::class.java))
        }

        /**
         * Current configuration of the [TimezoneSensor]. Some changes in the configuration will have
         * immediate effect.
         */
        val CONFIG: Config = Config()
    }

    private var lastTimezone: String = ""

    /**
     * Listens to [Intent.ACTION_TIMEZONE_CHANGED], [ACTION_AWARE_TIMEZONE_SET_LABEL] and
     * [ACTION_AWARE_TIMEZONE_SYNC].
     */
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

        initializeDbEngine(CONFIG)

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

        logd("$ACTION_AWARE_TIMEZONE: $data")
    }

    /**
     * Sync the related fields of the db to server.
     */
    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(TimezoneData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Listens to [AwareSensor.SensorBroadcastReceiver.SENSOR_START_ENABLED], [ACTION_AWARE_TIMEZONE_STOP],
     * [AwareSensor.SensorBroadcastReceiver.SENSOR_STOP_ALL], [ACTION_AWARE_TIMEZONE_START] events.
     */
    class TimezoneSensorBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                AwareSensor.SensorBroadcastReceiver.SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        start(context)
                    }
                }

                ACTION_AWARE_TIMEZONE_STOP,
                AwareSensor.SensorBroadcastReceiver.SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    stop(context)
                }

                ACTION_AWARE_TIMEZONE_START -> {
                    start(context)
                }
            }
        }
    }

    /**
     * Configuration of the sensor.
     */
    data class Config(
            var sensorObserver: Observer? = null
    ) : SensorConfig(dbPath = "aware_timezone") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
            }
        }

        fun replaceWith(json: String) {
            replaceWith(Gson().fromJson(json, Config::class.java) ?: return)
        }
    }

    /**
     * Observer to listen to live data updates.
     */
    interface Observer {
        fun onTimezoneChanged(data: TimezoneData)
    }
}

private fun logd(text: String) {
    if (TimezoneSensor.CONFIG.debug)
        Log.d(TimezoneSensor.TAG, text)
}
