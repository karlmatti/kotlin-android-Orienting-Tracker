package ee.taltech.kamatt.sportsmap

// do not import this! never! If this get inserted automatically when pasting java code, remove it
//import android.R
import android.Manifest
import android.annotation.SuppressLint
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.BuildConfig
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
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
import kotlinx.android.synthetic.main.export_session.*
import kotlinx.android.synthetic.main.login.*
import kotlinx.android.synthetic.main.options.*
import kotlinx.android.synthetic.main.register.*
import kotlinx.android.synthetic.main.stop_confirmation.*
import kotlinx.android.synthetic.main.track_control.*
import kotlinx.android.synthetic.main.welcome_screen.*
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.lang.Math.toDegrees
import java.util.*
import java.util.regex.Pattern



class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener,
    SeekBar.OnSeekBarChangeListener {
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

    // The desired intervals for location updates. Inexact. Updates may be more or less frequent.
    private var UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000

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
    private var isWelcomeVisible = true
    private var isLoginVisible = false
    private var isRegisterVisible = false
    private var isStopConfirmationVisible = false


    private var paceMin: Double = 120.0
    private var paceMax: Double = 480.0
    private var colorMin: String = "blue"
    private var colorMax: String = "red"

    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0
    private var lastTimestamp = Utils.getCurrentDateTime()

    private var polylineOptionsList: MutableList<PolylineOptions>? = null
    private var currentDbSessionId: Int = -1
    private var currentRestSessionId: String? = null


    private var distanceOverallDirect = 0f
    private var distanceOverallTotal = 0f
    private var paceOverall: String = "0"
    private var durationOverall: Long = 0L

    private var startPointMarker: MarkerOptions? = null
    private var listOfCPMarkerLatLngs: MutableList<LatLng>? = null
    private var lastWPMarkerLatLng: LatLng? = null
    private var markerList: MutableList<Marker>? = null

    private var loadedSession: GpsSession? = null
    private var loadedLocations: List<GpsLocation>? = null
    private var isOldSessionLoaded = false

    private var jwt: String? = null
    private var currentDbUserId: Int = -1

    private lateinit var recyclerViewAdapter: DataRecyclerViewAdapterSessions
    private var isMapCentered = false
    private var isKeepUserChosenUp = true
    private var isKeepNorthUp = false
    private var isKeepDirectionUp = false


    // ============================================== MAIN ENTRY - ONCREATE =============================================
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        // If we have a saved state then we can restore it now
        locationTypeRepository = LocationTypeRepository(this).open()
        /*val locationTypes = locationTypeRepository.getAll()
        for (locationType in locationTypes) {
            Log.d("locationType", locationType.toString())
        }*/
        gpsSessionRepository = GpsSessionRepository(this).open()

        val gpsSessions = gpsSessionRepository.getAll()
        for (gpsSession in gpsSessions) {
            Log.d("gpsSession", gpsSession.toString())
        }
        gpsLocationRepository = GpsLocationRepository(this).open()
        /*val gpsLocations = gpsLocationRepository.getAll()
        for (gpsLocation in gpsLocations) {
            Log.d("gpsLocation", gpsLocation.toString())
        }*/

        appUserRepository = AppUserRepository(this).open()
        /* appUserRepository.add(AppUser(C.REST_USERNAME, C.REST_PASSWORD))
         val appUsers = appUserRepository.getAll()
         for (appUser in appUsers) {
             Log.d("appUser", appUser.toString())
         }*/

        setContentView(R.layout.activity_main)
        imageButtonCP.setOnClickListener { handleCpOnClick() }
        imageButtonWP.setOnClickListener { handleWpOnClick() }
        buttonStartStop.setOnClickListener { handleStartStopOnClick() }
        imageViewToggleCompass.setOnClickListener { handleToggleCompass() }
        imageViewOptions.setOnClickListener { handleOptionsOnClick() }
        buttonGoToOldSessions.setOnClickListener { handleOpenOldSessionsOnClick() }
        buttonCloseRecyclerView.setOnClickListener { handleCloseOldSessionsOnClick() }
        buttonUpdateSession.setOnClickListener { handleUpdateSessionOnClick() }
        buttonUpdatePolylineParams.setOnClickListener { handleUpdateActiveSessionOnClick() }
        buttonLogin.setOnClickListener { handleLoginOnClick() }
        buttonShowWelcome1.setOnClickListener { handleShowWelcomeOnClick() }
        buttonShowWelcome2.setOnClickListener { handleShowWelcomeOnClick() }
        buttonShowLogin.setOnClickListener { handleShowLoginOnClick() }
        buttonShowRegister.setOnClickListener { handleShowRegisterOnClick() }
        buttonRegister.setOnClickListener { handleRegisterOnClick() }
        buttonLogOut.setOnClickListener { handleLogOutOnClick() }
        buttonConfirmationCancel.setOnClickListener { handleConfirmationCancelOnClick() }
        buttonConfirmationOk.setOnClickListener { handleConfirmationOkOnClick() }
        imageViewKeepCentered.setOnClickListener { handleKeepCenteredOnClick() }
        imageViewSwitchDirection.setOnClickListener { handleSwitchDirectionOnClick() }
        imageViewReset.setOnClickListener { handleResetOnClick() }


        seekBarGpsFreq.setOnSeekBarChangeListener(this)
        seekBarSyncFreq.setOnSeekBarChangeListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBarGpsFreq.min = 1
            seekBarSyncFreq.min = 1
        }
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
            isWelcomeVisible = savedInstanceState.getBoolean("isWelcomeVisible", true)
            isLoginVisible = savedInstanceState.getBoolean("isLoginVisible", true)
            isRegisterVisible = savedInstanceState.getBoolean("isRegisterVisible", true)
            isKeepDirectionUp = savedInstanceState.getBoolean("isKeepDirectionUp", false)
            isKeepNorthUp = savedInstanceState.getBoolean("isKeepNorthUp", false)
            isKeepUserChosenUp = savedInstanceState.getBoolean("isKeepUserChosenUp", true)
            isStopConfirmationVisible =
                savedInstanceState.getBoolean("isStopConfirmationVisible", false)
            isMapCentered = savedInstanceState.getBoolean("isMapCentered", false)
            durationOverall = savedInstanceState.getLong(C.LOCATION_UPDATE_ACTION_OVERALLTIME, 0L)
            distanceOverallTotal =
                savedInstanceState.getFloat(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, 0F)
            paceOverall =
                savedInstanceState.getString(C.LOCATION_UPDATE_ACTION_OVERALLPACE).toString()


            restoreOverallTextViews()
            if (savedInstanceState.getBoolean("isOldSessionLoaded", false)) {
                val loadedSessionId = savedInstanceState.getInt("loadedSessionId", 0)
                loadSession(loadedSessionId)
            }
            if (isMapCentered) {
                isMapCentered = false
                handleKeepCenteredOnClick()
            }
            restoreCompassState()
            restoreOptionsState()
            restoreOldSessionsState()
            restoreLoginState()
            restoreStopConfirmationState()
            drawSwitchDirection()
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
        recyclerViewAdapter = DataRecyclerViewAdapterSessions(this, gpsSessionRepository, 1)
        recyclerViewSessions.adapter = recyclerViewAdapter


    }


    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("locationServiceActive", locationServiceActive)
        outState.putBoolean("isCompassVisible", isCompassVisible)
        outState.putBoolean("isOptionsVisible", isOptionsVisible)
        outState.putBoolean("isOldSessionsVisible", isOldSessionsVisible)
        outState.putBoolean("isOldSessionLoaded", isOldSessionLoaded)
        outState.putBoolean("isWelcomeVisible", isWelcomeVisible)
        outState.putBoolean("isLoginVisible", isLoginVisible)
        outState.putBoolean("isRegisterVisible", isRegisterVisible)
        outState.putBoolean("isStopConfirmationVisible", isStopConfirmationVisible)
        outState.putBoolean("isMapCentered", isMapCentered)
        outState.putBoolean("isKeepUserChosenUp", isKeepUserChosenUp)
        outState.putBoolean("isKeepNorthUp", isKeepNorthUp)
        outState.putBoolean("isKeepDirectionUp", isKeepDirectionUp)
        outState.putLong(C.LOCATION_UPDATE_ACTION_OVERALLTIME, durationOverall)
        outState.putFloat(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, distanceOverallTotal)
        outState.putString(C.LOCATION_UPDATE_ACTION_OVERALLPACE, paceOverall)

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
        sendBroadcast(Intent(C.NOTIFICATION_ACTION_WP))
    }

    private fun handleCpOnClick() {
        sendBroadcast(Intent(C.NOTIFICATION_ACTION_CP))
    }

    @SuppressLint("SetTextI18n")
    private fun handleConfirmationOkOnClick() {
        // stopping the service
        val currentlyActiveSession: GpsSession =
            gpsSessionRepository.getSessionById(currentDbSessionId)
        currentlyActiveSession.distance = distanceOverallTotal.toDouble()
        currentlyActiveSession.speed = paceOverall
        currentlyActiveSession.duration = Utils.longToDateString(durationOverall)
        currentlyActiveSession.colorMin = colorMin
        currentlyActiveSession.colorMax = colorMax
        currentlyActiveSession.paceMin = paceMin
        currentlyActiveSession.paceMax = paceMax
        Log.d("updateSession in", "handleConfirmationOkOnClick()")
        gpsSessionRepository.updateSession(currentlyActiveSession)
        recyclerViewAdapter.addData(currentlyActiveSession)
        putRestGpsSession(currentlyActiveSession)
        val durationStartString = Utils.longToDateString(durationOverall)

        val temporaryDistance = "%.0f".format(distanceOverallTotal)
        val temporaryDuration = durationStartString
        val temporaryPace = paceOverall
        // stopping the service
        stopService(Intent(this, LocationService::class.java))
        buttonStartStop.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.baseline_play_arrow_24
            )
        )

        textViewOverallDistance.text = temporaryDistance
        textViewOverallDuration.text = temporaryDuration
        textViewOverallPace.text = temporaryPace


        includeStopConfirmation.visibility = View.INVISIBLE
        isStopConfirmationVisible = false


    }

    private fun handleConfirmationCancelOnClick() {
        includeStopConfirmation.visibility = View.INVISIBLE
        isStopConfirmationVisible = false
    }

    private fun handleStartStopOnClick() {
        isOldSessionLoaded = false
        if (locationServiceActive) {
            includeStopConfirmation.visibility = View.VISIBLE
            isStopConfirmationVisible = true


        } else {


            if (Build.VERSION.SDK_INT >= 26) {
                // starting the FOREGROUND service
                // service has to display non-dismissable notification within 5 secs
                val intent: Intent = Intent(this, LocationService::class.java)
                intent.putExtra(C.PACE_MAX, paceMax)
                intent.putExtra(C.PACE_MIN, paceMin)
                intent.putExtra(C.COLOR_MAX, colorMax)
                intent.putExtra(C.COLOR_MIN, colorMin)
                intent.putExtra(C.CURRENT_USER_DB_ID, currentDbUserId)
                intent.putExtra(C.CURRENT_USER_JWT, jwt)
                startForegroundService(intent)
            } else {
                val intent: Intent = Intent(this, LocationService::class.java)
                intent.putExtra(C.PACE_MAX, paceMax)
                intent.putExtra(C.PACE_MIN, paceMin)
                intent.putExtra(C.COLOR_MAX, colorMax)
                intent.putExtra(C.COLOR_MIN, colorMin)
                intent.putExtra(C.CURRENT_USER_DB_ID, currentDbUserId)
                intent.putExtra(C.CURRENT_USER_JWT, jwt)
                startService(intent)
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
        recyclerViewSessions.adapter!!.notifyDataSetChanged()
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
        // update rest
        putRestGpsSession(currentlyEditedSession)

        // close session window
        includeEditSession.visibility = View.INVISIBLE
        handleOpenOldSessionsOnClick()


    }

    private fun handleUpdateActiveSessionOnClick() {
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
            Log.d("updateSession in", "handleUpdateActiveSessionOnClick()")
            gpsSessionRepository.updateSession(currentSession)
            recyclerViewSessions.adapter!!.notifyDataSetChanged()

            // send updates to locationService
            val intent = Intent(C.UPDATE_OPTIONS_ACTION)
            intent.putExtra(C.GPS_UPDATE_FREQUENCY, (seekBarGpsFreq.progress) * 1000L)
            intent.putExtra(C.SYNC_UPDATE_FREQUENCY, (seekBarSyncFreq.progress))

            intent.putExtra(C.PACE_MIN, paceMin)
            intent.putExtra(C.PACE_MAX, paceMax)
            intent.putExtra(C.COLOR_MIN, colorMin)
            intent.putExtra(C.COLOR_MAX, colorMax)

            sendBroadcast(intent)

            // close session window
            includeOptions.visibility = View.INVISIBLE
            isOptionsVisible = false


        }
    }

    private fun handleLogOutOnClick() {
        includeWelcome.visibility = View.VISIBLE
        isWelcomeVisible = true
        if (locationServiceActive) {
            handleStartStopOnClick()
        }
        currentDbUserId = -1
        currentRestSessionId = "-1"
        currentDbSessionId = -1
        jwt = "-1"
        mMap.clear()
    }

    private fun handleKeepCenteredOnClick() {
        if (isMapCentered) {
            isMapCentered = false
            imageViewKeepCentered.setColorFilter(Utils.getAndroidColor("white"))
        } else {
            isMapCentered = true
            imageViewKeepCentered.setColorFilter(Utils.getAndroidColor("green"))
        }
        Log.d(TAG, isMapCentered.toString())
    }

    private fun handleSwitchDirectionOnClick() {
        if (isKeepNorthUp) {
            isKeepUserChosenUp = true
            isKeepNorthUp = false
            isKeepDirectionUp = false
            drawSwitchDirection()
        } else if (isKeepUserChosenUp) {

            isKeepUserChosenUp = false
            isKeepNorthUp = false
            isKeepDirectionUp = true
            drawSwitchDirection()
        } else if (isKeepDirectionUp) {

            isKeepUserChosenUp = false
            isKeepNorthUp = true
            isKeepDirectionUp = false
            drawSwitchDirection()
        }

    }

    private fun handleResetOnClick() {
        isKeepUserChosenUp = true
        isKeepNorthUp = false
        isKeepDirectionUp = false
        drawSwitchDirection()
        isMapCentered = false
    }


    private fun drawSwitchDirection() {
        if (isKeepDirectionUp) {
            imageViewSwitchDirection.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.baseline_navigation_white_24
                )
            )
            imageViewSwitchDirection.setColorFilter(Utils.getAndroidColor("white"))
        } else if (isKeepNorthUp) {
            imageViewSwitchDirection.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.baseline_navigation_white_24
                )
            )
            imageViewSwitchDirection.setColorFilter(Utils.getAndroidColor("blue"))
        } else {
            imageViewSwitchDirection.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.baseline_emoji_people_white_24
                )
            )
            imageViewSwitchDirection.setColorFilter(Utils.getAndroidColor("white"))
        }
    }

    // ============================================== BROADCAST RECEIVER =============================================
    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            // Log.d(TAG, intent!!.action)
            when (intent!!.action) {
                C.LOCATION_UPDATE_ACTION -> {


                    distanceOverallDirect =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALLDIRECT, 0.0F)
                    distanceOverallTotal =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, 0.0F)

                    if (!intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLPACE)
                            .isNullOrEmpty()
                    ) {
                        paceOverall =
                            intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLPACE)!!
                    }

                    durationOverall = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_OVERALLTIME, 0L)

                    val durationStartString = Utils.longToDateString(durationOverall)
                    if (distanceOverallDirect != 0.0f && durationOverall != 0L) {
                        textViewOverallDistance.text = "%.0f".format(distanceOverallTotal)
                        textViewOverallDuration.text = durationStartString

                    }
                    textViewOverallPace.text = paceOverall



                    textViewCPTotal.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CPTOTAL, 0.0F).toInt()
                            .toString()
                    textViewCPDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CPDIRECT, 0.0F).toInt()
                            .toString()
                    textViewCPPace.text =
                        intent.getStringExtra(C.LOCATION_UPDATE_ACTION_CPPACE)

                    textViewWPTotal.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WPTOTAL, 0.0F).toInt()
                            .toString()
                    textViewWPDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WPDIRECT, 0.0F).toInt()
                            .toString()
                    textViewWPPace.text =
                        intent.getStringExtra(C.LOCATION_UPDATE_ACTION_WPPACE)
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
                    currentDbSessionId = intent.getIntExtra(C.CURRENT_SESSION_ID, -1)
                    currentRestSessionId = intent.getStringExtra(C.CURRENT_SESSION_REST_ID)

                    updateCameraDirection(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0),
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_BEARING, 0f)
                    )
                }

            }
        }
    }


    // ============================================== HANDLE MAP =============================================
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
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

    private fun updateCameraDirection(lat: Double, lng: Double, bearing: Float) {
        if (lat != 0.0 && lng != 0.0) {
            if (isMapCentered) {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            lat,
                            lng
                        )
                    )
                )
                Log.d(TAG, "mapCentered: Yes")
            }
            if (isKeepNorthUp) {
                updateCameraBearing(mMap, 0f)
                Log.d(TAG, "direction: isKeepNorthUp")
            } else if (isKeepDirectionUp) {
                updateCameraBearing(mMap, if (bearing != 0f) bearing else 0f)
                Log.d(TAG, "direction: isKeepDirectionUp")
            } else if (isKeepUserChosenUp) {
                //updateCameraBearing(mMap, null)
                Log.d(TAG, "direction: isKeepUserChosenUp")
            }
        }
    }

    private fun updateCameraBearing(
        googleMap: GoogleMap?,
        bearing: Float?
    ) {
        if (googleMap == null) return
        val camPos = bearing?.let {
            CameraPosition
                .builder(
                    googleMap.cameraPosition // current Camera
                )
                .bearing(it)
                .build()
        }
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos))
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

                        if (startPointMarker == null || oldLocation == null) {
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
                            val pace: Float =
                                Utils.getPaceMinutesFloat(newTimeDifference, distanceFromLastPoint)

                            val newColor = Utils.calculateMapPolyLineColor(
                                loadedMinPace.toInt() / 60,
                                loadedMaxPace.toInt() / 60,
                                loadedMinColor,
                                loadedMaxColor,
                                pace
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

    // ============================================== RESTORE =============================================
    private fun restoreOptionsState() {
        if (isOptionsVisible) {
            includeOptions.visibility = View.VISIBLE
        } else {
            includeOptions.visibility = View.INVISIBLE
        }
    }

    private fun restoreStopConfirmationState() {
        if (isStopConfirmationVisible) {
            includeStopConfirmation.visibility = View.VISIBLE
        } else {
            includeStopConfirmation.visibility = View.INVISIBLE
        }
    }

    private fun restoreOverallTextViews() {
        val durationStartString = Utils.longToDateString(durationOverall)

        val temporaryDistance = "%.0f".format(distanceOverallTotal)
        val temporaryDuration = durationStartString
        val temporaryPace = paceOverall

        textViewOverallDistance.text = temporaryDistance
        textViewOverallDuration.text = temporaryDuration
        textViewOverallPace.text = temporaryPace
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

    private fun restoreLoginState() {
        if (isRegisterVisible) {
            includeRegister.visibility = View.VISIBLE
        } else {
            includeRegister.visibility = View.INVISIBLE
        }
        if (isLoginVisible) {
            includeLogin.visibility = View.VISIBLE
        } else {
            includeLogin.visibility = View.INVISIBLE
        }
        if (isWelcomeVisible) {
            includeWelcome.visibility = View.VISIBLE
        } else {
            includeWelcome.visibility = View.INVISIBLE
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

    fun startExportingSession(gpsSession: GpsSession) {
        buttonSendGPX.setOnClickListener { handleSendGpxOnClick(gpsSession) }
        buttonCancelExport.setOnClickListener { handleCancelExportOnClick() }
        includeExport.visibility = View.VISIBLE
        handleCloseOldSessionsOnClick()
    }

    private fun handleSendGpxOnClick(gpsSession: GpsSession) {

        if (editTextEmailToExport.text.toString().isEmailValid()) {

            try {
                if (gpsSession.restId != null) {
                    val content = Utils.generateGPX(
                        gpsSession.restId!!,
                        gpsLocationRepository.getLocationsBySessionId(gpsSession.id)
                    )

                    val tempFile = File.createTempFile(
                        gpsSession.restId!!,
                        ".gpx",
                        this.externalCacheDir
                    )

                    val fw = FileWriter(tempFile)

                    fw.write(content)

                    fw.flush()
                    fw.close()

                    val mailTo = "mailto:" + editTextEmailToExport.text.toString() +
                            "?&subject=" + Uri.encode("GPX file") +
                            "&body=" + Uri.encode("See attachments")
                    val emailIntent = Intent(Intent.ACTION_VIEW)
                    emailIntent.data = Uri.parse(mailTo)
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile))
                    startActivityForResult(emailIntent, 69)
                    Log.d(TAG, "email is sent!")
                } else {
                    Log.d(TAG, "Sessions restid is null!")
                }


            } catch (e: Exception) {
                Log.d(TAG, "No permissions!")
            }



            includeExport.visibility = View.INVISIBLE
            buttonSendGPX.setOnClickListener(null)
            buttonCancelExport.setOnClickListener(null)
        } else {
            textViewReceiverEmail.setTextColor(Utils.getAndroidColor("red"))
        }

    }

    private fun handleCancelExportOnClick() {
        Log.d(TAG, "handleCancelExportOnClick")
        includeExport.visibility = View.INVISIBLE
        buttonSendGPX.setOnClickListener(null)
        buttonCancelExport.setOnClickListener(null)
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
            textViewOverallDistance.text = loadedSession!!.distance.toInt().toString()
            textViewOverallDuration.text = loadedSession!!.duration
            textViewOverallPace.text = loadedSession!!.speed
        }
    }

    // ============================================== LOGIN AND REGISTER =============================================
    private fun handleLoginOnClick() {// api/v1.0/Account/Login
        val email: String = editTextEmailLogin.text.toString()
        val password: String = editTextPasswordLogin.text.toString()
        Log.d(TAG, "Login: E-mail: $email, Password: $password")
        loginUser(email, password)

    }

    private fun handleRegisterOnClick() { // /api/v1.0/Account/Register
        val firstName: String = editTextFirstName.text.toString()
        val lastName: String = editTextLastName.text.toString()
        val email: String = editTextEmailRegister.text.toString()
        val password: String = editTextPasswordRegister.text.toString()
        Log.d(
            TAG,
            "Register: First name: $firstName, Last name: $lastName, E-mail: $email, Password: $password"
        )
        registerUser(firstName, lastName, email, password)
        /*{
          "email": "user@example.com",
          "password": "string",
          "firstName": "string",
          "lastName": "string"
        } */
    }

    private fun handleShowLoginOnClick() {
        includeWelcome.visibility = View.INVISIBLE
        includeLogin.visibility = View.VISIBLE
        editTextEmailLogin.setText(C.REST_USERNAME)
        editTextPasswordLogin.setText(C.REST_PASSWORD)
        isWelcomeVisible = false
        isLoginVisible = true

    }

    private fun handleShowRegisterOnClick() {
        includeWelcome.visibility = View.INVISIBLE
        includeRegister.visibility = View.VISIBLE
        isWelcomeVisible = false
        isRegisterVisible = true
    }

    private fun handleShowWelcomeOnClick() {
        includeRegister.visibility = View.INVISIBLE
        includeLogin.visibility = View.INVISIBLE
        includeWelcome.visibility = View.VISIBLE
        isWelcomeVisible = true
        isLoginVisible = false
        isRegisterVisible = false

    }

    @SuppressLint("SetTextI18n")
    private fun registerUser(firstName: String, lastName: String, email: String, password: String) {
        textViewEmailRegister.setTextColor(Utils.getAndroidColor("black"))
        textViewEmailRegister.setText("E-mail")
        textViewPasswordRegister.setTextColor(Utils.getAndroidColor("black"))
        textViewPasswordRegister.setText("Password")
        if (email.isEmailValid() && password.isAlphaNumeric()) {
            val handler = WebApiSingletonHandler.getInstance(applicationContext)
            val requestJsonParameters = JSONObject()
            requestJsonParameters.put("firstName", firstName)
            requestJsonParameters.put("lastName", lastName)
            requestJsonParameters.put("email", email)
            requestJsonParameters.put("password", password)

            val httpRequest = JsonObjectRequest(
                Request.Method.POST,
                C.REST_BASE_URL + "account/register",
                requestJsonParameters,
                Response.Listener { response ->
                    Log.d(TAG, response.toString())
                    jwt = response.getString("token")
                    includeRegister.visibility = View.INVISIBLE
                    includeLogin.visibility = View.INVISIBLE
                    includeWelcome.visibility = View.INVISIBLE
                    isRegisterVisible = false
                    isLoginVisible = false
                    isWelcomeVisible = false
                    currentDbUserId =
                        appUserRepository.add(AppUser(email, password, firstName, lastName))
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, error.toString())
                }
            )

            handler.addToRequestQueue(httpRequest)

        } else {
            if (!email.isEmailValid()) {
                textViewEmailRegister.setTextColor(Utils.getAndroidColor("red"))

            }
            if (!password.isAlphaNumeric()) {
                textViewPasswordRegister.text = "Password (a-z|A-Z|0-9|.,;'?! ...)"
                textViewPasswordRegister.setTextColor(Utils.getAndroidColor("red"))

            }
        }

    }

    private fun loginUser(email: String, password: String) {
        textViewEmailRegister.setTextColor(Utils.getAndroidColor("black"))
        textViewPasswordRegister.setTextColor(Utils.getAndroidColor("black"))
        if (email.isEmailValid() && password.isAlphaNumeric()) {
            val handler = WebApiSingletonHandler.getInstance(applicationContext)
            val requestJsonParameters = JSONObject()
            requestJsonParameters.put("email", email)
            requestJsonParameters.put("password", password)

            val httpRequest = JsonObjectRequest(
                Request.Method.POST,
                C.REST_BASE_URL + "account/login",
                requestJsonParameters,
                Response.Listener { response ->
                    Log.d(TAG, response.toString())
                    jwt = response.getString("token")
                    currentDbUserId = appUserRepository.getUserIdByEmail(email)

                    if (currentDbUserId == -1) {
                        currentDbUserId = appUserRepository.add(
                            AppUser(
                                email, password,
                                response.getString("firstName"), response.getString("lastName")
                            )
                        )
                    }

                    includeRegister.visibility = View.INVISIBLE
                    includeLogin.visibility = View.INVISIBLE
                    includeWelcome.visibility = View.INVISIBLE
                    isRegisterVisible = false
                    isLoginVisible = false
                    isWelcomeVisible = false
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, error.toString())
                    textViewEmailLogin.setTextColor(Utils.getAndroidColor("red"))
                    textViewPasswordLogin.setTextColor(Utils.getAndroidColor("red"))
                }
            )

            handler.addToRequestQueue(httpRequest)
        } else {

            textViewEmailLogin.setTextColor(Utils.getAndroidColor("red"))
            textViewPasswordLogin.setTextColor(Utils.getAndroidColor("red"))

        }
    }

    fun deleteRestGpsSession(gpsSessionId: String) {
        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        Log.d(TAG, "delete session ${C.REST_BASE_URL + "GpsSessions/$gpsSessionId"}")
        val httpRequest = object : StringRequest(
            Request.Method.DELETE,
            C.REST_BASE_URL + "GpsSessions/$gpsSessionId",

            Response.Listener<String> { _ ->

                //Log.d(TAG, response)
                Log.d(TAG, "delete session successful")
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
                Log.d(TAG, "delete session unsuccessful")
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

    private fun putRestGpsSession(gpsSession: GpsSession) {


        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("id", gpsSession.restId)
        requestJsonParameters.put("name", gpsSession.name)
        requestJsonParameters.put("description", gpsSession.description)
        requestJsonParameters.put("recordedAt", gpsSession.recordedAt)
        requestJsonParameters.put(
            "duration",
            Utils.convertDurationStringToDouble(gpsSession.duration)
        )
        requestJsonParameters.put("speed", Utils.convertSpeedStringToDouble(gpsSession.speed))
        requestJsonParameters.put("distance", gpsSession.distance)
        requestJsonParameters.put("climb", gpsSession.climb)
        requestJsonParameters.put("descent", gpsSession.descent)
        requestJsonParameters.put("paceMin", gpsSession.paceMin)
        requestJsonParameters.put("paceMax", gpsSession.paceMax)
        requestJsonParameters.put("gpsSessionTypeId", "00000000-0000-0000-0000-000000000001")

        val httpRequest = object : StringRequest(
            Request.Method.PUT,
            C.REST_BASE_URL + "GpsSessions/" + gpsSession.restId,

            Response.Listener<String> { _ ->

                //Log.d(TAG, response)
                Log.d(TAG, "update session successful")
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
                Log.d(TAG, "update session unsuccessful")
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

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                Log.d("getBody", requestJsonParameters.toString())
                return requestJsonParameters.toString().toByteArray()
            }
        }

        handler.addToRequestQueue(httpRequest)
    }


    private fun String.isEmailValid() =
        Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        ).matcher(this).matches()

    private fun String.isAlphaNumeric() =
        Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[\\W])[\\w\\W]+$").matcher(this)
            .find()


    // ============================================== SEEKBAR LISTENER =============================================
    @SuppressLint("SetTextI18n")
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (seekBar != null) {
            if (seekBar.id == seekBarGpsFreq.id) {

                if (progress != 0) {
                    if (progress == 1) {
                        textViewGpsUpdateFreq.setText("GPS frequency: ${progress} second")
                    } else {
                        textViewGpsUpdateFreq.setText("GPS frequency: ${progress} seconds")
                    }
                }
                //Log.d("seekbar", "GpsFreq id is ${seekBar.id}, new value is ${progress}")
            } else if (seekBar.id == seekBarSyncFreq.id) {

                if (progress != 0) {
                    if (progress == 1) {
                        textViewSyncFrequency.setText("Sync every location")
                    } else {
                        textViewSyncFrequency.setText("Sync every ${progress} locations")
                    }

                }
                //Log.d("seekbar", "SyncFreq id is ${seekBar.id}, new value is $progress")
            }

        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
/*
    fun exportDb() {

            try {
                val session = intent.getStringExtra(C.DISPLAY_SESSION_HASH)!!
                val to = intent.getStringExtra(C.EXPORT_TO_EMAIL)!!
                // session must be restid
                if (to.isEmailValid()) {
                    val content = Utils.generateGPX(
                        session,
                        gpsLocationRepository.getLocationsBySessionId(currentDbSessionId)
                    )

                    val tempFile = File.createTempFile(
                        session,
                        ".gpx",
                        this.externalCacheDir
                    )

                    val fw = FileWriter(tempFile)

                    fw.write(content)

                    fw.flush()
                    fw.close()

                    val mailTo = "mailto:" + to +
                            "?&subject=" + Uri.encode("GPX file") +
                            "&body=" + Uri.encode("See attachments")
                    val emailIntent = Intent(Intent.ACTION_VIEW)
                    emailIntent.data = Uri.parse(mailTo)
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile))
                    startActivityForResult(emailIntent, 69)

                } else {
                    makeToast("Given email is invalid!")
                }

            } catch (e: Exception) {
                makeToast("No permissions!")
            }
        }
    private fun makeToast(message: String) {
        Toast.makeText(
            this@MainActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

 */

}
