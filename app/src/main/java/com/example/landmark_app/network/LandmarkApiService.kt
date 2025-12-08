package com.example.landmark_app.network

import com.example.landmark_app.model.Landmark
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface LandmarkApiService {

    @GET("api.php")
    suspend fun getLandmarks(): Response<List<Landmark>>

    @Multipart
    @POST("api.php")
    suspend fun createLandmark(
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<Landmark>

    @Multipart
    @POST("api.php") // Using POST with a hidden method field for PUT
    suspend fun updateLandmark(
        @Part("id") id: RequestBody,
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part? // Image is optional
    ): Response<Landmark>

    @FormUrlEncoded
    @POST("api.php") // Using POST with a hidden method field for DELETE
    suspend fun deleteLandmark(@Field("id") id: String, @Field("_method") method: String = "DELETE"): Response<Unit>
}