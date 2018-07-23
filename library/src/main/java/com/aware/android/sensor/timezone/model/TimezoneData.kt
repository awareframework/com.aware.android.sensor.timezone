package com.aware.android.sensor.timezone.model

import com.awareframework.android.core.model.AwareObject

/**
 * Contains the device’s timezone history.
 *
 * @author  sercant
 * @date 23/07/2018
 */
data class TimezoneData(
        val timezoneId: String
) : AwareObject(jsonVersion = 1) {
    companion object {
        const val TABLE_NAME = "timezoneData"
    }
}