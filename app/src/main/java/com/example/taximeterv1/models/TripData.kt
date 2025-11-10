package com.example.taximeterv1.models


import android.location.Location
import java.util.Calendar

data class TripData(
    var distanceInMeters: Float = 0f,
    var timeInSeconds: Long = 0L,
    var totalFare: Double = 0.0,
    var isActive: Boolean = false,
    var startTime: Long = 0L,
    var lastLocation: Location? = null,
    var waitingTimeSeconds: Long = 0L,
    var isWaiting: Boolean = false,
    var waitingStartTime: Long = 0L
) {
    val distanceInKm: Double
        get() = distanceInMeters / 1000.0

    val timeInMinutes: Long
        get() = timeInSeconds / 60

    val waitingTimeMinutes: Long
        get() = waitingTimeSeconds / 60

    fun reset() {
        distanceInMeters = 0f
        timeInSeconds = 0L
        totalFare = 0.0
        isActive = false
        startTime = 0L
        lastLocation = null
        waitingTimeSeconds = 0L
        isWaiting = false
        waitingStartTime = 0L
    }

    /**
     * Calcul du tarif selon la réglementation marocaine (LOGIQUE CORRIGÉE)
     */
    fun calculateFareMaroc(
        currentSpeed: Float = 0f
    ): Double {

        // --- LOGIQUE CORRIGÉE ---

        // 1. Définir les tarifs
        // "Tarif Minimum" (7.00 DH) est le prix plancher de la course.
        // "Prise en Charge" (ex: 2.00 DH) est le vrai frais de départ.

        val minimumFare = 7.00        // Le prix plancher (votre ancien "baseFare")
        val startingFee = 2.00        // La vraie prise en charge (frais de départ)

        // CORRIGÉ : Tarifs harmonisés
        val farePerKmDay = 3.50       // Tarif jour
        val farePerKmNight = 5.50     // Tarif nuit
        val farePerHourWaiting = 15.00  // Tarif attente (DH/heure)

        // Déterminer si c'est le tarif jour ou nuit
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNightTariff = hour < 6 || hour >= 20
        val farePerKm = if (isNightTariff) farePerKmNight else farePerKmDay

        // 2. Calculer le coût de la distance et de l'attente
        val distanceFare = distanceInKm * farePerKm
        val waitingFare = (waitingTimeSeconds / 3600.0) * farePerHourWaiting

        // 3. Calculer le total de la course (Départ + Distance + Attente)
        val totalCalculatedFare = startingFee + distanceFare + waitingFare

        // 4. Appliquer le tarif minimum
        // Le prix final est le plus élevé entre le total calculé et le tarif minimum.
        return maxOf(totalCalculatedFare, minimumFare)
    }

    /**
     * Détermine si le véhicule est à l'arrêt (attente)
     * Seuil: vitesse < 5 km/h
     */
    fun shouldBeInWaitingMode(speed: Float): Boolean {
        return speed < 5f && isActive
    }

    /**
     * Met à jour le temps d'attente
     */
    fun updateWaitingTime(currentTimeMillis: Long) {
        if (isWaiting && waitingStartTime > 0) {
            waitingTimeSeconds += (currentTimeMillis - waitingStartTime) / 1000
        }
    }
}