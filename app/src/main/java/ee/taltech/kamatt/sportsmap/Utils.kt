package ee.taltech.kamatt.sportsmap

import android.animation.ArgbEvaluator
import android.util.Log
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
    }
}
