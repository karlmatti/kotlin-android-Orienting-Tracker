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
        val SQL_CREATE_APP_USER_TABLE: String = getCreateTableAppUser(APP_USER_TABLE_NAME)
        val SQL_CREATE_GPS_LOCATION_TYPE_TABLE: String =
            getCreateTableGpsLocationType(GPS_LOCATION_TYPE_TABLE_NAME)
        val SQL_CREATE_GPS_SESSION_TABLE: String = getCreateTableGpsSession(GPS_SESSION_TABLE_NAME)
        val SQL_CREATE_GPS_LOCATION_TABLE: String =
            getCreateTableGpsLocation(GPS_LOCATION_TABLE_NAME)

        //  Delete table SQL statements
        const val SQL_DELETE_APP_USER_TABLE = "DROP TABLE IF EXISTS $APP_USER_TABLE_NAME"
        const val SQL_DELETE_GPS_LOCATION_TYPE_TABLE =
            "DROP TABLE IF EXISTS $GPS_LOCATION_TYPE_TABLE_NAME"
        const val SQL_DELETE_GPS_LOCATION_TABLE = "DROP TABLE IF EXISTS $GPS_LOCATION_TABLE_NAME"
        const val SQL_DELETE_GPS_SESSION_TABLE = "DROP TABLE IF EXISTS $GPS_SESSION_TABLE_NAME"

        //  Functions for create table SQL statements
        private fun getCreateTableGpsSession(TABLE_NAME: String): String {

            val ID = "_id"
            val NAME = "name"
            val DESCRIPTION = "sort"
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

            return "create table $TABLE_NAME(" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$NAME TEXT NOT NULL, " +
                    "$DESCRIPTION TEXT NOT NULL, " +
                    "$PACE_MIN DOUBLE," +
                    "$PACE_MAX DOUBLE," +
                    "$COLOR_MIN TEXT," +
                    "$COLOR_MAX TEXT," +
                    "$RECORDED_AT TEXT," +
                    "$DURATION INT," +
                    "$SPEED DOUBLE," +
                    "$DISTANCE DOUBLE," +
                    "$CLIMB DOUBLE," +
                    "$DESCENT DOUBLE," +
                    "$APP_USER_ID INT NOT NULL," +
                    "FOREIGN KEY($APP_USER_ID) REFERENCES $APP_USER_TABLE_NAME(_id)" +
                    ");"
        }

        private fun getCreateTableGpsLocation(TABLE_NAME: String): String {

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

            return "create table $TABLE_NAME(" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$RECORDED_AT TEXT NOT NULL, " +
                    "$LATITUDE DOUBLE," +
                    "$LONGITUDE DOUBLE," +
                    "$ACCURACY DOUBLE," +
                    "$ALTITUDE DOUBLE," +
                    "$VERTICAL_ACCURACY DOUBLE," +
                    "$GPS_SESSION_ID INT NOT NULL," +
                    "$GPS_LOCATION_TYPE_ID TEXT NOT NULL," +
                    "$APP_USER_ID INT NOT NULL," +
                    "FOREIGN KEY($GPS_SESSION_ID) REFERENCES $GPS_SESSION_TABLE_NAME(_id)," +
                    "FOREIGN KEY($GPS_LOCATION_TYPE_ID) REFERENCES $GPS_LOCATION_TYPE_TABLE_NAME(_id)," +
                    "FOREIGN KEY($APP_USER_ID) REFERENCES $APP_USER_TABLE_NAME(_id)" +
                    ");"

        }

        private fun getCreateTableGpsLocationType(TABLE_NAME: String): String {

            val ID = "_id"
            val NAME = "name"
            val DESCRIPTION = "description"

            return "create table $TABLE_NAME(" +
                    "$ID TEXT PRIMARY KEY, " +
                    "$NAME TEXT NOT NULL, " +
                    "$DESCRIPTION TEXT NOT NULL " +
                    ");"
        }

        private fun getCreateTableAppUser(TABLE_NAME: String): String {


            val ID = "_id"
            val EMAIL = "name"
            val PASSWORD = "description"

            return "create table $TABLE_NAME(" +
                    "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$EMAIL TEXT NOT NULL, " +
                    "$PASSWORD TEXT NOT NULL " +
                    ");"
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_APP_USER_TABLE)
        db?.execSQL(SQL_CREATE_GPS_LOCATION_TYPE_TABLE)
        db?.execSQL(SQL_CREATE_GPS_SESSION_TABLE)
        db?.execSQL(SQL_CREATE_GPS_LOCATION_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_GPS_SESSION_TABLE)
        db?.execSQL(SQL_DELETE_GPS_LOCATION_TABLE)
        db?.execSQL(SQL_DELETE_GPS_LOCATION_TYPE_TABLE)
        db?.execSQL(SQL_DELETE_APP_USER_TABLE)
        onCreate(db)
    }


}
