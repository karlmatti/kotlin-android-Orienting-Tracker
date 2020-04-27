package ee.taltech.kamatt.sportsmap

class C {
    companion object {
        private const val PREFIX = "ee.taltech.kamatt."
        const val NOTIFICATION_CHANNEL = "default_channel"
        const val NOTIFICATION_ACTION_WP = PREFIX + "wp"
        const val NOTIFICATION_ACTION_CP = PREFIX + "cp"

        const val LOCATION_UPDATE_ACTION = PREFIX + "location_update"
        const val LOCATION_UPDATE_STOP = PREFIX + "location_stop"

        const val LOCATION_UPDATE_ACTION_LATITUDE = PREFIX + "location_update.latitude"
        const val LOCATION_UPDATE_ACTION_LONGITUDE = PREFIX + "location_update.longitude"

        const val LOCATION_UPDATE_ACTION_OVERALLDIRECT = PREFIX + "location_update.overalldirect"
        const val LOCATION_UPDATE_ACTION_OVERALLTOTAL = PREFIX + "location_update.overalltotal"
        const val LOCATION_UPDATE_ACTION_OVERALLTEMPO = PREFIX + "location_update.overalltempo"
        const val LOCATION_UPDATE_ACTION_OVERALLTIME = PREFIX + "location_update.overalltime"

        const val LOCATION_UPDATE_ACTION_CPDIRECT = PREFIX + "location_update.cpdirect"
        const val LOCATION_UPDATE_ACTION_CPTOTAL = PREFIX + "location_update.cptotal"
        const val LOCATION_UPDATE_ACTION_CPTEMPO = PREFIX + "location_update.cptempo"
        const val LOCATION_UPDATE_ACTION_CPTIME = PREFIX + "location_update.cptime"

        const val LOCATION_UPDATE_ACTION_WPDIRECT = PREFIX + "location_update.wpdirect"
        const val LOCATION_UPDATE_ACTION_WPTOTAL = PREFIX + "location_update.wptotal"
        const val LOCATION_UPDATE_ACTION_WPTEMPO = PREFIX + "location_update.wptempo"
        const val LOCATION_UPDATE_ACTION_WPTIME = PREFIX + "location_update.wptime"

        const val CURRENT_SESSION_ID = PREFIX + "current_session_id"
        const val PACE_MIN = PREFIX + "pace_min"
        const val PACE_MAX = PREFIX + "pace_max"
        const val COLOR_MIN = PREFIX + "color_min"
        const val COLOR_MAX = PREFIX + "color_max"

        const val NOTIFICATION_ID = 4321
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34

        const val REST_BASE_URL = "https://sportmap.akaver.com/api/v1.0/"
        const val REST_USERNAME = "kamatt@taltech.ee"
        const val REST_PASSWORD = "Lambi.ats4321"

        const val DB_NAME = "sportsmap.db"
        const val DB_VERSION = 1

        const val REST_LOCATIONID_LOC = "00000000-0000-0000-0000-000000000001"
        const val REST_LOCATIONID_WP = "00000000-0000-0000-0000-000000000002"
        const val REST_LOCATIONID_CP = "00000000-0000-0000-0000-000000000003"


    }
}