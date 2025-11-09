package com.example.taximeterv1.models

data class DriverProfile(
    val firstName: String = "Mohamed",
    val lastName: String = "Alami",
    val age: Int = 35,
    val licenseType: String = "Permis B",
    val experienceYears: Int = 10,
    val phoneNumber: String = "+212 6 XX XX XX XX",
    val vehicleNumber: String = "A-12345"
) {
    val fullName: String
        get() = "$firstName $lastName"

    fun toQRString(): String {
        return """
            Chauffeur de Taxi
            Nom: $fullName
            Âge: $age ans
            Permis: $licenseType
            Expérience: $experienceYears ans
            Téléphone: $phoneNumber
            Véhicule: $vehicleNumber
        """.trimIndent()
    }
}