package com.awareframework.android.sensor.timezone.model

import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

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

    override fun toString(): String = Gson().toJson(this)
}