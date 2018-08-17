package com.awareframework.android.sensor.timezone

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.awareframework.android.sensor.timezone.model.TimezoneData
import com.awareframework.android.core.db.Engine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.aware.android.sensor.timezone", appContext.packageName)

        TimezoneSensor.start(appContext, TimezoneSensor.Config(
                object : TimezoneSensor.Observer {
                    override fun onTimezoneChanged(data: TimezoneData) {
                        // your code here...
                    }
                }
        ).apply {
            deviceId = UUID.randomUUID().toString()
            dbType = Engine.DatabaseType.ROOM
            // more configuration...
        })
    }
}
