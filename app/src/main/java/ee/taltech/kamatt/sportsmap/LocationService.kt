package ee.taltech.kamatt.sportsmap

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import ee.taltech.kamatt.sportsmap.db.model.GpsLocation
import ee.taltech.kamatt.sportsmap.db.model.GpsSession
import ee.taltech.kamatt.sportsmap.db.repository.AppUserRepository
import ee.taltech.kamatt.sportsmap.db.repository.GpsLocationRepository
import ee.taltech.kamatt.sportsmap.db.repository.GpsSessionRepository
import ee.taltech.kamatt.sportsmap.db.repository.LocationTypeRepository
import java.io.Serializable


class LocationService : Service() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
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
    private var tempoOverall: String = "--:--"
    private var locationStart: Location? = null

    private var distanceCPDirect = 0f
    private var distanceCPTotal = 0f
    private var tempoCP: String = ""
    private var locationCP: Location? = null

    private var distanceWPDirect = 0f
    private var distanceWPTotal = 0f
    private var tempoWP: String = ""
    private var locationWP: Location? = null

    private var startTimeOverall: Long = Utils.getCurrentDateTime()
    private var startTimeCP: Long = startTimeOverall
    private var startTimeWP: Long = startTimeOverall


    private var durationOverall: Long = 0
    private var durationCP: Long = 0
    private var durationWP: Long = 0

    private var jwt: String? = null
    private var currentRestSessionId: String? = null


    private lateinit var locationTypeRepository: LocationTypeRepository
    private lateinit var gpsSessionRepository: GpsSessionRepository
    private lateinit var gpsLocationRepository: GpsLocationRepository
    private lateinit var appUserRepository: AppUserRepository

    private var currentDbUserId: Int = 0
    private var currentDbSessionId: Long = -1
    private lateinit var currentDbSession: GpsSession

    private var polylineOptionsList: MutableList<PolylineOptions>? = null
    private var paceMin: Double = 120.0
    private var paceMax: Double = 480.0
    private var colorMin: String = "blue"
    private var colorMax: String = "red"

    private var listOfCPMarkerLatLngs: MutableList<LatLng>? = null
    private var lastWPMarkerLatLng: LatLng? = null

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

        locationTypeRepository = LocationTypeRepository(this).open()
        gpsSessionRepository = GpsSessionRepository(this).open()
        gpsLocationRepository = GpsLocationRepository(this).open()
        appUserRepository = AppUserRepository(this).open()
        currentDbUserId = appUserRepository.getUserIdByEmail(C.REST_USERNAME)
        Log.d("currentUserId", currentDbUserId.toString())

        //getRestToken()
        getLastLocation()

        createLocationRequest()
        requestLocationUpdates()

    }

    private fun getRestToken() {
        val handler = WebApiSingletonHandler.getInstance(applicationContext)

        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("email", C.REST_USERNAME)
        requestJsonParameters.put("password", C.REST_PASSWORD)


        val httpRequest = JsonObjectRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "account/login",
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
                jwt = response.getString("token")
                startRestTrackingSession(currentDbSession)
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
            }
        )

        handler.addToRequestQueue(httpRequest)

    }

    private fun startRestTrackingSession(currSession: GpsSession) {
        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("name", currSession.name)
        requestJsonParameters.put("description", currSession.description)
        requestJsonParameters.put("recordedAt", currSession.recordedAt)
        requestJsonParameters.put(
            "duration",
            Utils.convertDurationStringToDouble(currSession.duration)
        )
        requestJsonParameters.put("speed", Utils.convertSpeedStringToDouble(currSession.speed))
        requestJsonParameters.put("distance", currSession.distance)
        requestJsonParameters.put("climb", currSession.climb)
        requestJsonParameters.put("descent", currSession.descent)
        requestJsonParameters.put("paceMin", currSession.paceMin)
        requestJsonParameters.put("paceMax", currSession.paceMax)

        val httpRequest = object : JsonObjectRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "GpsSessions",
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
                currentRestSessionId = response.getString("id")
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                return headers
            }
        }


        handler.addToRequestQueue(httpRequest)
    }

    private fun saveRestLocation(location: Location, location_type: String) {
        if (jwt == null || currentRestSessionId == null) {
            return
        }

        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        val requestJsonParameters = JSONObject()

        requestJsonParameters.put("recordedAt", dateFormat.format(Date(location.time)))

        requestJsonParameters.put("latitude", location.latitude)
        requestJsonParameters.put("longitude", location.longitude)
        requestJsonParameters.put("accuracy", location.accuracy)
        requestJsonParameters.put("altitude", location.altitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestJsonParameters.put("verticalAccuracy", location.verticalAccuracyMeters)
        }
        requestJsonParameters.put("gpsSessionId", currentRestSessionId)
        requestJsonParameters.put("gpsLocationTypeId", location_type)


        val httpRequest = object : JsonObjectRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "GpsLocations",
            requestJsonParameters,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)
    }


    private fun requestLocationUpdates() {
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

        if (location.accuracy > 100) {
            Log.d(TAG, "location.accuracy is ${location.accuracy}")
            return
        }


        if (currentLocation == null) {
            locationStart = location
            locationCP = location
            locationWP = location
        } else {
            if (currentLocation!!.distanceTo(location) > 100.0f) {
                Log.d("pv kaugused:", "lappes")
                return
            }
            Log.d("pv kaugused:", currentLocation!!.distanceTo(location).toString())

            distanceOverallDirect = location.distanceTo(locationStart)
            distanceOverallTotal += location.distanceTo(currentLocation)
            tempoOverall = Utils.getPaceString(durationOverall, distanceOverallTotal)
            durationOverall += (location.time - currentLocation!!.time)

            distanceCPDirect = location.distanceTo(locationCP)
            distanceCPTotal += location.distanceTo(currentLocation)
            tempoCP = Utils.getPaceString(durationCP, distanceCPTotal)
            durationCP += (location.time - currentLocation!!.time)


            distanceWPDirect = location.distanceTo(locationWP)
            distanceWPTotal += location.distanceTo(currentLocation)
            tempoWP = Utils.getPaceString(durationWP, distanceWPTotal)
            durationWP += (location.time - currentLocation!!.time)

            val distanceFromLastPoint: Float = currentLocation!!.distanceTo(location)
            val newTimeDifference = location.time - currentLocation!!.time
            val tempo: Float = Utils.getPaceMinutesFloat(newTimeDifference, distanceFromLastPoint)
            updateCurrentSession()
            val newColor = Utils.calculateMapPolyLineColor(
                paceMin.toInt() / 60,
                paceMax.toInt() / 60,
                colorMin,
                colorMax,
                tempo
            )
            if (polylineOptionsList == null) {
                polylineOptionsList = mutableListOf(
                    PolylineOptions()
                        .color(newColor)
                        .add(LatLng(location.latitude, location.longitude))
                )
            } else {
                polylineOptionsList!!.add(
                    PolylineOptions()
                        .color(newColor)
                        .add(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
                        .add(LatLng(location.latitude, location.longitude))
                )
            }
        }
        // save the location for calculations
        currentLocation = location

        updateDbGpsLocation(location, C.REST_LOCATIONID_LOC)
        saveRestLocation(location, C.REST_LOCATIONID_LOC)
        showNotification()

        // Utils.addToMapPolylineOptions(location.latitude, location.longitude)
        // broadcast new location to UI
        val intent = Intent(C.LOCATION_UPDATE_ACTION)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, location.latitude)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, location.longitude)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLDIRECT, distanceOverallDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, distanceOverallTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALLTEMPO, tempoOverall)
        intent.putExtra(
            C.LOCATION_UPDATE_ACTION_OVERALLTIME,
            Utils.longToDateString(durationOverall)
        )

        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPDIRECT, distanceCPDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPTOTAL, distanceCPTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPTEMPO, tempoCP)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CPTIME, durationCP)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPDIRECT, distanceWPDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPTOTAL, distanceWPTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPTEMPO, tempoWP)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WPTIME, durationWP)

        // Session information
        intent.putExtra(C.CURRENT_SESSION_ID, currentDbSessionId)
        intent.putExtra(C.CURRENT_SESSION_REST_ID, currentRestSessionId)
        if (polylineOptionsList != null) {
            intent.putExtra(
                C.LOCATION_UPDATE_POLYLINE_OPTIONS,
                polylineOptionsList!! as Serializable
            )
        }
        if (listOfCPMarkerLatLngs != null) {
            intent.putExtra(
                C.LOCATION_UPDATE_CP_LATLNGS,
                listOfCPMarkerLatLngs!! as Serializable
            )
        }
        if (lastWPMarkerLatLng != null) {
            intent.putExtra(
                C.LOCATION_UPDATE_WP_LATLNGS,
                lastWPMarkerLatLng!! as Parcelable
            )
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }

    private fun updateCurrentSession() {
        currentDbSession = gpsSessionRepository.getSessionById(currentDbSessionId.toInt())
        paceMax = currentDbSession.paceMax
        paceMin = currentDbSession.paceMin
        colorMax = currentDbSession.colorMax
        colorMin = currentDbSession.colorMin
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
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.w(TAG, "task successful")
                        if (task.result != null) {
                            onNewLocation(task.result!!)
                        }
                    } else {

                        Log.w(TAG, "Failed to get location." + task.exception)
                    }
                }
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


        // don't forget to unregister broadcast receiver!!!!
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

        startTimeOverall = Utils.getCurrentDateTime()
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


        val intentCp = Intent(C.NOTIFICATION_ACTION_CP)
        val intentWp = Intent(C.NOTIFICATION_ACTION_WP)

        paceMin = intent!!.getDoubleExtra(C.PACE_MIN, 120.0)
        paceMax = intent.getDoubleExtra(C.PACE_MAX, 480.0)
        colorMin = intent.getStringExtra(C.COLOR_MIN)!!
        colorMax = intent.getStringExtra(C.COLOR_MAX)!!
        jwt = intent.getStringExtra(C.CURRENT_USER_JWT)
        currentDbUserId = intent.getIntExtra(C.CURRENT_USER_DB_ID, 0)



        startDbTrackingSession(
            dateFormat.format(Date(startTimeOverall)), paceMin,
            paceMax, colorMin,
            colorMax
        )
        showNotification()

        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        TODO("Return the communication channel to the service.")
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)

    }

    fun showNotification() {

        val intentCp = Intent(C.NOTIFICATION_ACTION_CP)
        val intentWp = Intent(C.NOTIFICATION_ACTION_WP)


        val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, 0)
        val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, 0)

        val notificationsView = RemoteViews(packageName, R.layout.track_control)
        val durationStartString = Utils.longToDateString(durationOverall)

        notificationsView.setOnClickPendingIntent(R.id.imageButtonCP, pendingIntentCp)
        notificationsView.setOnClickPendingIntent(R.id.imageButtonWP, pendingIntentWp)

        notificationsView.setTextViewText(
            R.id.textViewOverallDirect,
            "%.2f".format(distanceOverallDirect)
        )
        notificationsView.setTextViewText(R.id.textViewOverallTotal, durationStartString)
        notificationsView.setTextViewText(R.id.textViewOverallTempo, tempoOverall)

        notificationsView.setTextViewText(R.id.textViewCPDirect, "%.2f".format(distanceCPDirect))
        notificationsView.setTextViewText(R.id.textViewCPTotal, "%.2f".format(distanceCPTotal))
        notificationsView.setTextViewText(R.id.textViewCPTempo, tempoCP)

        notificationsView.setTextViewText(R.id.textViewWPDirect, "%.2f".format(distanceWPDirect))
        notificationsView.setTextViewText(R.id.textViewWPTotal, "%.2f".format(distanceWPTotal))
        notificationsView.setTextViewText(R.id.textViewWPTempo, tempoWP)

        // construct and show notification
        val builder = NotificationCompat.Builder(applicationContext, C.NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.baseline_directions_walk_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.setContent(notificationsView)

        // Super important, start as foreground service - ie android considers this as an active app. Need visual reminder - notification.
        // must be called within 5 secs after service starts.
        startForeground(C.NOTIFICATION_ID, builder.build())

    }


    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //  Log.d(TAG, intent!!.action)
            when (intent!!.action) {
                C.NOTIFICATION_ACTION_WP -> {
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f
                    durationWP = 0
                    lastWPMarkerLatLng = LatLng(
                        locationWP!!.latitude,
                        locationWP!!.longitude
                    )

                    saveRestLocation(locationWP!!, C.REST_LOCATIONID_WP)
                    showNotification()
                }
                C.NOTIFICATION_ACTION_CP -> {
                    locationCP = currentLocation
                    distanceCPDirect = 0f
                    distanceCPTotal = 0f
                    durationCP = 0
                    updateDbGpsLocation(locationCP!!, C.REST_LOCATIONID_CP)
                    //reset WP also, since we know exactly where we are on the map
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f
                    durationWP = 0
                    if (listOfCPMarkerLatLngs == null) {
                        listOfCPMarkerLatLngs = mutableListOf(
                            LatLng(
                                locationWP!!.latitude,
                                locationWP!!.longitude
                            )
                        )
                    } else {
                        listOfCPMarkerLatLngs!!.add(
                            LatLng(
                                locationCP!!.latitude,
                                locationCP!!.longitude
                            )
                        )
                    }
                    updateDbGpsLocation(locationWP!!, C.REST_LOCATIONID_WP)

                    saveRestLocation(locationCP!!, C.REST_LOCATIONID_CP)
                    showNotification()
                }
            }
        }

    }
    // ============================================== DATABASE CONTROLLER =============================================

    private fun startDbTrackingSession(
        recordedAt: String, paceMin: Double,
        paceMax: Double, colorMin: String,
        colorMax: String
    ) {

        currentDbSession = GpsSession(
            recordedAt, recordedAt, paceMin,
            paceMax, colorMin, colorMax, recordedAt, "--:--:--",
            "--:--", 0.0, 0.0, 0.0, currentDbUserId
        )
        currentDbSessionId = gpsSessionRepository.add(currentDbSession)
        startRestTrackingSession(currentDbSession)

    }

    private fun updateDbGpsLocation(location: Location, locationType: String) {
        val gpsLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            GpsLocation(
                dateFormat.format(Date(location.time)),
                location.latitude, location.longitude,
                location.accuracy.toDouble(), location.altitude,
                location.verticalAccuracyMeters.toDouble(), currentDbSessionId,
                locationType, currentDbUserId
            )
        } else {
            GpsLocation(
                dateFormat.format(Date(location.time)),
                location.latitude, location.longitude,
                location.accuracy.toDouble(), location.altitude,
                0.0, currentDbSessionId,
                locationType, currentDbUserId
            )
        }
        gpsLocationRepository.add(gpsLocation)

    }
}