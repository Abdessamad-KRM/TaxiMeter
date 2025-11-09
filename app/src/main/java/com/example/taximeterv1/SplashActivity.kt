package com.example.taximeterv1

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.taximeterv1.utils.SessionManager // Importation du SessionManager

class SplashActivity : AppCompatActivity() {

    private companion object {
        const val SPLASH_DELAY = 2000L // 2 seconds
    }

    // Ajout du SessionManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialiser le SessionManager
        sessionManager = SessionManager(this)

        // Logique de redirection modifiée
        Handler(Looper.getMainLooper()).postDelayed({

            // Vérifier si l'utilisateur est connecté
            if (sessionManager.isLoggedIn()) {
                // L'utilisateur est connecté -> Aller à MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // L'utilisateur n'est pas connecté -> Aller à LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
            }

            finish() // Terminer la Splash Activity
        }, SPLASH_DELAY)
    }
}