package ee.taltech.kamatt.sportsmap

import android.animation.ArgbEvaluator
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
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

            val resultColor: Int
            when {
                currentTempo >= maxSpeed -> resultColor = getAndroidColor(maxColor)
                currentTempo <= minSpeed -> resultColor = getAndroidColor(minColor)
                else -> {
                    //  colorPercent = 0% -> minColor ... colorPercent = 100% -> maxColor
                    val colorPercent: Float = (((currentTempo - minSpeed) * 100) / maxSpeed - minSpeed) / 100F
                    resultColor = ArgbEvaluator().evaluate(colorPercent, getAndroidColor(maxColor), getAndroidColor(minColor)) as Int
                    Log.d("calcMapPolyLineColor", "colorPercent: $colorPercent")
                }
            }
            Log.d("calcMapPolyLineColor", "resultColor: $resultColor")

            return resultColor
        }

        private fun getAndroidColor(colorName: String): Int {
            return when (colorName) {
                "green" -> 0xff00ff00.toInt()
                "red" -> 0xffff0000.toInt()
                "blue" -> 0xff0000ff.toInt()
                else -> 0xff000000.toInt()
            }
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

        // tempo km/min
        fun calculateTempo(milliseconds: Long, distanceTotal: Float): Float {

            val minutes = milliseconds / 1000 / 60 + 1
            val kilometers = distanceTotal / 1000
            var tempo = (kilometers / minutes).toDouble()

            tempo = floor(tempo)
            return String.format("%.2f", tempo).toFloat()
        }
        fun getPaceString(millis: Long, distance: Float): String {
            Log.d(TAG, millis.toString() + '-' + distance.toString())
            val speed = millis / 60.0 / distance
            if (speed > 99) return "--:--"
            val minutes = (speed ).toInt();
            val seconds = ((speed - minutes) * 60).toInt()

            return minutes.toString() + ":" + (if (seconds < 10)  "0" else "") +seconds.toString();

        }
        fun getPaceInteger(millis: Long, distance: Float): Int {
            Log.d(TAG, millis.toString() + '-' + distance.toString())
            val speed = millis / 60.0 / distance
            if (speed > 99) return 0
            val minutes = (speed ).toInt();
            val seconds = ((speed - minutes) * 60).toInt()

            return minutes

        }
    }
}
