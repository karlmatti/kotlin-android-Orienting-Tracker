package ee.taltech.kamatt.sportsmap.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ee.taltech.kamatt.sportsmap.C

class DbHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = C.DB_NAME
        const val DATABASE_VERSION = C.DB_VERSION

        //  Table names
        const val APP_USER_TABLE_NAME: String = "APP_USER"
        const val GPS_LOCATION_TYPE_TABLE_NAME: String = "GPS_LOCATION_TYPE"
        const val GPS_LOCATION_TABLE_NAME: String = "GPS_LOCATION"
        const val GPS_SESSION_TABLE_NAME: String = "GPS_SESSION"

        //  Create table SQL statements
        val SQL_CREATE_APP_USER_TABLE: String = getCreateTableAppUser()
        val SQL_CREATE_GPS_LOCATION_TYPE_TABLE: String = getCreateTableGpsLocationType()
        val SQL_CREATE_GPS_SESSION_TABLE: String = getCreateTableGpsSession()
        val SQL_CREATE_GPS_LOCATION_TABLE: String = getCreateTableGpsLocation()

        //  Delete table SQL statements
        const val SQL_DELETE_APP_USER_TABLE = "DROP TABLE IF EXISTS $APP_USER_TABLE_NAME"
        const val SQL_DELETE_GPS_LOCATION_TYPE_TABLE =
            "DROP TABLE IF EXISTS $GPS_LOCATION_TYPE_TABLE_NAME"
        const val SQL_DELETE_GPS_LOCATION_TABLE = "DROP TABLE IF EXISTS $GPS_LOCATION_TABLE_NAME"
        const val SQL_DELETE_GPS_SESSION_TABLE = "DROP TABLE IF EXISTS $GPS_SESSION_TABLE_NAME"

        //  Functions for create table SQL statements
        private fun getCreateTableGpsSession(): String {

            val ID = "_id"
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

            return "create table $GPS_SESSION_TABLE_NAME(" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$NAME TEXT NOT NULL, " +
                    "$DESCRIPTION TEXT NOT NULL, " +
                    "$PACE_MIN DOUBLE," +
                    "$PACE_MAX DOUBLE," +
                    "$COLOR_MIN TEXT," +
                    "$COLOR_MAX TEXT," +
                    "$RECORDED_AT TEXT," +
                    "$DURATION STRING," +
                    "$SPEED DOUBLE," +
                    "$DISTANCE DOUBLE," +
                    "$CLIMB DOUBLE," +
                    "$DESCENT DOUBLE," +
                    "$APP_USER_ID INT NOT NULL," +
                    "FOREIGN KEY($APP_USER_ID) REFERENCES $APP_USER_TABLE_NAME(_id)" +
                    ");"
        }

        private fun getCreateTableGpsLocation(): String {

            val ID = "_id"
            val RECORDED_AT = "recordedAt"
            val LATITUDE = "latitude"
            val LONGITUDE = "longitude"
            val ACCURACY = "accuracy"
            val ALTITUDE = "altitude"
            val VERTICAL_ACCURACY = "verticalAccuracy"
            val GPS_SESSION_ID = "gpsSessionId"
            val GPS_LOCATION_TYPE_ID = "gpsLocationTypeId"
            val APP_USER_ID = "appUserId"

            return "create table $GPS_LOCATION_TABLE_NAME(" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$RECORDED_AT TEXT NOT NULL, " +
                    "$LATITUDE DOUBLE," +
                    "$LONGITUDE DOUBLE," +
                    "$ACCURACY DOUBLE," +
                    "$ALTITUDE DOUBLE," +
                    "$VERTICAL_ACCURACY DOUBLE," +
                    "$GPS_SESSION_ID LONG NOT NULL," +
                    "$GPS_LOCATION_TYPE_ID TEXT NOT NULL," +
                    "$APP_USER_ID INTEGER NOT NULL," +
                    "FOREIGN KEY($GPS_SESSION_ID) REFERENCES $GPS_SESSION_TABLE_NAME(rowid)," +
                    "FOREIGN KEY($GPS_LOCATION_TYPE_ID) REFERENCES $GPS_LOCATION_TYPE_TABLE_NAME(_id)," +
                    "FOREIGN KEY($APP_USER_ID) REFERENCES $APP_USER_TABLE_NAME(_id)" +
                    ");"

        }

        private fun getCreateTableGpsLocationType(): String {

            val ID = "_id"
            val NAME = "name"
            val DESCRIPTION = "description"

            return "create table $GPS_LOCATION_TYPE_TABLE_NAME(" +
                    "$ID TEXT PRIMARY KEY, " +
                    "$NAME TEXT NOT NULL, " +
                    "$DESCRIPTION TEXT NOT NULL " +
                    ");"
        }

        private fun getCreateTableAppUser(): String {

            val ID = "_id"
            val EMAIL = "email"
            val PASSWORD = "password"
            val FIRSTNAME = "firstName"
            val LASTNAME = "lastName"

            return "create table $APP_USER_TABLE_NAME(" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$EMAIL TEXT UNIQUE NOT NULL, " +
                    "$PASSWORD TEXT NOT NULL," +
                    "$FIRSTNAME TEXT NOT NULL," +
                    "$LASTNAME TEXT NOT NULL," +
                    ");"
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_APP_USER_TABLE)
        db?.execSQL(SQL_CREATE_GPS_LOCATION_TYPE_TABLE)
        db?.execSQL(SQL_CREATE_GPS_SESSION_TABLE)
        db?.execSQL(SQL_CREATE_GPS_LOCATION_TABLE)
        initLocationType(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_GPS_SESSION_TABLE)
        db?.execSQL(SQL_DELETE_GPS_LOCATION_TABLE)
        db?.execSQL(SQL_DELETE_GPS_LOCATION_TYPE_TABLE)
        db?.execSQL(SQL_DELETE_APP_USER_TABLE)
        onCreate(db)
    }

    private fun initLocationType(db: SQLiteDatabase?) {
        val ID = "_id"
        val NAME = "name"
        val DESCRIPTION = "description"
        db?.execSQL("INSERT INTO $GPS_LOCATION_TYPE_TABLE_NAME (" +
                "$ID, $NAME ,$DESCRIPTION)VALUES( " +
                "'${C.REST_LOCATIONID_LOC}', 'LOC', 'Regular periodic location update')")
        db?.execSQL("INSERT INTO $GPS_LOCATION_TYPE_TABLE_NAME (" +
                "$ID, $NAME ,$DESCRIPTION)VALUES( " +
                "'${C.REST_LOCATIONID_WP}', 'WP', 'Waypoint - temporary location, used as navigation aid')")
        db?.execSQL("INSERT INTO $GPS_LOCATION_TYPE_TABLE_NAME (" +
                "$ID, $NAME ,$DESCRIPTION)VALUES( " +
                "'${C.REST_LOCATIONID_CP}', 'CP', 'Checkpoint - found on terrain the location marked on the paper map')")
    }

}
