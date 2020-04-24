package ee.taltech.kamatt.sportsmap.db.model

class GpsSession {
    var id: Int = 0
    var name: String
    var description: String
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

    constructor(name: String, description: String) {
        this.name = name
        this.description = description
    }

    constructor(id: Int, name: String, description: String) {
        this.id = id
        this.name = name
        this.description = description
    }


}