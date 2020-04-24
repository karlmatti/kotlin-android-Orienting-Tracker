package ee.taltech.kamatt.sportsmap.db.model

import android.content.ContentValues

class AppUser () {
    var id: Int = 0
    lateinit var email: String
    lateinit var password: String

    constructor(id: Int, email: String, password: String) : this() {
        this.id = id
        this.email = email
        this.password = password
    }
    constructor(email: String, password: String) : this() {
        this.email = email
        this.password = password
    }
    fun getContentValues(): ContentValues {
        val values = ContentValues()
        val EMAIL = "email"
        val PASSWORD = "PASSWORD"
        values.put(EMAIL, this.email)
        values.put(PASSWORD, this.password)
        return values
    }

    override fun toString(): String {
        return "AppUser(id=$id, email='$email', password='$password')"
    }

}