package ee.taltech.kamatt.sportsmap.db.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import ee.taltech.kamatt.sportsmap.db.DbHandler
import ee.taltech.kamatt.sportsmap.db.model.AppUser
import ee.taltech.kamatt.sportsmap.db.model.LocationType


class AppUserRepository(val context: Context) {
    // todo: UD methods
    private lateinit var dbHandler: DbHandler
    private lateinit var db: SQLiteDatabase

    fun open(): AppUserRepository{
        dbHandler = DbHandler(context)
        db = dbHandler.writableDatabase

        return this
    }
    fun close(){
        dbHandler.close()
    }



    fun add(appUser: AppUser){
        db.insert(DbHandler.APP_USER_TABLE_NAME, null, appUser.getContentValues())
    }

    private fun fetch() : Cursor {

        val columns = arrayOf(
            "_id",
            "email",
            "password"
        )
        val orderBy =
            "_id"

        return db.query(
            DbHandler.APP_USER_TABLE_NAME,
            columns,
            null,
            null,
            null,
            null,
            orderBy
        )
    }

    fun getAll(): List<AppUser>{
        val appUsers = ArrayList<AppUser>()
        val cursor = fetch()
        while (cursor.moveToNext()){
            appUsers.add(
                AppUser(
                    cursor.getInt(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("email")),
                    cursor.getString(cursor.getColumnIndex("password"))
                )
            )
        }

        return appUsers
    }

    fun getUserIdByEmail(email: String): Long {
        val cursor = this.db.rawQuery(
            "select rowid from " + DbHandler.APP_USER_TABLE_NAME + " where email='" + email + "'",
            null
        )
        while (cursor.moveToNext()) {
            return cursor.getLong(cursor.getColumnIndex("rowid"))
        }
        return 0
    }




}