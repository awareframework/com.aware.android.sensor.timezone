# AWARE Timezone

[![jitpack-badge](https://jitpack.io/v/awareframework/com.aware.android.sensor.timezone.svg)](https://jitpack.io/#awareframework/com.aware.android.sensor.timezone)

The timezone sensor keeps track of the user’s current timezone.

## Public functions

### TimezoneSensor

+ `start(context: Context, config: TimezoneSensor.Config?)`: Starts the timezone sensor with the optional configuration.
+ `stop(context: Context)`: Stops the timezone service.

### TimezoneSensor.Config

Class to hold the configuration of the timezone sensor.

#### Fields

+ `sensorObserver: TimezoneObserver`: Callback for live data updates.
+ `enabled: Boolean` Sensor is enabled or not. (default = false)
+ `debug: Boolean` enable/disable logging to `Logcat`. (default = false)
+ `label: String` Label for the data. (default = "")
+ `deviceId: String` Id of the device that will be associated with the events and the sensor. (default = "")
+ `dbEncryptionKey` Encryption key for the database. (default =String? = null)
+ `dbType: Engine` Which db engine to use for saving data. (default = `Engine.DatabaseType.NONE`)
+ `dbPath: String` Path of the database. (default = "aware_wifi")
+ `dbHost: String` Host for syncing the database. (Defult = `null`)

## Broadcasts

+ `TimezoneSensor.ACTION_AWARE_TIMEZONE` broadcasted when there is new timezone information. Extra includes `TimezoneSensor.EXTRA_DATA` for the new timezone.

## Data Representations

### Timezone Data

| Field      | Type   | Description                                                                  |
| ---------- | ------ | ---------------------------------------------------------------------------- |
| timezoneId | String | the timezone ID string, i.e., “America/Los_Angeles, GMT-08:00” [(more)][1] |
| deviceId   | String | AWARE device UUID                                                            |
| label      | String | Customizable label. Useful for data calibration or traceability              |
| timestamp  | Long   | unixtime milliseconds since 1970                                             |
| timezone   | Int    | Timezone of the device                                                       |
| os         | String | Operating system of the device (ex. android)                                 |

[1]: https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html#getID()

## Example usage

```kotlin
// To start the service.
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

// To stop the service
TimezoneSensor.stop(appContext)
```

## License

Copyright (c) 2018 AWARE Mobile Context Instrumentation Middleware/Framework (http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
