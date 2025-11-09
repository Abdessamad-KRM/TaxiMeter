package com.example.taximeterv1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taximeterv1.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmailUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Si déjà connecté, aller à MainActivity
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return // Important
        }

        initViews()

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            handleLogin()
        }
    }

    private fun initViews() {
        etEmailUsername = findViewById(R.id.etEmailUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
    }

    private fun handleLogin() {
        val emailOrUser = etEmailUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (emailOrUser.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        // Récupérer les données sauvegardées
        val savedProfile = sessionManager.getProfileDetails()
        val savedEmail = savedProfile[SessionManager.KEY_EMAIL]
        val savedPassword = savedProfile[SessionManager.KEY_PASSWORD]

        // Vérification (très basique)
        if (emailOrUser == savedEmail && password == savedPassword) {
            // Mettre à jour le statut de connexion (au cas où il se serait déconnecté)
            sessionManager.createLoginSession(
                savedEmail!!, savedPassword!!,
                savedProfile[SessionManager.KEY_FULL_NAME]!!,
                savedProfile[SessionManager.KEY_AGE]!!,
                savedProfile[SessionManager.KEY_LICENSE_TYPE]!!,
                savedProfile[SessionManager.KEY_EXPERIENCE]!!,
                savedProfile[SessionManager.KEY_PHOTO_URI]
            )

            // Rediriger vers MainActivity
            Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show()
        }
    }
}