package ee.taltech.kamatt.sportsmap.db.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ee.taltech.kamatt.sportsmap.db.DbHandler
import ee.taltech.kamatt.sportsmap.db.model.GpsLocation

class GpsLocationRepository(val context: Context) {
    // todo: UD methods
    private lateinit var dbHandler: DbHandler
    private lateinit var db: SQLiteDatabase

    fun open(): GpsLocationRepository{
        dbHandler = DbHandler(context)
        db = dbHandler.writableDatabase

        return this
    }
    fun close(){
        dbHandler.close()
    }



    fun add(gpsLocation: GpsLocation){
        db.insert(DbHandler.GPS_LOCATION_TABLE_NAME, null, gpsLocation.getContentValues())
    }


    private fun fetch() : Cursor {

        val columns = arrayOf(
            "_id",
            "recordedAt",
            "latitude",
            "longitude",
            "accuracy",
            "altitude",
            "verticalAccuracy",
            "gpsSessionId",
            "gpsLocationTypeId",
            "appUserId"
        )
        val orderBy =
            "_id"

        return db.query(
            DbHandler.GPS_LOCATION_TABLE_NAME,
            columns,
            null,
            null,
            null,
            null,
            orderBy
        )
    }

    fun getAll(): List<GpsLocation>{
        val gpsLocations = ArrayList<GpsLocation>()
        val cursor = fetch()
        while (cursor.moveToNext()){
            gpsLocations.add(
                GpsLocation(
                    cursor.getInt(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("recordedAt")),
                    cursor.getDouble(cursor.getColumnIndex("latitude")),
                    cursor.getDouble(cursor.getColumnIndex("longitude")),
                    cursor.getDouble(cursor.getColumnIndex("accuracy")),
                    cursor.getDouble(cursor.getColumnIndex("altitude")),
                    cursor.getDouble(cursor.getColumnIndex("verticalAccuracy")),
                    cursor.getLong(cursor.getColumnIndex("gpsSessionId")),
                    cursor.getString(cursor.getColumnIndex("gpsLocationTypeId")),
                    cursor.getLong(cursor.getColumnIndex("appUserId"))


                )
            )
        }

        return gpsLocations
    }

}