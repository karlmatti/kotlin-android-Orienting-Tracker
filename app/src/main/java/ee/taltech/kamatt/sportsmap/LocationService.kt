package ee.taltech.kamatt.sportsmap

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import java.util.*
import kotlin.math.floor


class LocationService : Service() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }


    // The desired intervals for location updates. Inexact. Updates may be more or less frequent.
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private val mLocationRequest: LocationRequest = LocationRequest()
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationCallback: LocationCallback? = null

    // last received location
    private var currentLocation: Location? = null

    private var distanceOverallDirect = 0f
    private var distanceOverallTotal = 0f
    private var tempoOverall = 0
    private var locationStart: Location? = null

    private var distanceCPDirect = 0f
    private var distanceCPTotal = 0f
    private var tempoCP = 0
    private var locationCP: Location? = null

    private var distanceWPDirect = 0f
    private var distanceWPTotal = 0f
    private var tempoWP = 0
    private var locationWP: Location? = null

    private var startTimeOverall: Long = getCurrentDateTime()
    private var startTimeCP: Long = startTimeOverall
    private var startTimeWP: Long = startTimeOverall
    private var currentTime: Long = startTimeOverall


    private var durationOverall: Long = 0
    private var durationCP: Long = 0
    private var durationWP: Long = 0

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()

        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_CP)
        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_WP)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)


        registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }

        getLastLocation()

        createLocationRequest()
        requestLocationUpdates()

    }

    fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")

        try {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(
                TAG,
                "Lost location permission. Could not request updates. $unlikely"
            )
        }
    }

    private fun onNewLocation(location: Location) {
        Log.i(TAG, "New location: $location")
        currentTime = getCurrentDateTime()
        durationOverall = currentTime - startTimeOverall
        durationCP = currentTime - startTimeCP
        durationWP = currentTime - startTimeWP
        tempoOverall = calculateTempo(durationOverall, distanceOverallTotal)
        tempoCP = calculateTempo(durationCP, distanceCPTotal)
        tempoWP = calculateTempo(durationWP, distanceWPTotal)

        if (currentLocation == null){
            locationStart = location
            locationCP = location
            locationWP = location
        } else {
            distanceOverallDirect = location.distanceTo(locationStart)
            distanceOverallTotal += location.distanceTo(currentLocation)

            distanceCPDirect = location.distanceTo(locationCP)
            distanceCPTotal += location.distanceTo(currentLocation)

            distanceWPDirect = location.distanceTo(locationWP)
            distanceWPTotal += location.distanceTo(currentLocation)
        }
        // save the location for calculations
        currentLocation = location

        showNotification()

        // broadcast new location to UI
        val intent = Intent(C.LOCATION_UPDATE_ACTION)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, location.latitude)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, location.longitude)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLDIRECT, distanceOverallDirect )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, distanceOverallTotal )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLTEMPO, tempoOverall )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLTIME, longToDateString(durationOverall))

        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPDIRECT,distanceCPDirect )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPTOTAL,distanceCPTotal )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPTEMPO, tempoCP)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPTIME, durationCP)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPDIRECT,distanceWPDirect )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPTOTAL,distanceWPTotal )
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPTEMPO, tempoWP)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPTIME, durationWP)

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }



    private fun createLocationRequest() {
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.maxWaitTime = UPDATE_INTERVAL_IN_MILLISECONDS
    }


    private fun getLastLocation() {
        try {
            mFusedLocationClient.lastLocation
                .addOnCompleteListener { task -> if (task.isSuccessful) {
                    Log.w(TAG, "task successfull")
                    if (task.result != null){
                        onNewLocation(task.result!!)
                    }
                } else {

                    Log.w(TAG, "Failed to get location." + task.exception)
                }}
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        //stop location updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)

        // remove notifications
        NotificationManagerCompat.from(this).cancelAll()


        // don't forget to unregister brodcast receiver!!!!
        unregisterReceiver(broadcastReceiver)


        // broadcast stop to UI
        val intent = Intent(C.LOCATION_UPDATE_ACTION)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }

    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        startTimeOverall = getCurrentDateTime()
        startTimeCP = startTimeOverall
        startTimeWP = startTimeOverall

        // set counters and locations to 0/null
        currentLocation = null
        locationStart = null
        locationCP = null
        locationWP = null

        distanceOverallDirect = 0f
        distanceOverallTotal = 0f
        distanceCPDirect = 0f
        distanceCPTotal = 0f
        distanceWPDirect = 0f
        distanceWPTotal = 0f


        showNotification()

        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        TODO("not implemented")
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)

    }

    fun showNotification(){
        val intentCp = Intent(C.NOTIFICATION_ACTION_CP)
        val intentWp = Intent(C.NOTIFICATION_ACTION_WP)

        val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, 0)
        val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, 0)

        val notificationsView = RemoteViews(packageName, R.layout.track_control)
        val durationStartString = longToDateString(durationOverall)

        notificationsView.setOnClickPendingIntent(R.id.imageButtonCP, pendingIntentCp)
        notificationsView.setOnClickPendingIntent(R.id.imageButtonWP, pendingIntentWp)

        notificationsView.setTextViewText(R.id.textViewOverallDirect, "%.2f".format(distanceOverallDirect))
        notificationsView.setTextViewText(R.id.textViewOverallTotal, durationStartString)
        notificationsView.setTextViewText(R.id.textViewOverallTempo, tempoOverall.toString())

        notificationsView.setTextViewText(R.id.textViewCPDirect, "%.2f".format(distanceCPDirect))
        notificationsView.setTextViewText(R.id.textViewCPTotal, "%.2f".format(distanceCPTotal))
        notificationsView.setTextViewText(R.id.textViewCPTempo, tempoCP.toString())

        notificationsView.setTextViewText(R.id.textViewWPDirect, "%.2f".format(distanceWPDirect))
        notificationsView.setTextViewText(R.id.textViewWPTotal, "%.2f".format(distanceWPTotal))
        notificationsView.setTextViewText(R.id.textViewWPTempo, tempoWP.toString())




        // construct and show notification
        val builder = NotificationCompat.Builder(applicationContext, C.NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.baseline_gps_fixed_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.setContent(notificationsView)

        // Super important, start as foreground service - ie android considers this as an active app. Need visual reminder - notification.
        // must be called within 5 secs after service starts.
        startForeground(C.NOTIFICATION_ID, builder.build())

    }


    private inner class InnerBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent!!.action)
            when(intent.action){
                C.NOTIFICATION_ACTION_WP -> {
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f
                    durationWP = 0
                    showNotification()
                }
                C.NOTIFICATION_ACTION_CP -> {
                    locationCP = currentLocation
                    distanceCPDirect = 0f
                    distanceCPTotal = 0f
                    durationCP = 0
                    showNotification()
                }
            }
        }

    }
    private fun longToDateString(milliseconds: Long): String {
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

    private fun getCurrentDateTime(): Long {
        return Calendar.getInstance().timeInMillis
    }
    private fun calculateTempo(milliseconds: Long, distanceTotal: Float): Int {

        val minutes = milliseconds / 1000 / 60 + 1
        val kilometers = distanceTotal / 1000
        var tempo = (kilometers / minutes).toDouble()

        tempo = floor(tempo)
        return tempo.toInt()
    }
}