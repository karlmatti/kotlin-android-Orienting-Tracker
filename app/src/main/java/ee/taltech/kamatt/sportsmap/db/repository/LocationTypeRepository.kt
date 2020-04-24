package ee.taltech.kamatt.sportsmap.db.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ee.taltech.kamatt.sportsmap.db.DbHandler
import ee.taltech.kamatt.sportsmap.db.model.LocationType

class LocationTypeRepository(val context: Context) {
    // todo: UD methods
    private lateinit var dbHandler: DbHandler
    private lateinit var db: SQLiteDatabase

    fun open(): LocationTypeRepository{
        dbHandler = DbHandler(context)
        db = dbHandler.writableDatabase

        return this
    }
    fun close(){
        dbHandler.close()
    }



    fun add(locationType: LocationType){
        db.insert(DbHandler.GPS_LOCATION_TYPE_TABLE_NAME, null, locationType.getContentValues())
    }

    private fun fetch() : Cursor {

        val columns = arrayOf(
            "_id",
            "name",
            "description"
        )
        val orderBy =
            "_id"

        return db.query(
            DbHandler.GPS_LOCATION_TYPE_TABLE_NAME,
            columns,
            null,
            null,
            null,
            null,
            orderBy
        )
    }

    fun getAll(): List<LocationType>{
        val locationTypes = ArrayList<LocationType>()
        val cursor = fetch()
        while (cursor.moveToNext()){
            locationTypes.add(
                LocationType(
                    cursor.getString(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getString(cursor.getColumnIndex("name"))
                )
            )
        }

        return locationTypes
    }

}