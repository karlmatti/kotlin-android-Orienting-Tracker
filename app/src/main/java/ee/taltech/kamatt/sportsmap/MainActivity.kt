package ee.taltech.kamatt.sportsmap

import android.Manifest
// do not import this! never! If this get inserted automatically when pasting java code, remove it
//import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_MAGNETIC_FIELD
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_GAME
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.buttons_top.*
import kotlinx.android.synthetic.main.track_control.*
import java.lang.Math.toDegrees


class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }
    //  Compass
    lateinit var sensorManager: SensorManager
    lateinit var compassImage: ImageView
    lateinit var accelerometer: Sensor
    lateinit var magnetometer: Sensor
    var currentDegree = 0.0f
    var lastAccelerometer = FloatArray(3)
    var lastMagnetometer = FloatArray(3)
    var lastAccelerometerSet = false
    var lastMagnetometerSet = false

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    //  private var markerCP: Marker? = null
    //  private var markerWP: Marker? = null
    private var mapPolyline: Polyline? = null
    private var locationServiceActive = false
    private var isCompassEnabled = true
    private var isOptionsEnabled = false

    private var polyLineMinSpeed: Int = 4
    private var polyLineMaxSpeed: Int = 7
    private var polyLineMinColor: String = "green"
    private var polyLineMaxColor: String = "red"
    // ============================================== MAIN ENTRY - ONCREATE =============================================
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        imageButtonCP.setOnClickListener { handleCpOnClick() }
        imageButtonWP.setOnClickListener { handleWpOnClick() }
        buttonStartStop.setOnClickListener { handleStartStopOnClick() }
        imageViewToggleCompass.setOnClickListener { handleToggleCompass() }
        imageViewOptions.setOnClickListener { handleOptionsOnClick() }
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
        compassImage = findViewById<ImageView>(R.id.imageViewNorthDir)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)

    }



    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

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
                .setAction("OK", {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        C.REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                })
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
            if (grantResults.count() <= 0) { // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
                Toast.makeText(this, "User interaction was cancelled.", Toast.LENGTH_SHORT).show()
            } else if (grantResults[0] === PackageManager.PERMISSION_GRANTED) {// Permission was granted.
                Log.i(TAG, "Permission was granted")
                Toast.makeText(this, "Permission was granted", Toast.LENGTH_SHORT).show()
            } else { // Permission denied.
                Snackbar.make(
                    findViewById(R.id.activity_main),
                    "You denied GPS! What can I do?",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Settings", {
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
                    })
                    .show()
            }
        }

    }

    // ============================================== CLICK HANDLERS =============================================

    private fun handleWpOnClick() {
        Log.d(TAG, "buttonWPOnClick")
        sendBroadcast(Intent(C.NOTIFICATION_ACTION_WP))

        val lat = intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0F).toDouble()
        val lng = intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0F).toDouble()


    }

    private fun handleCpOnClick() {
        Log.d(TAG, "buttonCPOnClick")
        sendBroadcast(Intent(C.NOTIFICATION_ACTION_CP))

    }

    private fun handleStartStopOnClick() {
        Log.d(TAG, "buttonStartStop. locationServiceActive: $locationServiceActive")
        if (locationServiceActive) {
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
                startForegroundService(Intent(this, LocationService::class.java))
            } else {
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
        if (isCompassEnabled) {
            compassImage.setBackgroundResource(0)
            isCompassEnabled = false

        } else {
            compassImage.setBackgroundResource(R.drawable.baseline_arrow_upward_black_24)
            isCompassEnabled = true
        }
    }

    private fun handleOptionsOnClick() {

        if (isOptionsEnabled) {
            includeOptions.visibility = View.INVISIBLE

            isOptionsEnabled = false

        } else {
            includeOptions.visibility = View.VISIBLE
            isOptionsEnabled = true
        }
    }

    // ============================================== BROADCAST RECEIVER =============================================
    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Log.d(TAG, intent!!.action)
            when (intent!!.action) {
                C.LOCATION_UPDATE_ACTION -> {
                    textViewOverallDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALLTOTAL, 0.0F).toInt()
                            .toString()
                    textViewOverallTotal.text =
                        intent.getStringExtra(C.LOCATION_UPDATE_ACTION_OVERALLTIME)
                    textViewOverallTempo.text =
                        intent.getIntExtra(C.LOCATION_UPDATE_ACTION_OVERALLTEMPO, 0).toString()

                    textViewCPTotal.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CPTOTAL, 0.0F).toInt()
                            .toString()
                    textViewCPDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CPDIRECT, 0.0F).toInt()
                            .toString()
                    textViewCPTempo.text =
                        intent.getIntExtra(C.LOCATION_UPDATE_ACTION_CPTEMPO, 0).toString()

                    textViewWPTotal.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WPTOTAL, 0.0F).toInt()
                            .toString()
                    textViewWPDirect.text =
                        intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WPDIRECT, 0.0F).toInt()
                            .toString()
                    textViewWPTempo.text =
                        intent.getIntExtra(C.LOCATION_UPDATE_ACTION_WPTEMPO, 0).toString()

                    updateMap(
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LATITUDE, 0.0),
                        intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LONGITUDE, 0.0)
                    )

                }
                C.LOCATION_UPDATE_STOP -> {

                }
            }
        }
    }

    // ============================================== HANDLE MAP =============================================
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(59.3927437, 24.6642), 17.0f))

    }

    private fun updateMap(lat: Double, lon: Double) {
        // mMap.clear()
        val center = LatLng(lat, lon)

        if (marker != null) {
            marker!!.remove()
        }

        if (mapPolyline != null) {
            mapPolyline!!.remove()
        }

        /*
        marker = mMap
            .addMarker(
                MarkerOptions()
                    .position(center)
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    .anchor(0.5f, 0.5f) // use icon center for lat,lon
            )*/

        var polylineOptions = Utils.getMapPolylineOptions()

        Utils.setMapPolyLineColor(0xffff0000.toInt())
        mapPolyline = mMap.addPolyline(polylineOptions)



        ///mMap.moveCamera(CameraUpdateFactory.newLatLng(center))
    }

    /*
    private fun addCPMarkerOnMap(lat: Double, lon: Double) {
        val location = LatLng(lat, lon)
        markerCP = mMap
            .addMarker(
                MarkerOptions()
                    .position(location)
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    .anchor(0.5f, 0.5f) // use icon center for lat,lon
            )
    }
    private fun addWPMarkerOnMap(lat: Double, lng: Double) {
        val location = LatLng(lat, lng)
        Log.d("addWPMarker", "lat")
        Log.d("addWPMarker", "lng")
        if (markerWP != null) {
            markerWP!!.remove()
        }
        markerWP = mMap
            .addMarker(
                MarkerOptions()
                    .position(location)
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                    .anchor(0.5f, 0.5f) // use icon center for lat,lon
            )
        Log.d("addWPMarker", "end")
    }*/

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
                        RELATIVE_TO_SELF, 0.5f)
                    rotateAnimation.duration = 1000
                    rotateAnimation.fillAfter = true

                    compassImage.startAnimation(rotateAnimation)
                    currentDegree = -degree
                }
            }


    }

    fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }
}
