package com.example.landmark_app.model

data class Landmark(
    val id: Int,
    val title: String,
    val latitude: String?,     // nullable
    val longitude: String?,    // nullable
    val image: String
) {
    val fullImageUrl: String
        get() = "https://labs.anontech.info/cse489/t3/${image}"
}