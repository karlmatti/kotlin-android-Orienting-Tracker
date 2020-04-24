package ee.taltech.kamatt.sportsmap.db.model

import android.content.ContentValues

class LocationType(var id: String, var description: String, var name: String) {

    fun getContentValues(): ContentValues {
        val values = ContentValues()
        val ID = "_id"
        val NAME = "name"
        val DESCRIPTION = "description"
        values.put(ID, this.id)
        values.put(NAME, this.name)
        values.put(DESCRIPTION, this.description)
        return values
    }

    override fun toString(): String {
        return "LocationType(id='$id', description='$description', name='$name')"
    }

}