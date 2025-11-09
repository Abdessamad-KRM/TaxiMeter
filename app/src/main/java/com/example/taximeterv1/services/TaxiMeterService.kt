package com.example.taximeterv1.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.taximeterv1.utils.NotificationHelper

/**
 * Service optionnel pour le suivi en arrière-plan
 * Ce service n'est PAS nécessaire pour le fonctionnement de base de l'app
 */
class TaxiMeterService : Service() {

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si vous voulez un service en avant-plan (foreground)
        // startForeground(NotificationHelper.getTripNotificationId(),
        //                notificationHelper.createTripNotification(0.0, 0L, 0.0))

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}