package ee.taltech.kamatt.sportsmap

// do not import this! never! If this get inserted automatically when pasting java code, remove it
//import android.R
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_MAGNETIC_FIELD
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_GAME
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.BuildConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import ee.taltech.kamatt.sportsmap.db.model.AppUser
import ee.taltech.kamatt.sportsmap.db.model.GpsLocation
import ee.taltech.kamatt.sportsmap.db.model.GpsSession
import ee.taltech.kamatt.sportsmap.db.repository.AppUserRepository
import ee.taltech.kamatt.sportsmap.db.repository.GpsLocationRepository
import ee.taltech.kamatt.sportsmap.db.repository.GpsSessionRepository
import ee.taltech.kamatt.sportsmap.db.repository.LocationTypeRepository
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.buttons_top.*
import kotlinx.android.synthetic.main.edit_session.*
import kotlinx.android.synthetic.main.options.*
import kotlinx.android.synthetic.main.track_control.*
import java.lang.Math.toDegrees


//  TODO: bug. should load new sessions to "old sessions" right after stopping session
//  TODO: bug. when session ends then should leave old values

//  TODO: LOW. current user for session is not dynamical
//  TODO: LOW. pace should be double and in seconds everywhere but UI
//  TODO: LOW. in old sessions - changes should also be updated in rest (pacemin, pacemax, colormin, colormax, name, description)
//  TODO: LOW. time updates in real time not with location update
//  TODO: LOW. save climb and other less important values as well in session when session ends


class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private lateinit var locationTypeRepository: LocationTypeRepository
        private lateinit var gpsSessionRepository: GpsSessionRepository
        private lateinit var gpsLocationRepository: GpsLocationRepository
        private lateinit var appUserRepository: AppUserRepository

        fun deleteSessionFromDb(gpsSession: GpsSession) {
            gpsLocationRepository.removeLocationsById(gpsSession.id)
            gpsSessionRepository.removeSessionById(gpsSession.id)
        }

        private lateinit var currentlyEditedSession: GpsSession

    }

    //  Compass
    private lateinit var sensorManager: SensorManager
    private lateinit var compassImage: ImageView
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private var currentDegree = 0.0f
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null

    //  private var markerCP: Marker? = null
    //  private var markerWP: Marker? = null
    private var mapPolyline: Polyline? = null
    private var locationServiceActive = false
    private var isCompassVisible = false
    private var isOptionsVisible = false
    private var isOldSessionsVisible = false

    private var paceMin: Double = 120.0
    private var paceMax: Double = 480.0
    private var colorMin: String = "blue"
    private var colorMax: String = "red"

    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0
    private var lastTimestamp = Utils.getCurrentDateTime()

    private var polylineOptionsList: MutableList<PolylineOptions>? = null
    private var currentDbSessionId: Int = -1

    private var distanceOverallDirect = 0f
    private var distanceOverallTotal = 0f
    private var tempoOverall: String = "0"
    private var durationOverall: String = "0"

    private var startPointMarker: MarkerOptions? = null
    private var listOfCPMarkerLatLngs: MutableList<LatLng>? = null
    private var lastWPMarkerLatLng: LatLng? = null
    private var markerList: MutableList<Marker>? = null

    private var loadedSession: GpsSession? = null
    private var loadedLocations: List<GpsLocation>? = null
    private var isOldSessionLoaded = false

    // ============================================== MAIN ENTRY - ONCREATE =============================================
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        // If we have a saved state then we can restore it now
        locationTypeRepository = LocationTypeRepository(this).open()
        val locationTypes = locationTypeRepository.getAll()
        for (locationType in locationTypes) {
            Log.d("locationType", locationType.toString())
        }
        gpsSessionRepository = GpsSessionRepository(this).open()

        val gpsSessions = gpsSessionRepository.getAll()
        for (gpsSession in gpsSessions) {
            Log.d("gpsSession", gpsSession.toString())
        }
        gpsLocationRepository = GpsLocationRepository(this).open()
        val gpsLocations = gpsLocationRepository.getAll()
        for (gpsLocation in gpsLocations) {
            Log.d("gpsLocation", gpsLocation.toString())
        }

        appUserRepository = AppUserRepository(this).open()
        appUserRepository.add(AppUser(C.REST_USERNAME, C.REST_PASSWORD))
        val appUsers = appUserRepository.getAll()
        for (appUser in appUsers) {
            Log.d("appUser", appUser.toString())
        }

        setContentView(R.layout.activity_main)
        imageButtonCP.setOnClickListener { handleCpOnClick() }
        imageButtonWP.setOnClickListener { handleWpOnClick() }
        buttonStartStop.setOnClickListener { handleStartStopOnClick() }
        imageViewToggleCompass.setOnClickListener { handleToggleCompass() }
        imageViewOptions.setOnClickListener { handleOptionsOnClick() }
        buttonGoToOldSessions.setOnClickListener { handleOpenOldSessionsOnClick() }
        buttonCloseRecyclerView.setOnClickListener { handleCloseOldSessionsOnClick() }
        buttonUpdateSession.setOnClickListener { handleUpdateSessionOnClick() }
        buttonUpdatePolylineParams.setOnClickListener { handleUpdatePolylineParamsOnClick() }
        // safe to call every time
        createNotificationChannel()

        if (!checkPermissions()) {
            requestPermissions()
        }

        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)


        compassImage = findViewById(R.id.imageViewNorthDir)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)
        if (savedInstanceState != null) {
            locationServiceActive = savedInstanceState.getBoolean("locationServiceActive", false)
            isCompassVisible = savedInstanceState.getBoolean("isCompassVisible", false)
            isOptionsVisible = savedInstanceState.getBoolean("isOptionsVisible", false)
            isOldSessionsVisible = savedInstanceState.getBoolean("isOldSessionsVisible", false)
            if (savedInstanceState.getBoolean("isOldSessionLoaded", false)) {
                val loadedSessionId = savedInstanceState.getInt("loadedSessionId", 0)
                loadSession(loadedSessionId)
            }


            restoreCompassState()
            restoreOptionsState()
            restoreOldSessionsState()
        }
        if (locationServiceActive) {
            buttonStartStop.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.baseline_stop_24
                )
            )
            startPointMarker = null
        } else {

            buttonStartStop.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.baseline_play_arrow_24
                )
            )

            currentDbSessionId = -1
        }

        recyclerViewSessions.layoutManager = LinearLayoutManager(this)
        recyclerViewSessions.adapter =
            DataRecyclerViewAdapterSessions(this, gpsSessionRepository, 1)

    }


    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("locationServiceActive", locationServiceActive)
        outState.putBoolean("isCompassVisible", isCompassVisible)
        outState.putBoolean("isOptionsVisible", isOptionsVisible)
        outState.putBoolean("isOldSessionsVisible", isOldSessionsVisible)
        outState.putBoolean("isOldSessionLoaded", isOldSessionLoaded)
        if (isOldSessionLoaded) {
            outState.putInt("loadedSessionId", loadedSession!!.id)
        }

    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        reDrawPolyline()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        locationTypeRepository.close()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
    }

    // ============================================== NOTIFICATION CHANNEL CREATION =============================================
    private fun createNotificationChannel() {
        // when on 8 Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                C.NOTIFICATION_CHANNEL,
                "Default channel",
                NotificationManager.IMPORTANCE_LOW
            )

            //.setShowBadge(false).setSound(null, null);

            channel.description = "Default channel"

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    // ============================================== PERMISSION HANDLING =============================================
    // Returns the current state of the permissions needed.
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(
                TAG,
                "Displaying permission rationale to provide additional context."
            )
            Snackbar.make(
                findViewById(R.id.activity_main),
                "Hey, i really need to access GPS!",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("OK") {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        C.REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                C.REQUEST_PERMISSIONS_REQUEST_CODE
            )

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode === C.REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.count() <= 0 -> { // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                    Toast.makeText(this, "User interaction was cancelled.", Toast.LENGTH_SHORT)
                        .show()
                }
                grantResults[0] === PackageManager.PERMISSION_GRANTED -> {// Permission was granted.
                    Log.i(TAG, "Permission was granted")
                    Toast.makeText(this, "Permission was granted", Toast.LENGTH_SHORT).show()
                }
                else -> { // Permission denied.
                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        "You denied GPS! What can I do?",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("Settings") {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri: Uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }

    }

    // ============================================== CLICK HANDLERS =============================================

    private fun handleWpOnClick() {
        Log.d(TAG, "buttonWPOnClick")
        sendBroadcast(Intent(C.NOTIFICATION_ACTION_WP))
    }

    private fun handleCpOnClick() {
        Log.d(TAG, "buttonCPOnClick")
        sendBroadcast(Intent(C.NOTIFICATION_ACTION_CP))
    }

    private fun handleStartStopOnClick() {
        Log.d(TAG, "buttonStartStop. locationServiceActive: $locationServiceActive")
        isOldSessionLoaded = false
        if (locationServiceActive) {

            // stopping the service
            val currentlyActiveSession: GpsSession =
                gpsSessionRepository.getSessionById(currentDbSessionId)
            currentlyActiveSession.distance = distanceOverallTotal.toDouble()
            currentlyActiveSession.speed = tempoOverall
            currentlyActiveSession.duration = durationOverall
            currentlyActiveSession.colorMin = colorMin
            currentlyActiveSession.colorMax = colorMax
            currentlyActiveSession.paceMin = paceMin
            currentlyActiveSession.paceMax = paceMax
            gpsSessionRepository.updateSession(currentlyActiveSession)

            // stopping the service
            stopService(Intent(this, LocationService::class.java))
            buttonStartStop.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.baseline_play_arrow_24
                )
            )


        } else {

            // clear the track on map
            Utils.clearMapPolylineOptions()

            if (Build.VERSION.SDK_INT >= 26) {
                // starting the FOREGROUND service
                // service has to display non-dismissable notification within 5 secs
                val intent: Intent = Intent(this, LocationService::class.java)
                intent.putExtra(C.PACE_MAX, paceMax)
                intent.putExtra(C.PACE_MIN, paceMin)
                intent.putExtra(C.COLOR_MAX, colorMax)
                intent.putExtra(C.COLOR_MIN, colorMin)
                startForegroundService(intent)
            } else {
                val intent: Intent = Intent(this, LocationService::class.java)
                intent.putExtra(C.PACE_MAX, paceMax)
                intent.putExtra(C.PACE_MIN, paceMin)
                intent.putExtra(C.COLOR_MAX, colorMax)
                intent.putExtra(C.COLOR_MIN, colorMin)
                startService(Intent(this, LocationService::class.java))
            }
            buttonStartStop.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.baseline_stop_24
                )
            )
        }

        locationServiceActive = !locationServiceActive
    }

    private fun handleToggleCompass() {
        isCompassVisible = if (isCompassVisible) {
            compassImage.setBackgroundResource(0)
            false

        } else {
            compassImage.setBackgroundResource(R.drawable.baseline_arrow_upward_black_24)
            true
        }
    }

    private fun handleOptionsOnClick() {

        if (isOptionsVisible) {
            includeOptions.visibility = View.INVISIBLE

            isOptionsVisible = false

        } else {
            if (currentDbSessionId != -1) {
                val currentSession = gpsSessionRepository.getSessionById(currentDbSessionId)
                Log.d("currentDbSessionId", currentSession.id.toString())
                Log.d("currentDbSess pacemin", (currentSession.paceMin).toInt().toString())
                Log.d("currentDbSess colmin", currentSession.colorMin)
                editTextMinSpeed.setText((currentSession.paceMin / 60).toInt().toString())
                editTextMaxSpeed.setText((currentSession.paceMax / 60).toInt().toString())
                editTextMinColor.setText(currentSession.colorMin)
                editTextMaxColor.setText(currentSession.colorMax)
            }

            includeOptions.visibility = View.VISIBLE
            isOptionsVisible = true
        }
    }

    private fun handleOpenOldSessionsOnClick() {

        includeOptions.visibility = View.INVISIBLE
        recyclerViewSessions.visibility = View.VISIBLE
        buttonCloseRecyclerView.visibility = View.VISIBLE
        isOptionsVisible = false
        isOldSessionsVisible = true

    }

    private fun handleCloseOldSessionsOnClick() {
        recyclerViewSessions.visibility = View.INVISIBLE
        buttonCloseRecyclerView.visibility = View.INVISIBLE
        isOldSessionsVisible = false
    }

    private fun handleUpdateSessionOnClick() {

        // update db and adapter
        currentlyEditedSession.name = editTextSessionName.text.toString()
        currentlyEditedSession.description = editTextSessionDescription.text.toString()
        val paceMin: Double = editTextSessionMinSpeed.text.toString().toDouble() * 60
        val paceMax: Double = editTextSessionMaxSpeed.text.toString().toDouble() * 60
        if (paceMin < paceMax && paceMin >= 0) {
            currentlyEditedSession.paceMin = paceMin
            currentlyEditedSession.paceMax = paceMax
        }
        currentlyEditedSession.colorMin = editTextSessionMinColor.text.toString()
        currentlyEditedSession.colorMax = editTextSessionMaxColor.text.toString()
        // update db and adapter
        gpsSessionRepository.updateSession(currentlyEditedSession)
        recyclerViewSessions.adapter!!.notifyDataSetChanged()
        // close session window
        includeEditSession.visibility = View.INVISIBLE
        handleOpenOldSessionsOnClick()


    }

    private fun handleUpdatePolylineParamsOnClick() {
        if (locationServiceActive) {
            val currentSession = gpsSessionRepository.getSessionById(currentDbSessionId)
            // update db and adapter
            paceMin = editTextMinSpeed.text.toString().toDouble() * 60.0
            paceMax = editTextMaxSpeed.text.toString().toDouble() * 60.0
            if (paceMin < paceMax && paceMin >= 0) {

                currentSession.paceMin = paceMin
                currentSession.paceMax = paceMax
            }
            colorMin = editTextMinColor.text.toString()
            colorMax = editTextMaxColor.text.toString()

            currentSession.colorMin = colorMin
            currentSession.colorMax = colorMax
            // update db and adapter

            gpsSessionRepository.updateSession(currentSession)
            recyclerViewSessions.adapter!!.notifyDataSetChanged()
            // close session window

            includeOptions.visibility = View.INVISIBLE
            isOptionsVisible = false
        }
    }

    // ============================================== BROADCAST RECEIVER =============================================
    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Log.d(TAG, intent!!.action)
            when (intent!!.action) {
                C.LOCATION_UPDATE_ACTION -> {
                    distanceOverallDirect =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALLDIRECT, 0.0F)
                    distanceOverallTotal =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, 0.0F)


                    if (!intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTEMPO)
                            .isNullOrEmpty()
                    ) {
                        tempoOverall =
                            intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTEMPO)!!
                        textViewOverallTempo.text =
                            intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTEMPO)
                    }
                    if (!intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTIME).isNullOrEmpty()
                    ) {
                        durationOverall =
                            intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTIME)!!
                    }

                    textViewOverallDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, 0.0F)
                            .toInt().toString()

                    textViewOverallTotal.text =
                        intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTIME)





                    textViewCPTotal.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CPTOTAL, 0.0F).toInt()
                            .toString()
                    textViewCPDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CPDIRECT, 0.0F).toInt()
                            .toString()
                    textViewCPTempo.text =
                        intent.getStringExtra(C.LOCATION_UPDATE_ACTION_CPTEMPO)

                    textViewWPTotal.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WPTOTAL, 0.0F).toInt()
                            .toString()
                    textViewWPDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WPDIRECT, 0.0F).toInt()
                            .toString()
                    textViewWPTempo.text =
                        intent.getStringExtra(C.LOCATION_UPDATE_ACTION_WPTEMPO)
                    polylineOptionsList =
                        intent.getSerializableExtra(C.LOCATION_UPDATE_POLYLINE_OPTIONS) as?
                                MutableList<PolylineOptions>

                    listOfCPMarkerLatLngs = intent
                        .getSerializableExtra(C.LOCATION_UPDATE_CP_LATLNGS) as? MutableList<LatLng>

                    lastWPMarkerLatLng = intent.getParcelableExtra(C.LOCATION_UPDATE_WP_LATLNGS)


                    updateMap(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0)
                    )
                    lastTimestamp = Utils.getCurrentDateTime()
                    currentDbSessionId = intent.getLongExtra(C.CURRENT_SESSION_ID, -1).toInt()


                }
                C.LOCATION_UPDATE_STOP -> {

                }
            }
        }
    }


    // ============================================== HANDLE MAP =============================================
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.isMyLocationEnabled = true
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(59.3927437, 24.6642), 17.0f))
        reDrawPolyline()
    }

    private fun updateMap(lat: Double, lng: Double) {
        if (marker != null) {
            marker!!.remove()
        }

        if (mapPolyline != null) {
            mapPolyline!!.remove()
        }
        lastLatitude = lat
        lastLongitude = lng

        reDrawPolyline()


    }

    private fun reDrawPolyline() {

        if (isOldSessionLoaded) {
            if (this::mMap.isInitialized) {
                mMap!!.clear()
                val loadedMinPace = loadedSession!!.paceMin
                val loadedMaxPace = loadedSession!!.paceMax
                val loadedMinColor = loadedSession!!.colorMin
                val loadedMaxColor = loadedSession!!.colorMax

                var oldLocation: Location? = null
                for (loadedLocation in loadedLocations!!) {
                    if (loadedLocation.gpsLocationTypeId == C.REST_LOCATIONID_LOC) {

                        if (startPointMarker == null) {
                            startPointMarker = MarkerOptions().position(
                                LatLng(
                                    loadedLocation.latitude,
                                    loadedLocation.longitude
                                )
                            )
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_play_arrow_black_24))
                            mMap.addMarker(startPointMarker)


                            oldLocation = Location(LocationManager.GPS_PROVIDER).apply {
                                latitude = loadedLocation.latitude
                                longitude = loadedLocation.longitude
                                time =
                                    LocationService.dateFormat.parse(loadedLocation.recordedAt)!!.time
                            }

                        } else {
                            val newLocation = Location(LocationManager.GPS_PROVIDER).apply {
                                latitude = loadedLocation.latitude
                                longitude = loadedLocation.longitude
                                time =
                                    LocationService.dateFormat.parse(loadedLocation.recordedAt)!!.time
                            }
                            val distanceFromLastPoint: Float = oldLocation!!.distanceTo(newLocation)
                            val newTimeDifference = newLocation.time - oldLocation.time
                            val tempo: Float =
                                Utils.getPaceMinutesFloat(newTimeDifference, distanceFromLastPoint)
                            //val testTempo: Float = Utils.getPaceMinutesFloat(newTimeDifference, distanceFromLastPoint)
                            Log.d(TAG, "current tempo $tempo")
                            val newColor = Utils.calculateMapPolyLineColor(
                                loadedMinPace.toInt(),
                                loadedMaxPace.toInt(),
                                loadedMinColor,
                                loadedMaxColor,
                                tempo
                            )


                            mMap.addPolyline(
                                PolylineOptions()
                                    .color(newColor)
                                    .add(LatLng(oldLocation!!.latitude, oldLocation!!.longitude))
                                    .add(LatLng(newLocation.latitude, newLocation.longitude))
                            )
                            oldLocation = newLocation
                        }


                    } else if (loadedLocation.gpsLocationTypeId == C.REST_LOCATIONID_CP) {
                        val cpMarkerOptions = MarkerOptions()
                            .position(LatLng(loadedLocation.latitude, loadedLocation.longitude))
                            .icon(
                                bitmapDescriptorFromVector(
                                    this,
                                    R.drawable.baseline_add_location_24
                                )
                            )
                        mMap.addMarker(cpMarkerOptions)
                    }
                }

            }

        } else {
            if (!polylineOptionsList.isNullOrEmpty()) {

                mMap.clear()
                if (startPointMarker == null) {
                    startPointMarker =
                        MarkerOptions().position(LatLng(lastLatitude, lastLongitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_play_arrow_black_24))

                    mMap.addMarker(startPointMarker)
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastLatitude,
                                lastLongitude
                            ), 17.0f
                        )
                    )
                } else {
                    mMap.addMarker(startPointMarker)
                }
                if (listOfCPMarkerLatLngs != null) {

                    for (cpLatLng in listOfCPMarkerLatLngs!!) {
                        val cpMarkerOptions = MarkerOptions()
                            .position(LatLng(cpLatLng.latitude, cpLatLng.longitude))
                            .icon(
                                bitmapDescriptorFromVector(
                                    this,
                                    R.drawable.baseline_add_location_24
                                )
                            )
                        val cpMarker = mMap.addMarker(cpMarkerOptions)
                        if (markerList == null) {
                            markerList = mutableListOf(cpMarker)
                        } else {
                            markerList!!.add(cpMarker)
                        }

                    }
                }
                if (lastWPMarkerLatLng != null) {
                    val wpMarker = MarkerOptions()
                        .position(lastWPMarkerLatLng!!)
                        .icon(
                            bitmapDescriptorFromVector(
                                this,
                                R.drawable.baseline_place_24
                            )
                        )
                    mMap.addMarker(wpMarker)

                }

                for (polylineOptions in polylineOptionsList!!) {
                    mapPolyline = mMap.addPolyline(polylineOptions)
                }
            }
        }
    }

    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {

        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
    // ============================================== COMPASS =============================================

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor === accelerometer) {
            lowPass(event.values, lastAccelerometer)
            lastAccelerometerSet = true
        } else if (event.sensor === magnetometer) {
            lowPass(event.values, lastMagnetometer)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val degree = (toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360

                val rotateAnimation = RotateAnimation(
                    currentDegree,
                    -degree,
                    RELATIVE_TO_SELF, 0.5f,
                    RELATIVE_TO_SELF, 0.5f
                )
                rotateAnimation.duration = 1000
                rotateAnimation.fillAfter = true

                compassImage.startAnimation(rotateAnimation)
                currentDegree = -degree
            }
        }


    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    private fun restoreCompassState() {
        if (!isCompassVisible) {
            compassImage.setBackgroundResource(0)
        } else {
            compassImage.setBackgroundResource(R.drawable.baseline_arrow_upward_black_24)
        }
    }

    // ============================================== OPTIONS =============================================
    private fun restoreOptionsState() {
        if (isOptionsVisible) {
            includeOptions.visibility = View.VISIBLE
        } else {
            includeOptions.visibility = View.INVISIBLE
        }
    }

    private fun restoreOldSessionsState() {
        if (isOldSessionsVisible) {
            recyclerViewSessions.visibility = View.VISIBLE
            buttonCloseRecyclerView.visibility = View.VISIBLE
        } else {
            recyclerViewSessions.visibility = View.INVISIBLE
            buttonCloseRecyclerView.visibility = View.INVISIBLE
        }
    }


    // ============================================== DATABASE CONTROLLER =============================================
    fun startEditingSession(gpsSession: GpsSession) {
        currentlyEditedSession = gpsSession
        includeEditSession.visibility = View.VISIBLE
        handleCloseOldSessionsOnClick()
        editTextSessionName.setText(gpsSession.name)
        editTextSessionDescription.setText(gpsSession.description)
        editTextSessionMinSpeed.setText((gpsSession.paceMin / 60).toInt().toString())
        editTextSessionMaxSpeed.setText((gpsSession.paceMax / 60).toInt().toString())
        editTextSessionMinColor.setText(gpsSession.colorMin)
        editTextSessionMaxColor.setText(gpsSession.colorMax)


    }

    // ============================================== LOAD SESSION =============================================
    fun loadSession(sessionId: Int) {
        isOldSessionLoaded = true


        loadedSession = gpsSessionRepository.getSessionById(sessionId)
        loadedLocations = gpsLocationRepository.getLocationsBySessionId(sessionId)
        reDrawPolyline()
        setSessionStatisticsIfLoaded()


    }

    private fun setSessionStatisticsIfLoaded() {
        if (isOldSessionLoaded) {
            textViewOverallDirect.text = loadedSession!!.distance.toInt().toString()
            textViewOverallTotal.text = loadedSession!!.duration
            textViewOverallTempo.text = loadedSession!!.speed
        }
    }


}
