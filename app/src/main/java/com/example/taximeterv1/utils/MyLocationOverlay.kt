package com.example.taximeterv1.utils

import android.graphics.*
import android.location.Location
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Overlay personnalisé pour afficher la position actuelle style Google Maps
 * - Cercle bleu avec effet pulsant
 * - Flèche de direction basée sur le bearing
 * - Cercle de précision semi-transparent
 */
class MyLocationOverlay(
    private val mapView: MapView
) : Overlay() {

    private var currentLocation: GeoPoint? = null
    private var bearing: Float = 0f
    private var accuracy: Float = 0f
    private var animationPhase: Float = 0f

    // Paints
    private val accuracyPaint = Paint().apply {
        color = Color.parseColor("#4D2196F3") // Bleu semi-transparent
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val circlePaint = Paint().apply {
        color = Color.parseColor("#2196F3") // Bleu Google Maps
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#2196F3"))
    }

    private val circleBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val arrowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pulsePaint = Paint().apply {
        color = Color.parseColor("#4D2196F3")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    fun updateLocation(location: Location) {
        currentLocation = GeoPoint(location.latitude, location.longitude)
        bearing = location.bearing
        accuracy = location.accuracy
        mapView.invalidate()
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val location = currentLocation ?: return
        val point = mapView.projection.toPixels(location, null)

        // Animation pulsante
        animationPhase = (animationPhase + 0.05f) % 1f

        // 1. Dessiner le cercle de précision
        if (accuracy > 0) {
            // Calculer le rayon en pixels en fonction du zoom
            val metersPerPixel = (156543.03392 * Math.cos(location.latitude * Math.PI / 180.0) / Math.pow(2.0, mapView.zoomLevelDouble)).toFloat()
            val accuracyRadius = accuracy / metersPerPixel
            canvas.drawCircle(
                point.x.toFloat(),
                point.y.toFloat(),
                accuracyRadius,
                accuracyPaint
            )
        }

        // 2. Dessiner l'effet pulsant
        val pulseRadius = 20f + (animationPhase * 15f)
        val pulseAlpha = (255 * (1f - animationPhase)).toInt()
        pulsePaint.alpha = pulseAlpha
        canvas.drawCircle(
            point.x.toFloat(),
            point.y.toFloat(),
            pulseRadius,
            pulsePaint
        )

        // 3. Dessiner le cercle principal (position)
        canvas.drawCircle(
            point.x.toFloat(),
            point.y.toFloat(),
            16f,
            circlePaint
        )

        // 4. Dessiner le contour blanc
        canvas.drawCircle(
            point.x.toFloat(),
            point.y.toFloat(),
            16f,
            circleBorderPaint
        )

        // 5. Dessiner la flèche de direction (si bearing disponible)
        if (bearing != 0f) {
            canvas.save()
            canvas.rotate(bearing, point.x.toFloat(), point.y.toFloat())

            val arrowPath = Path().apply {
                moveTo(point.x.toFloat(), point.y.toFloat() - 10f) // Pointe
                lineTo(point.x.toFloat() - 5f, point.y.toFloat() + 5f) // Gauche
                lineTo(point.x.toFloat() + 5f, point.y.toFloat() + 5f) // Droite
                close()
            }

            canvas.drawPath(arrowPath, arrowPaint)
            canvas.restore()
        }

        // Continuer l'animation
        mapView.postInvalidate()
    }
}