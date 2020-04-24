package ee.taltech.kamatt.sportsmap.db.model

import android.content.ContentValues

class GpsLocation {


    var id = 0
    var recordedAt: String
    var latitude: Double
    var longitude: Double
    var accuracy: Double
    var altitude: Double
    var verticalAccuracy: Double
    var gpsSessionId: Long
    var gpsLocationTypeId: String
    var appUserId: Int

    constructor(
        id: Int,
        recordedAt: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        altitude: Double,
        verticalAccuracy: Double,
        gpsSessionId: Long,
        gpsLocationTypeId: String,
        appUserId: Int
    ) {
        this.id = id
        this.recordedAt = recordedAt
        this.latitude = latitude
        this.longitude = longitude
        this.accuracy = accuracy
        this.altitude = altitude
        this.verticalAccuracy = verticalAccuracy
        this.gpsSessionId = gpsSessionId
        this.gpsLocationTypeId = gpsLocationTypeId
        this.appUserId = appUserId
    }

    constructor(
        recordedAt: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        altitude: Double,
        verticalAccuracy: Double,
        gpsSessionId: Long,
        gpsLocationTypeId: String,
        appUserId: Int
    ) {
        this.recordedAt = recordedAt
        this.latitude = latitude
        this.longitude = longitude
        this.accuracy = accuracy
        this.altitude = altitude
        this.verticalAccuracy = verticalAccuracy
        this.gpsSessionId = gpsSessionId
        this.gpsLocationTypeId = gpsLocationTypeId
        this.appUserId = appUserId
    }


    fun getContentValues(): ContentValues {

        val RECORDED_AT = "recordedAt"
        val LATITUDE = "latitude"
        val LONGITUDE = "longitude"
        val ACCURACY = "accuracy"
        val ALTITUDE = "altitude"
        val VERTICAL_ACCURACY = "verticalAccuracy"
        val GPS_SESSION_ID = "gpsSessionId"
        val GPS_LOCATION_TYPE_ID = "gpsLocationTypeId"
        val APP_USER_ID = "appUserId"

        val values = ContentValues()
        values.put(RECORDED_AT, this.recordedAt)
        values.put(LATITUDE, this.latitude)
        values.put(LONGITUDE, this.longitude)
        values.put(ACCURACY, this.accuracy)
        values.put(ALTITUDE, this.altitude)
        values.put(VERTICAL_ACCURACY, this.verticalAccuracy)
        values.put(GPS_SESSION_ID, this.gpsSessionId)
        values.put(GPS_LOCATION_TYPE_ID, this.gpsLocationTypeId)
        values.put(APP_USER_ID, this.appUserId)

        return values
    }

    override fun toString(): String {
        return "GpsLocation(id=$id, recordedAt='$recordedAt', latitude=$latitude, longitude=$longitude, accuracy=$accuracy, altitude=$altitude, verticalAccuracy=$verticalAccuracy, gpsSessionId=$gpsSessionId, gpsLocationTypeId='$gpsLocationTypeId', appUserId=$appUserId)"
    }

}