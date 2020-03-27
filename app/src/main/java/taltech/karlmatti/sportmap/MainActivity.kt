package taltech.karlmatti.sportmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
    }

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
}
