package com.example.landmark_app.network

import com.example.landmark_app.model.Landmark
import retrofit2.Response
import retrofit2.http.*

interface LandmarkApiService {

    @GET("api.php")
    suspend fun getLandmarks(): List<Landmark>

    // CREATE
    @Multipart
    @POST("api.php")
    suspend fun createLandmark(
        @Part("title") title: okhttp3.RequestBody,
        @Part("lat") lat: okhttp3.RequestBody,
        @Part("lon") lon: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part?
    ): Response<Landmark>

    // UPDATE (PUT using POST)
    @Multipart
    @POST("api.php")
    suspend fun updateLandmark(
        @Part("_method") method: okhttp3.RequestBody,
        @Part("id") id: okhttp3.RequestBody,
        @Part("title") title: okhttp3.RequestBody,
        @Part("lat") lat: okhttp3.RequestBody,
        @Part("lon") lon: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part?
    ): Response<Landmark>

    @FormUrlEncoded
    @POST("api.php")
    suspend fun updateLandmarkNoImage(
        @Field("_method") method: String = "PUT",
        @Field("id") id: Int,
        @Field("title") title: String,
        @Field("lat") latitude: String,
        @Field("lon") longitude: String
    ): Response<Unit>

    // DELETE (using POST + _method=DELETE)
    @FormUrlEncoded
    @POST("api.php")
    suspend fun deleteLandmark(
        @Field("_method") method: String = "DELETE",
        @Field("id") id: Int
    ): Response<Unit>
}