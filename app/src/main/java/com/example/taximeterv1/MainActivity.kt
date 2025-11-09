package com.example.taximeterv1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import com.example.taximeterv1.models.TripData
import com.example.taximeterv1.utils.DistanceCalculator
import com.example.taximeterv1.utils.NotificationHelper
import com.example.taximeterv1.utils.MyLocationOverlay // Importation de votre curseur personnalisé
import com.google.android.gms.location.*
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.util.Calendar

class MainActivity : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks {

    // UI Components
    private lateinit var tvTotalFare: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvTariffMode: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvRates: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var layoutStopReset: LinearLayout
    private lateinit var btnProfile: ImageButton

    // OSMdroid Map
    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationOverlay // Utilise votre classe
    private var startMarker: Marker? = null
    private var routePolyline: Polyline? = null

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    // Trip Data
    private val tripData = TripData()
    private val routePoints = mutableListOf<GeoPoint>()
    private var currentSpeed: Float = 0f

    // Timer
    private val timerHandler = Handler(Looper.getMainLooper())
    private var lastUpdateTime: Long = 0L

    // MODIFIÉ 1: Le timer tourne TOUJOURS pour mettre à jour l'UI (Jour/Nuit)
    private val timerRunnable = object : Runnable {
        override fun run() {
            // Cette partie calcule la course UNIQUEMENT si elle est active
            if (tripData.isActive) {
                val currentTime = System.currentTimeMillis()
                // Update total time
                tripData.timeInSeconds = (currentTime - tripData.startTime) / 1000

                // Check waiting mode based on speed
                val shouldWait = tripData.shouldBeInWaitingMode(currentSpeed)

                if (shouldWait && !tripData.isWaiting) {
                    tripData.isWaiting = true
                    tripData.waitingStartTime = currentTime
                } else if (!shouldWait && tripData.isWaiting) {
                    tripData.updateWaitingTime(currentTime)
                    tripData.isWaiting = false
                    tripData.waitingStartTime = 0L
                } else if (tripData.isWaiting) {
                    tripData.updateWaitingTime(currentTime)
                    tripData.waitingStartTime = currentTime
                }

                lastUpdateTime = currentTime
            }

            // Mettez à jour l'UI CHAQUE SECONDE (pour le mode jour/nuit)
            updateUI()

            // Relancez le timer quoi qu'il arrive
            timerHandler.postDelayed(this, 1000)
        }
    }

    // Notification Helper
    private lateinit var notificationHelper: NotificationHelper

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_UPDATE_INTERVAL = 2000L // 2 seconds
        private const val LOCATION_FASTEST_INTERVAL = 1000L // 1 second
        private const val DEFAULT_ZOOM = 17.0 // OSMdroid uses Double for zoom
        private const val WAITING_SPEED_THRESHOLD = 5f // km/h
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMdroid Configuration
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        initViews()
        apply7SegmentFont()
        setupOsmMap()
        setupLocationClient()
        setupListeners()
        // MODIFIÉ 2: updateRatesDisplay() est supprimé d'ici
        notificationHelper = NotificationHelper(this)

        // MODIFIÉ 3: Démarrer le timer global ici
        timerHandler.post(timerRunnable)
    }

    private fun initViews() {
        tvTotalFare = findViewById(R.id.tvTotalFare)
        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)
        tvTariffMode = findViewById(R.id.tvTariffMode)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvRates = findViewById(R.id.tvRates)
        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        layoutStopReset = findViewById(R.id.layoutStopReset)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun apply7SegmentFont() {
        try {
            val typeface = ResourcesCompat.getFont(this, R.font.digital_7)
            tvTotalFare.typeface = typeface
            tvDistance.typeface = typeface
            tvTime.typeface = typeface
            tvTariffMode.typeface = typeface
            tvSpeed.typeface = typeface
            tvStatus.typeface = typeface
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur chargement police", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupOsmMap() {
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)
        mapView.isVerticalMapRepetitionEnabled = false
        mapView.isHorizontalMapRepetitionEnabled = false

        val mapController = mapView.controller
        mapController.setZoom(12.0)
        val startPoint = GeoPoint(34.020882, -6.841650) // Rabat
        mapController.setCenter(startPoint)

        checkLocationPermission()
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentSpeed = if (location.hasSpeed()) {
                        location.speed * 3.6f
                    } else {
                        0f
                    }
                    onLocationUpdate(location)
                }
            }
        }
    }

    private fun setupListeners() {
        btnStart.setOnClickListener {
            startTrip()
        }
        btnStop.setOnClickListener {
            showStopConfirmationDialog()
        }
        btnReset.setOnClickListener {
            resetTrip()
        }
        btnProfile.setOnClickListener {
            startActivity(Intent(this, DriverProfileActivity::class.java))
        }
    }

    private fun checkLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            enableMyLocation()
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.location_permission_required),
                LOCATION_PERMISSION_REQUEST_CODE,
                *permissions
            )
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Initialise votre curseur personnalisé
            locationOverlay = MyLocationOverlay(mapView)
            mapView.overlays.add(locationOverlay)

            getCurrentLocation()
            tvStatus.text = "PRÊT - GPS ACTIF"
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentGeoPoint = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(currentGeoPoint)
                    mapView.controller.setZoom(DEFAULT_ZOOM)

                    // Met à jour le curseur dès la première position
                    locationOverlay.updateLocation(it)
                }
            }
        }
    }

    private fun startTrip() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Permission de localisation requise", Toast.LENGTH_SHORT).show()
            return
        }

        tripData.isActive = true
        tripData.startTime = System.currentTimeMillis()
        lastUpdateTime = tripData.startTime
        routePoints.clear()

        btnStart.visibility = View.GONE
        layoutStopReset.visibility = View.VISIBLE

        startLocationUpdates()

        // MODIFIÉ 4: Pas besoin de démarrer le timer, il tourne déjà
        // timerHandler.post(timerRunnable)

        tvStatus.text = "COURSE EN COURS"
        Toast.makeText(this, "Course démarrée", Toast.LENGTH_SHORT).show()
    }

    private fun stopTrip() {
        tripData.isActive = false
        if (tripData.isWaiting) {
            tripData.updateWaitingTime(System.currentTimeMillis())
        }
        stopLocationUpdates()
        // Le timer continue de tourner pour l'UI, on ne le stoppe pas
        notificationHelper.showTripCompletedNotification(
            tripData.distanceInKm,
            tripData.timeInMinutes,
            tripData.totalFare
        )
        tvStatus.text = "COURSE TERMINÉE"
        Toast.makeText(this, "Course terminée - ${String.format("%.2f", tripData.totalFare)} DH", Toast.LENGTH_LONG).show()
    }

    private fun resetTrip() {
        if (tripData.isActive) {
            stopTrip()
        }
        tripData.reset()
        currentSpeed = 0f
        routePoints.clear()

        if (startMarker != null) {
            mapView.overlays.remove(startMarker)
            startMarker = null
        }
        if (routePolyline != null) {
            routePolyline?.setPoints(emptyList())
        }
        mapView.invalidate()

        btnStart.visibility = View.VISIBLE
        layoutStopReset.visibility = View.GONE

        updateUI() // Mettre à jour l'UI immédiatement
        tvStatus.text = "PRÊT"
        Toast.makeText(this, "Compteur réinitialisé", Toast.LENGTH_SHORT).show()
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun onLocationUpdate(location: Location) {
        if (!tripData.isActive) return

        val currentGeoPoint = GeoPoint(location.latitude, location.longitude)

        // Met à jour la position de votre curseur personnalisé
        locationOverlay.updateLocation(location)

        if (tripData.lastLocation != null) {
            val distance = DistanceCalculator.calculateDistance(tripData.lastLocation, location)

            if (currentSpeed >= WAITING_SPEED_THRESHOLD && distance > 2) {
                tripData.distanceInMeters += distance
                routePoints.add(currentGeoPoint)

                if (routePolyline == null) {
                    routePolyline = Polyline()
                    routePolyline?.color = ContextCompat.getColor(this, R.color.taxi_yellow)
                    routePolyline?.width = 12f
                    mapView.overlays.add(routePolyline)
                }
                routePolyline?.setPoints(routePoints)
            }
        } else {
            startMarker = Marker(mapView)
            startMarker?.position = currentGeoPoint
            startMarker?.title = "Départ"
            startMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(startMarker)
            routePoints.add(currentGeoPoint)
        }

        tripData.lastLocation = location
        tripData.totalFare = tripData.calculateFareMaroc(currentSpeed)
        mapView.controller.animateTo(currentGeoPoint)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.invalidate()

        // updateUI() est déjà appelé par le timer, pas besoin de le mettre ici
    }

    // MODIFIÉ 5: Logique de Jour/Nuit déplacée ici
    private fun updateUI() {
        // Format avec style 7 segments (zéros devant)
        tvTotalFare.text = String.format("%06.2f", tripData.totalFare)
        tvDistance.text = String.format("%05.2f", tripData.distanceInKm)

        val minutes = tripData.timeInSeconds / 60
        val seconds = tripData.timeInSeconds % 60
        tvTime.text = String.format("%02d:%02d", minutes, seconds)

        // Update speed
        tvSpeed.text = String.format("VITESSE: %.0f KM/H", currentSpeed)

        // --- LOGIQUE AJOUTÉE (DE updateRatesDisplay) ---
        // Calcule l'heure actuelle à chaque seconde
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 20 // 20h00 à 5h59
        val fareKm = if (isNight) "9.50" else "7.50"

        // Met à jour le texte des tarifs
        tvRates.text = "Base: 7 DH | ${if (isNight) "Nuit" else "Jour"}: $fareKm/km | Attente: 15/h"
        // --- FIN DE LA LOGIQUE AJOUTÉE ---

        // Update mode avec couleurs appropriées
        if (tripData.isWaiting) {
            tvTariffMode.text = "MODE: ATTENTE"
            tvTariffMode.setTextColor(ContextCompat.getColor(this, R.color.red))
            tvTariffMode.setShadowLayer(10f, 0f, 0f, ContextCompat.getColor(this, R.color.red))
        } else {
            // Utilise les variables 'isNight' que nous venons de calculer
            tvTariffMode.text = if (isNight) "MODE: NUIT" else "MODE: JOUR"
            tvTariffMode.setTextColor(ContextCompat.getColor(this, R.color.green))
            tvTariffMode.setShadowLayer(10f, 0f, 0f, ContextCompat.getColor(this, R.color.green))
        }
    }

    // SUPPRIMÉ : Cette fonction est maintenant intégrée dans updateUI()
    /*
    private fun updateRatesDisplay() {
        ...
    }
    */

    private fun showStopConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Terminer la course ?")
            .setMessage(
                "Distance: ${String.format("%.2f", tripData.distanceInKm)} km\n" +
                        "Temps: ${tripData.timeInMinutes} min\n" +
                        "Temps d'attente: ${tripData.waitingTimeMinutes} min\n\n" +
                        "Tarif total: ${String.format("%.2f", tripData.totalFare)} DH"
            )
            .setPositiveButton("OUI") { _, _ ->
                stopTrip()
            }
            .setNegativeButton("NON", null)
            .show()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        enableMyLocation()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "Permission refusée", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tripData.isActive) {
            stopTrip()
        }
        // Arrêter le timer quand l'activité est détruite
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // Assurez-vous que le timer est en cours si l'activité reprend
        timerHandler.removeCallbacks(timerRunnable) // Éviter les doublons
        timerHandler.post(timerRunnable) // Redémarrer
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        // Il est bon de stopper le timer quand l'app est en pause
        // timerHandler.removeCallbacks(timerRunnable) // Décommentez si vous voulez économiser la batterie
    }
}