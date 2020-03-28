# Using Fused Location in background

Manifest permissions
~~~~
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
~~~~

Manifest service
~~~~
        <!-- This is critical: android:foregroundServiceType="location" -->
        <service android:name=".LocationService"
            android:foregroundServiceType="location"
            android:enabled="true"
            android:exported="true"
            android:launchMode="singleTask" />
~~~~


Starting the foreground service from main activity (foreground - basicaly active app for android, just without main ui).
To let user know of that - non-dismissable notification is mandatory.
~~~~
            if (Build.VERSION.SDK_INT >= 26) {
                // starting the FOREGROUND service
                // service has to display non-dismissable notification within 5 secs
                startForegroundService(Intent(this, LocationService::class.java))
            } else {
                startService(Intent(this, LocationService::class.java))
            }
~~~~


In service, show notification with startForeground
~~~~
        // construct and show notification
        var builder = NotificationCompat.Builder(applicationContext, C.NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.baseline_gps_fixed_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.setContent(notifyview)

        // Super important, start as foreground service - ie android considers this as an active app. Need visual reminder - notification.
        // must be called within 5 secs after service starts.
        startForeground(C.NOTIFICATION_ID, builder.build())
~~~~


This should be it, rest is standard. Take care of permissions, use fused location, etc...
