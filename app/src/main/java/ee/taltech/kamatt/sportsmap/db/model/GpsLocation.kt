package ee.taltech.kamatt.sportsmap.db.model

class GpsLocation {
    var id = 0
    var recorderAt: String
    var latitude: Double
    var longitude: Double
    var accuracy: Double
    var altitude: Double
    var verticalAccuracy: Double
    var gpsSessionId: Int
    var gpsLocationTypeId: String
    var appUserId: Int

    constructor(
        id: Int,
        recorderAt: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        altitude: Double,
        verticalAccuracy: Double,
        gpsSessionId: Int,
        gpsLocationTypeId: String,
        appUserId: Int
    ) {
        this.id = id
        this.recorderAt = recorderAt
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
        recorderAt: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        altitude: Double,
        verticalAccuracy: Double,
        gpsSessionId: Int,
        gpsLocationTypeId: String,
        appUserId: Int
    ) {
        this.recorderAt = recorderAt
        this.latitude = latitude
        this.longitude = longitude
        this.accuracy = accuracy
        this.altitude = altitude
        this.verticalAccuracy = verticalAccuracy
        this.gpsSessionId = gpsSessionId
        this.gpsLocationTypeId = gpsLocationTypeId
        this.appUserId = appUserId
    }

}