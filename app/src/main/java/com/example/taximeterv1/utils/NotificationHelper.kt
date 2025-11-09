package com.example.taximeterv1.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taximeterv1.MainActivity
import com.example.taximeterv1.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_TRIP = "taxi_meter_trip"
        private const val CHANNEL_ID_COMPLETED = "taxi_meter_completed"
        private const val NOTIFICATION_ID_TRIP = 1001
        private const val NOTIFICATION_ID_COMPLETED = 1002

        fun getTripNotificationId(): Int = NOTIFICATION_ID_TRIP
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for ongoing trip
            val tripChannel = NotificationChannel(
                CHANNEL_ID_TRIP,
                context.getString(R.string.trip_in_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current trip information"
                setShowBadge(false)
            }

            // Channel for completed trip
            val completedChannel = NotificationChannel(
                CHANNEL_ID_COMPLETED,
                context.getString(R.string.trip_completed),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for completed trips"
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(tripChannel)
            notificationManager?.createNotificationChannel(completedChannel)
        }
    }

    fun createTripNotification(
        distance: Double,
        time: Long,
        fare: Double
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_TRIP)
            .setSmallIcon(R.drawable.ic_taxi_notification)
            .setContentTitle(context.getString(R.string.trip_in_progress))
            .setContentText(
                context.getString(
                    R.string.current_fare,
                    fare
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${context.getString(R.string.distance)}: ${String.format("%.2f", distance)} km\n" +
                                "${context.getString(R.string.time)}: $time ${context.getString(R.string.min)}\n" +
                                "${context.getString(R.string.total_fare)}: ${String.format("%.2f", fare)} ${context.getString(R.string.dh)}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    fun showTripCompletedNotification(distance: Double, time: Long, fare: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETED)
            .setSmallIcon(R.drawable.ic_taxi_notification)
            .setContentTitle(context.getString(R.string.trip_completed))
            .setContentText(
                context.getString(
                    R.string.notification_content,
                    distance,
                    time,
                    fare
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${context.getString(R.string.trip_summary)}\n\n" +
                                "${context.getString(R.string.distance)}: ${String.format("%.2f", distance)} km\n" +
                                "${context.getString(R.string.time)}: $time ${context.getString(R.string.min)}\n" +
                                "${context.getString(R.string.total_fare)}: ${String.format("%.2f", fare)} ${context.getString(R.string.dh)}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_COMPLETED, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelTripNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TRIP)
    }
}