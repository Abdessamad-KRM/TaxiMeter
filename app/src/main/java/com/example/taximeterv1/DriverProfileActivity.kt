package com.example.taximeterv1

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide // IMPORTATION DE GLIDE
import com.example.taximeterv1.utils.QRCodeGenerator
import com.example.taximeterv1.utils.SessionManager
import com.google.android.material.button.MaterialButton
import pub.devrel.easypermissions.EasyPermissions
import java.io.File

class DriverProfileActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var btnBack: ImageButton
    private lateinit var ivDriverPhoto: ImageView
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverLicense: TextView
    private lateinit var tvDriverAge: TextView
    private lateinit var tvLicenseType: TextView
    private lateinit var tvExperience: TextView
    private lateinit var ivQRCode: ImageView
    private lateinit var btnShareProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private lateinit var sessionManager: SessionManager
    private var driverData: HashMap<String, String?> = HashMap()

    private var profileImageUri: Uri? = null

    // Lanceur pour la galerie (MODIFIÉ POUR GLIDE)
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                profileImageUri = result.data?.data
                if (profileImageUri != null) {

                    // Mettre à jour avec Glide
                    Glide.with(this).load(profileImageUri).into(ivDriverPhoto)

                    // Sauvegarder le nouvel URI
                    sessionManager.updateProfileImage(profileImageUri.toString())
                    ajusterPaddingImage()
                }
            }
        }

    // Lanceur pour la caméra (MODIFIÉ POUR GLIDE)
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                // Mettre à jour avec Glide
                Glide.with(this).load(profileImageUri).into(ivDriverPhoto)

                // Sauvegarder le nouvel URI
                sessionManager.updateProfileImage(profileImageUri.toString())
                ajusterPaddingImage()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_profile)

        sessionManager = SessionManager(this)

        initViews()
        loadProfileData() // Va utiliser Glide
        setupListeners()
        generateQRCode()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivDriverPhoto = findViewById(R.id.ivDriverPhoto)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvDriverLicense = findViewById(R.id.tvDriverLicense)
        tvDriverAge = findViewById(R.id.tvDriverAge)
        tvLicenseType = findViewById(R.id.tvLicenseType)
        tvExperience = findViewById(R.id.tvExperience)
        ivQRCode = findViewById(R.id.ivQRCode)
        btnShareProfile = findViewById(R.id.btnShareProfile)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun loadProfileData() {
        driverData = sessionManager.getProfileDetails()

        val fullName = driverData[SessionManager.KEY_FULL_NAME]
        val age = driverData[SessionManager.KEY_AGE]
        val license = driverData[SessionManager.KEY_LICENSE_TYPE]
        val experience = driverData[SessionManager.KEY_EXPERIENCE]
        val photoUriString = driverData[SessionManager.KEY_PHOTO_URI]

        tvDriverName.text = fullName
        tvDriverLicense.text = license
        tvDriverAge.text = "$age ans"
        tvLicenseType.text = license
        tvExperience.text = "$experience ans"

        // --- CORRECTION AVEC GLIDE ---
        // Remplace tout le bloc try/catch
        Glide.with(this)
            .load(photoUriString) // Charge l'URI sauvegardé (String ou Uri)
            .placeholder(R.drawable.ic_person) // Icône pendant le chargement
            .error(R.drawable.ic_person) // Icône si l'URI est mort ou illisible
            .into(ivDriverPhoto)

        ajusterPaddingImage()
    }

    private fun ajusterPaddingImage() {
        // Cette fonction n'est utile que si l'image est valide
        val photoUriString = driverData[SessionManager.KEY_PHOTO_URI]
        if (!photoUriString.isNullOrEmpty()) {
            ivDriverPhoto.setPadding(0, 0, 0, 0)
            ivDriverPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        btnShareProfile.setOnClickListener {
            shareProfile()
        }
        ivDriverPhoto.setOnClickListener {
            showImagePickDialog()
        }
        btnLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        AlertDialog.Builder(this)
            .setTitle("Se déconnecter")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("OUI") { _, _ ->
                sessionManager.logoutUser()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("NON", null)
            .show()
    }

    private fun getProfileShareString(): String {
        return """
            Nom: ${driverData[SessionManager.KEY_FULL_NAME]}
            Age: ${driverData[SessionManager.KEY_AGE]} ans
            Permis: ${driverData[SessionManager.KEY_LICENSE_TYPE]}
            Expérience: ${driverData[SessionManager.KEY_EXPERIENCE]} ans
        """.trimIndent()
    }

    private fun generateQRCode() {
        val qrData = getProfileShareString()
        val qrBitmap = QRCodeGenerator.generateQRCode(qrData, 400, 400)
        qrBitmap?.let {
            ivQRCode.setImageBitmap(it)
        }
    }

    private fun shareProfile() {
        val shareText = """
            ${getString(R.string.driver_profile)}
            
            ${getProfileShareString()}
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_profile)))
    }

    // --- Fonctions de sélection d'image (maintenant simplifiées) ---

    private fun showImagePickDialog() {
        val options = arrayOf("Prendre une photo", "Choisir dans la galerie", "Annuler")
        AlertDialog.Builder(this)
            .setTitle("Changer la photo de profil")
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "Prendre une photo" -> checkCameraPermission()
                    "Choisir dans la galerie" -> pickFromGallery()
                    "Annuler" -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            takePicture()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "L'application a besoin du la caméra pour prendre une photo.",
                101,
                Manifest.permission.CAMERA
            )
        }
    }

    private fun pickFromGallery() {
        // SIMPLIFIÉ : ACTION_PICK est parfait pour Glide
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun takePicture() {
        val file = File(externalCacheDir, "profile_pic_${System.currentTimeMillis()}.jpg")
        profileImageUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri)
        }
        cameraLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == 101) {
            takePicture()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        // (Optionnel) Gérer le refus de permission
    }
}