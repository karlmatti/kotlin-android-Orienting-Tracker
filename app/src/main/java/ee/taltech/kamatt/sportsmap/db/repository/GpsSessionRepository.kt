package ee.taltech.kamatt.sportsmap.db.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ee.taltech.kamatt.sportsmap.db.DbHandler
import ee.taltech.kamatt.sportsmap.db.model.GpsSession


class GpsSessionRepository(val context: Context) {
    // todo: UD methods
    private lateinit var dbHandler: DbHandler
    private lateinit var db: SQLiteDatabase

    fun open(): GpsSessionRepository{
        dbHandler = DbHandler(context)
        db = dbHandler.writableDatabase

        return this
    }
    fun close(){
        dbHandler.close()
    }



    fun add(gpsSession: GpsSession){
        db.insert(DbHandler.GPS_SESSION_TABLE_NAME, null, gpsSession.getContentValues())
    }


    private fun fetch() : Cursor {

        val columns = arrayOf(
            "_id",
            "name",
            "description",
            "paceMin",
            "paceMax",
            "colorMin",
            "colorMax",
            "recordedAt",
            "duration",
            "speed",
            "distance",
            "climb",
            "descent",
            "appUserId"
        )
        val orderBy =
            "_id"

        return db.query(
            DbHandler.GPS_SESSION_TABLE_NAME,
            columns,
            null,
            null,
            null,
            null,
            orderBy
        )
    }

    fun getAll(): List<GpsSession>{
        val gpsSessions = ArrayList<GpsSession>()
        val cursor = fetch()
        while (cursor.moveToNext()){
            gpsSessions.add(
                GpsSession(
                    cursor.getInt(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getDouble(cursor.getColumnIndex("paceMin")),
                    cursor.getDouble(cursor.getColumnIndex("paceMax")),
                    cursor.getString(cursor.getColumnIndex("colorMin")),
                    cursor.getString(cursor.getColumnIndex("colorMax")),
                    cursor.getString(cursor.getColumnIndex("recordedAt")),
                    cursor.getInt(cursor.getColumnIndex("duration")),
                    cursor.getDouble(cursor.getColumnIndex("speed")),
                    cursor.getDouble(cursor.getColumnIndex("distance")),
                    cursor.getDouble(cursor.getColumnIndex("climb")),
                    cursor.getDouble(cursor.getColumnIndex("descent")),
                    cursor.getInt(cursor.getColumnIndex("appUserId"))
                )
            )
        }

        return gpsSessions
    }

}