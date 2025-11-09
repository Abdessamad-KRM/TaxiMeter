package com.example.taximeterv1.utils

import android.location.Location

object DistanceCalculator {

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(startLocation: Location?, endLocation: Location?): Float {
        if (startLocation == null || endLocation == null) {
            return 0f
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            startLocation.latitude,
            startLocation.longitude,
            endLocation.latitude,
            endLocation.longitude,
            results
        )

        return results[0]
    }

    /**
     * Format distance for display
     */
    fun formatDistance(distanceInMeters: Float): String {
        val distanceInKm = distanceInMeters / 1000.0
        return String.format("%.2f", distanceInKm)
    }

    /**
     * Format time for display (MM:SS)
     */
    fun formatTime(timeInSeconds: Long): String {
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Format fare for display
     */
    fun formatFare(fare: Double): String {
        return String.format("%.2f", fare)
    }
}