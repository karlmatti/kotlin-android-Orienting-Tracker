package ee.taltech.kamatt.sportsmap.db.model

import android.content.ContentValues

class AppUser () {
    var id: Int = 0
    lateinit var email: String
    lateinit var password: String
    var firstName: String = ""
    var lastName: String = ""

    constructor(id: Int, email: String, password: String) : this() {
        this.id = id
        this.email = email
        this.password = password
    }
    constructor(email: String, password: String) : this() {
        this.email = email
        this.password = password
    }

    constructor(email: String, password: String, firstName: String, lastName: String) : this() {
        this.email = email
        this.password = password
        this.firstName = firstName
        this.lastName = lastName
    }

    constructor(
        id: Int,
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) : this() {
        this.id = id
        this.email = email
        this.password = password
        this.firstName = firstName
        this.lastName = lastName
    }
    fun getContentValues(): ContentValues {
        val values = ContentValues()
        val EMAIL = "email"
        val PASSWORD = "password"
        val FIRSTNAME = "firstName"
        val LASTNAME = "lastName"
        values.put(EMAIL, this.email)
        values.put(PASSWORD, this.password)
        values.put(FIRSTNAME, this.firstName)
        values.put(LASTNAME, this.lastName)
        return values
    }


    override fun toString(): String {
        return "AppUser(id=$id, email='$email', password='$password', firstName='$firstName', lastName='$lastName')"
    }


}