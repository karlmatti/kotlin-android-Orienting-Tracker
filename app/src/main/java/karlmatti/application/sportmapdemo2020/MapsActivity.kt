package karlmatti.application.sportmapdemo2020

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var provider = ""

    private var distance = 0f
    private var prevLocation: Location? = null

    private var accSensor: Sensor? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissions problem")

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        provider = locationManager.getBestProvider(criteria, false)
        Log.d(TAG, "Provider: $provider")

        val location = locationManager.getLastKnownLocation(provider)

        if (location != null){
            Log.d("onCreate", "onLocationChanged(location)")
            onLocationChanged(location)
        } else {
            Log.w(TAG, "getLastKnownLocation null")
        }

        //createNotificationChannel()
    }
    fun onLocationChanged(location: Location?) {
        val lat = location!!.latitude
        val lng = location!!.longitude
        val position = LatLng(lat, lng)


/*
        if (prevLocation != null){
            distance = distance + prevLocation!!.distanceTo(location)
            textViewDistance.text = distance.toString()
        }
*/
        prevLocation = location
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        // Add a marker in Sydney and move the camera
        //val sydney = LatLng(-34.0, 151.0)
        //mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        val lat = prevLocation!!.latitude
        val lng = prevLocation!!.longitude
        val position = LatLng(lat, lng)
        mMap.addMarker(MarkerOptions().position(position).title("Marker in current location"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
    }

    fun buttonStartStopOnClick(view: View) {}


/*
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Default Channel for Notification demo"
            var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun onClickButtonNotif1(view: View) {
        var builder = NotificationCompat.Builder(this, "channel")
            // setSmallIcon is mandatory
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("Title text")
            .setContentText("Content text is here")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(this).notify(0, builder.build())
    }
    fun onClickButtonNotif2(view: View) {
        val intent = Intent(this, NotifResultActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        var builder = NotificationCompat.Builder(this, "channel")
            // setSmallIcon is mandatory
            .setSmallIcon(R.drawable.baseline_room_24)
            .setContentTitle("Title text 2")
            .setContentText("Content text is here 2")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        NotificationManagerCompat.from(this).notify(1, builder.build())
    }
    fun onClickButtonNotif3(view: View) {}
    fun onClickButtonNotif4(view: View) {}

 */
}

private fun GoogleMap.setOnMarkerClickListener(mapsActivity: MapsActivity) {
    TODO("Not yet implemented")
}
