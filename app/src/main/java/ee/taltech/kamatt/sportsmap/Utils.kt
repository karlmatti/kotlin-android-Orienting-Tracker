package ee.taltech.kamatt.sportsmap

import android.graphics.Color
import android.widget.Switch
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.nio.file.Files.size
import java.util.*
import kotlin.math.floor


class Utils {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private var mapPolylineOptions: PolylineOptions? = null

        @Synchronized
        fun getMapPolylineOptions(): PolylineOptions {
            if (mapPolylineOptions == null) {
                mapPolylineOptions = PolylineOptions()
            }

            return mapPolylineOptions!!;
        }

        fun clearMapPolylineOptions(){
            mapPolylineOptions = PolylineOptions()
        }

        fun setMapPolyLineColor(androidColor: Int){
            mapPolylineOptions!!.color(androidColor)
        }
        fun calculateMapPolyLineColor(minSpeed: Int, maxSpeed: Int, minColor: String, maxColor: String, currentTempo: Int): Int {
            //  colorPercent = 0% -> minColor ... colorPercent = 100% -> maxColor
            val colorPercent: Int = (((currentTempo - minSpeed) * 100) / maxSpeed - minSpeed)
            return colorPercent
        }

        fun addToMapPolylineOptions(lat: Double, lon: Double){

            getMapPolylineOptions().add(LatLng(lat, lon))
        }

        fun getCurrentDateTime(): Long {
            return Calendar.getInstance().timeInMillis
        }
        fun longToDateString(milliseconds: Long): String {
            return if (milliseconds > 0) {
                val hours = milliseconds / 1000 / 60 / 60
                val minutes = milliseconds / 1000 / 60
                val seconds = milliseconds / 1000 % 60

                if (seconds > 99) {
                    "$hours:$minutes:" + seconds.toString().get(0) + seconds.toString().get(1)
                } else {
                    "$hours:$minutes:$seconds"
                }

            } else {
                "00:00:00"
            }
        }


        fun calculateTempo(milliseconds: Long, distanceTotal: Float): Int {

            val minutes = milliseconds / 1000 / 60 + 1
            val kilometers = distanceTotal / 1000
            var tempo = (kilometers / minutes).toDouble()

            tempo = floor(tempo)
            return tempo.toInt()
        }

    }
}
