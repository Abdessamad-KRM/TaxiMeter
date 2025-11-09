package com.example.taximeterv1

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.taximeterv1.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import pub.devrel.easypermissions.EasyPermissions
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etLicenseType: TextInputEditText
    private lateinit var etExperience: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var cardChangePhoto: MaterialCardView

    private lateinit var sessionManager: SessionManager
    private var profileImageUri: Uri? = null

    // Lanceur pour la galerie
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                profileImageUri = result.data?.data
                if (profileImageUri != null) {

                    // MODIFIÉ : Utiliser Glide au lieu de setImageURI
                    Glide.with(this)
                        .load(profileImageUri) // Charger l'URI
                        .into(ivProfilePhoto) // L'afficher dans l'ImageView
                }
            }
        }

    // Lanceur pour la caméra
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                // MODIFIÉ : Utiliser Glide au lieu de setImageURI
                Glide.with(this)
                    .load(profileImageUri) // Charger l'URI de la caméra
                    .into(ivProfilePhoto) // L'afficher
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        sessionManager = SessionManager(this)

        // Initialisation des vues
        initViews()

        // Listeners
        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            handleRegistration()
        }

        cardChangePhoto.setOnClickListener {
            showImagePickDialog()
        }
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etAge = findViewById(R.id.etAge)
        etLicenseType = findViewById(R.id.etLicenseType)
        etExperience = findViewById(R.id.etExperience)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        cardChangePhoto = findViewById(R.id.cardChangePhoto)
    }

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
        // C'est la méthode la plus simple et elle fonctionne parfaitement avec Glide
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun takePicture() {
        // Crée un URI temporaire pour stocker l'image
        val file = File(externalCacheDir, "profile_pic_${System.currentTimeMillis()}.jpg")
        profileImageUri = FileProvider.getUriForFile(
            this,
            "com.example.taximeterv1.provider", // Assurez-vous d'ajouter un FileProvider dans votre Manifest
            file
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri)
        }
        cameraLauncher.launch(intent)
    }

    private fun handleRegistration() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val age = etAge.text.toString().trim()
        val license = etLicenseType.text.toString().trim()
        val experience = etExperience.text.toString().trim()
        val photoUriString = profileImageUri?.toString()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() ||
            age.isEmpty() || license.isEmpty() || experience.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        if (photoUriString == null) {
            Toast.makeText(this, "Veuillez choisir une photo de profil", Toast.LENGTH_SHORT).show()
            return
        }

        // Sauvegarde des données
        sessionManager.createLoginSession(
            email, password, fullName, age, license, experience, photoUriString
        )

        // Redirection vers la page principale
        Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    // Gérer le résultat des permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}