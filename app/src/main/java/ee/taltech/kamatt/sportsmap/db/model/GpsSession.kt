package ee.taltech.kamatt.sportsmap.db.model

import android.content.ContentValues

class GpsSession {
    var id: Int = 0
    lateinit var name: String
    lateinit var description: String
    var paceMin: Double = 0.0
    var paceMax: Double = 0.0
    var colorMin: String = "green"
    var colorMax: String = "red"
    var recordedAt: String = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    var duration: Int = 0
    var speed: Double = 0.0
    var distance: Double = 0.0
    var climb: Double = 0.0
    var descent: Double = 0.0
    var appUserId: Int = 0


    constructor(name: String, description: String) {
        this.name = name
        this.description = description
    }

    constructor(id: Int, name: String, description: String) {
        this.id = id
        this.name = name
        this.description = description
    }

    constructor(
        name: String,
        description: String,
        paceMin: Double,
        paceMax: Double,
        colorMin: String,
        colorMax: String,
        recordedAt: String,
        duration: Int,
        speed: Double,
        distance: Double,
        climb: Double,
        descent: Double,
        appUserId: Int
    ) {
        this.name = name
        this.description = description
        this.paceMin = paceMin
        this.paceMax = paceMax
        this.colorMin = colorMin
        this.colorMax = colorMax
        this.recordedAt = recordedAt
        this.duration = duration
        this.speed = speed
        this.distance = distance
        this.climb = climb
        this.descent = descent
        this.appUserId = appUserId
    }

    constructor(
        id: Int,
        name: String,
        description: String,
        paceMin: Double,
        paceMax: Double,
        colorMin: String,
        colorMax: String,
        recordedAt: String,
        duration: Int,
        speed: Double,
        distance: Double,
        climb: Double,
        descent: Double,
        appUserId: Int
    ) {
        this.id = id
        this.name = name
        this.description = description
        this.paceMin = paceMin
        this.paceMax = paceMax
        this.colorMin = colorMin
        this.colorMax = colorMax
        this.recordedAt = recordedAt
        this.duration = duration
        this.speed = speed
        this.distance = distance
        this.climb = climb
        this.descent = descent
        this.appUserId = appUserId
    }

    constructor(id: Int, paceMin: Double, paceMax: Double, colorMin: String, colorMax: String) {
        this.id = id
        this.paceMin = paceMin
        this.paceMax = paceMax
        this.colorMin = colorMin
        this.colorMax = colorMax
    }

    fun getContentValues(): ContentValues {
        val values = ContentValues()
        val NAME = "name"
        val DESCRIPTION = "description"
        val PACE_MIN = "paceMin"
        val PACE_MAX = "paceMax"
        val COLOR_MIN = "colorMin"
        val COLOR_MAX = "colorMax"
        val RECORDED_AT = "recordedAt"
        val DURATION = "duration"
        val SPEED = "speed"
        val DISTANCE = "distance"
        val CLIMB = "climb"
        val DESCENT = "descent"
        val APP_USER_ID = "appUserId"

        values.put(NAME, this.name)
        values.put(DESCRIPTION, this.description)
        values.put(PACE_MIN, this.paceMin)
        values.put(PACE_MAX, this.paceMax)
        values.put(COLOR_MIN, this.colorMin)
        values.put(COLOR_MAX, this.colorMax)
        values.put(RECORDED_AT, this.recordedAt)
        values.put(DURATION, this.duration)
        values.put(SPEED, this.speed)
        values.put(DISTANCE, this.distance)
        values.put(CLIMB, this.climb)
        values.put(DESCENT, this.descent)
        values.put(APP_USER_ID, this.appUserId)
        return values
    }

    override fun toString(): String {
        return "GpsSession(id=$id, name='$name', description='$description', paceMin=$paceMin, paceMax=$paceMax, colorMin='$colorMin', colorMax='$colorMax', recordedAt='$recordedAt', duration=$duration, speed=$speed, distance=$distance, climb=$climb, descent=$descent, appUserId=$appUserId)"
    }

}