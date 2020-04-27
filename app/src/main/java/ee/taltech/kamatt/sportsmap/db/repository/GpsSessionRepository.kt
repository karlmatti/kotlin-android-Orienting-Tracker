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


    fun add(gpsSession: GpsSession): Long {
        return db.insert(DbHandler.GPS_SESSION_TABLE_NAME, null, gpsSession.getContentValues())
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
                    cursor.getString(cursor.getColumnIndex("duration")),
                    cursor.getString(cursor.getColumnIndex("speed")),
                    cursor.getDouble(cursor.getColumnIndex("distance")),
                    cursor.getDouble(cursor.getColumnIndex("climb")),
                    cursor.getDouble(cursor.getColumnIndex("descent")),
                    cursor.getInt(cursor.getColumnIndex("appUserId"))
                )
            )
        }

        return gpsSessions
    }

    fun getSessionsByUserId(userId: Int): List<GpsSession> {
        val sqlQuery: String =
            "select * from " + DbHandler.GPS_SESSION_TABLE_NAME + " where appUserId='" + userId.toString() + "'"
        val cursor = db.rawQuery(sqlQuery, null)
        val gpsSessions = ArrayList<GpsSession>()
        while (cursor.moveToNext()) {
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
                    cursor.getString(cursor.getColumnIndex("duration")),
                    cursor.getString(cursor.getColumnIndex("speed")),
                    cursor.getDouble(cursor.getColumnIndex("distance")),
                    cursor.getDouble(cursor.getColumnIndex("climb")),
                    cursor.getDouble(cursor.getColumnIndex("descent")),
                    cursor.getInt(cursor.getColumnIndex("appUserId"))
                )
            )
        }
        cursor.close()
        return gpsSessions

    }

    /**
     * Remove a session from database by _id
     *
     * @param sessionId to remove
     */
    fun removeSessionById(sessionId: Int) {
        db.execSQL("DELETE FROM " + DbHandler.GPS_SESSION_TABLE_NAME + " WHERE _id= '" + sessionId + "'")
    }

    fun updateSession(gpsSession: GpsSession) {
        db.update(
            DbHandler.GPS_SESSION_TABLE_NAME,
            gpsSession.getContentValues(),
            "_id=" + gpsSession.id,
            null
        )
    }

    fun getSessionById(id: Int): GpsSession {
        val sqlQuery: String =
            "select * from " + DbHandler.GPS_SESSION_TABLE_NAME + " where _id='" + id.toString() + "'"
        val cursor = db.rawQuery(sqlQuery, null)
        var returnedGpsSession = GpsSession("default name", "default desc")
        while (cursor.moveToNext()) {
            returnedGpsSession = GpsSession(
                cursor.getInt(cursor.getColumnIndex("_id")),
                cursor.getString(cursor.getColumnIndex("name")),
                cursor.getString(cursor.getColumnIndex("description")),
                cursor.getDouble(cursor.getColumnIndex("paceMin")),
                cursor.getDouble(cursor.getColumnIndex("paceMax")),
                cursor.getString(cursor.getColumnIndex("colorMin")),
                cursor.getString(cursor.getColumnIndex("colorMax")),
                cursor.getString(cursor.getColumnIndex("recordedAt")),
                cursor.getString(cursor.getColumnIndex("duration")),
                cursor.getString(cursor.getColumnIndex("speed")),
                cursor.getDouble(cursor.getColumnIndex("distance")),
                cursor.getDouble(cursor.getColumnIndex("climb")),
                cursor.getDouble(cursor.getColumnIndex("descent")),
                cursor.getInt(cursor.getColumnIndex("appUserId"))
            )


        }
        cursor.close()
        return returnedGpsSession
    }


}