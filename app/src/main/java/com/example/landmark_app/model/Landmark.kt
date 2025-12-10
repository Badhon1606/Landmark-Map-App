package com.example.landmark_app.model

import com.google.gson.annotations.SerializedName

data class Landmark(
    val id: Int,
    val title: String,
    @SerializedName("lat")
    val latitude: String?,
    @SerializedName("lon")
    val longitude: String?,
    @SerializedName("image")
    val image: String
) {
    val fullImageUrl: String
        get() = "https://labs.anontech.info/cse489/t3/$image"
}