package ee.taltech.kamatt.sportsmap

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.core.app.ActivityCompat
import ee.taltech.kamatt.sportsmap.db.model.GpsLocation
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class Utils {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        fun calculateMapPolyLineColor(
            minSpeed: Int,
            maxSpeed: Int,
            minColor: String,
            maxColor: String,
            currentPace: Float
        ): Int {
            val resultColor: Int
            resultColor = when {
                currentPace >= maxSpeed -> getAndroidColor(maxColor)
                currentPace <= minSpeed -> getAndroidColor(minColor)
                else -> {
                    //  colorPercent = 0% -> minColor ... colorPercent = 100% -> maxColor
                    val colorPercent: Float =
                        (((currentPace - minSpeed) * 100) / (maxSpeed - minSpeed)) / 100F
                    ArgbEvaluator().evaluate(
                        colorPercent,
                        getAndroidColor(maxColor),
                        getAndroidColor(minColor)
                    ) as Int
                }
            }

            return resultColor
        }

        fun getAndroidColor(colorName: String): Int {
            return when (colorName) {
                "green" -> 0xff00ff00.toInt()
                "red" -> 0xffff0000.toInt()
                "blue" -> 0xff0000ff.toInt()
                "white" -> 0xffffffff.toInt()
                else -> 0xff000000.toInt()
            }
        }



        fun getCurrentDateTime(): Long {
            return Calendar.getInstance().timeInMillis
        }

        fun longToDateString(millis: Long): String {
            return String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(millis)
                ),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(millis)
                )
            )
        }

        fun getPaceString(millis: Long, distance: Float): String {
            Log.d(TAG, "$millis-$distance")
            val speed = millis / 60.0 / distance
            if (speed > 99) return "--:--"
            val minutes = (speed).toInt()
            val seconds = ((speed - minutes) * 60).toInt()

            return minutes.toString() + ":" + (if (seconds < 10) "0" else "") + seconds.toString()

        }

        fun getPaceMinutesFloat(millis: Long, distance: Float): Float {
            if (millis < 0) {
                return 0f
            }
            return millis / 60.0f / distance
        }

        fun convertSpeedStringToDouble(speedString: String): Double {
            if (speedString == "--:--") {
                return 0.0
            } else {
                val speedStringArray = speedString.split(":")
                val minutes: Double = speedStringArray[0].toDouble()
                val seconds: Double = speedStringArray[1].toDouble()
                return (minutes * 60) + seconds
            }
        }

        fun convertDurationStringToDouble(durationString: String): Double {

            if (durationString == "--:--:--") {
                return 0.0
            } else {
                val durationStringArray = durationString.split(":")
                val hours: Double = durationStringArray[0].toDouble()
                val minutes: Double = durationStringArray[1].toDouble()
                val seconds: Double = durationStringArray[2].toDouble()
                return (hours * 3600) + (minutes * 60) + seconds
            }
        }

        @SuppressLint("SimpleDateFormat")
        fun generateGPX(
            name: String,
            points: List<GpsLocation>
        ): String {
            val header =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n"
            val name = "<name>$name</name><trkseg>\n"
            var segments = ""
            val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            for (location in points) {
                segments += "<trkpt lat=\"" + location.latitude.toString() +
                        "\" lon=\"" + location.longitude.toString() +
                        "\"><time>" + location.recordedAt +
                        "</time>" + "<type>" + location.gpsLocationTypeId + "</type>" +
                        "</trkpt>\n"
            }
            val footer = "</trkseg></trk></gpx>"

            return header + "\n" + name + "\n" + segments + "\n" + footer + "\n"

        }

        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
            READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE
        )

        fun verifyStoragePermissions(activity: Activity?) {
            // Check if we have write permission
            val permission =
                ActivityCompat.checkSelfPermission(
                    activity!!,
                    WRITE_EXTERNAL_STORAGE
                )
            if (permission != PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }
}
