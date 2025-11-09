package com.example.taximeterv1.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DriverProfile", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val IS_LOGGED_IN = "isLoggedIn"
        const val KEY_EMAIL = "email"
        // J'utilise l'email comme identifiant unique,
        // mais vous pouvez ajouter un nom d'utilisateur si vous le voulez.
        const val KEY_PASSWORD = "password"
        const val KEY_FULL_NAME = "fullName"
        const val KEY_AGE = "age"
        const val KEY_LICENSE_TYPE = "licenseType"
        const val KEY_EXPERIENCE = "experience"
        const val KEY_PHOTO_URI = "photoUri"
    }

    /**
     * Crée une session d'inscription/connexion
     */
    fun createLoginSession(email: String, pass: String, name: String,
                           age: String, license: String, exp: String, photoUri: String?) {
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_PASSWORD, pass) // ATTENTION: Stocker un mot de passe en clair est DANGEREUX. C'est juste pour cet exemple.
        editor.putString(KEY_FULL_NAME, name)
        editor.putString(KEY_AGE, age)
        editor.putString(KEY_LICENSE_TYPE, license)
        editor.putString(KEY_EXPERIENCE, exp)
        editor.putString(KEY_PHOTO_URI, photoUri)
        editor.apply()
    }

    /**
     * Vérifie le statut de connexion
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    /**
     * Récupère le profil sauvegardé
     */
    fun getProfileDetails(): HashMap<String, String?> {
        val driver = HashMap<String, String?>()
        driver[KEY_EMAIL] = prefs.getString(KEY_EMAIL, null)
        driver[KEY_PASSWORD] = prefs.getString(KEY_PASSWORD, null)
        driver[KEY_FULL_NAME] = prefs.getString(KEY_FULL_NAME, null)
        driver[KEY_AGE] = prefs.getString(KEY_AGE, null)
        driver[KEY_LICENSE_TYPE] = prefs.getString(KEY_LICENSE_TYPE, null)
        driver[KEY_EXPERIENCE] = prefs.getString(KEY_EXPERIENCE, null)
        driver[KEY_PHOTO_URI] = prefs.getString(KEY_PHOTO_URI, null)
        return driver
    }
    fun updateProfileImage(photoUri: String?) {
        editor.putString(KEY_PHOTO_URI, photoUri)
        editor.apply()
    }

    /**
     * Gère la déconnexion
     */
    fun logoutUser() {
        editor.clear()
        editor.apply()

        // Vous pouvez rediriger vers LoginActivity ici si vous le souhaitez
    }
}