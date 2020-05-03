package ee.taltech.kamatt.sportsmap

import android.animation.ArgbEvaluator
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*


class Utils {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private var mapPolylineOptions: PolylineOptions? = null

        @Synchronized
        fun getMapPolylineOptions(): PolylineOptions {
            if (mapPolylineOptions == null) {
                mapPolylineOptions = PolylineOptions()
            }

            return mapPolylineOptions!!
        }

        fun clearMapPolylineOptions() {
            mapPolylineOptions = PolylineOptions()
        }

        fun calculateMapPolyLineColor(
            minSpeed: Int,
            maxSpeed: Int,
            minColor: String,
            maxColor: String,
            currentTempo: Float
        ): Int {
            val resultColor: Int
            resultColor = when {
                currentTempo >= maxSpeed -> getAndroidColor(maxColor)
                currentTempo <= minSpeed -> getAndroidColor(minColor)
                else -> {
                    //  colorPercent = 0% -> minColor ... colorPercent = 100% -> maxColor
                    val colorPercent: Float =
                        (((currentTempo - minSpeed) * 100) / (maxSpeed - minSpeed)) / 100F
                    ArgbEvaluator().evaluate(
                        colorPercent,
                        getAndroidColor(maxColor),
                        getAndroidColor(minColor)
                    ) as Int
                }
            }

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

        fun addToMapPolylineOptions(lat: Double, lon: Double) {

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
                    "$hours:$minutes:" + seconds.toString()[0] + seconds.toString()[1]
                } else {
                    "$hours:$minutes:$seconds"
                }

            } else {
                "00:00:00"
            }
        }

        fun getPaceString(millis: Long, distance: Float): String {
            Log.d(TAG, "$millis-$distance")
            val speed = millis / 60.0 / distance
            if (speed > 99) return "--:--"
            val minutes = (speed).toInt()
            val seconds = ((speed - minutes) * 60).toInt()

            return minutes.toString() + ":" + (if (seconds < 10) "0" else "") + seconds.toString()

        }

        fun getPaceInteger(millis: Long, distance: Float): Int {
            val speed = millis / 60.0 / distance
            if (speed > 99) return 0
            val minutes = (speed).toInt()
            val seconds = ((speed - minutes) * 60).toInt()

            return (minutes * 60) + seconds

        }

        fun getPaceMinutesFloat(millis: Long, distance: Float): Float {

            if (millis < 0) {
                return 0f
            }
            return millis / 60.0f / distance
        }
    }
}
