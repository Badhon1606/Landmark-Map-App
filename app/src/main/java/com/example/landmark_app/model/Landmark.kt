package com.example.landmark_app.model

import com.google.gson.annotations.SerializedName

data class Landmark(
    val id: Int,
    val title: String,
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("image") val imageUrl: String
)