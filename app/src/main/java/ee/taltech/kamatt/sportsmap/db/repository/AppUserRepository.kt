package ee.taltech.kamatt.sportsmap.db.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import ee.taltech.kamatt.sportsmap.db.DbHandler
import ee.taltech.kamatt.sportsmap.db.model.AppUser
import ee.taltech.kamatt.sportsmap.db.model.LocationType


class AppUserRepository(val context: Context) {

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


    fun add(appUser: AppUser): Int {
        return db.insert(DbHandler.APP_USER_TABLE_NAME, null, appUser.getContentValues()).toInt()
    }

    private fun fetch() : Cursor {

        val columns = arrayOf(
            "_id",
            "email",
            "password",
            "firstName",
            "lastName"
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
                    cursor.getString(cursor.getColumnIndex("password")),
                    cursor.getString(cursor.getColumnIndex("firstName")),
                    cursor.getString(cursor.getColumnIndex("lastName"))
                )
            )
        }

        return appUsers
    }

    fun getUserIdByEmail(email: String): Int {
        val sqlQuery: String =
            "select rowid, * from " + DbHandler.APP_USER_TABLE_NAME + " where email='" + email + "'"
        val cursor = db.rawQuery(sqlQuery, null)
        if (cursor.moveToFirst()) {
            cursor.moveToFirst()
            return cursor.getInt(cursor.getColumnIndex("_id"))
        }

        return -1
    }




}