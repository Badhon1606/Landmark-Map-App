package com.example.landmark_app.network

import com.example.landmark_app.model.Landmark
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface LandmarkApi {

    @GET("api.php")
    suspend fun getLandmarks(): List<Landmark>

    @Multipart
    @POST("api.php")
    suspend fun createLandmark(
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<Unit>

    @Multipart
    @POST("api.php")
    suspend fun updateLandmarkWithImage(
        @Part("_method") method: RequestBody,
        @Part("id") id: RequestBody,
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api.php")
    suspend fun updateLandmarkNoImage(
        @Field("_method") method: String = "PUT",
        @Field("id") id: Int,
        @Field("title") title: String,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api.php")
    suspend fun deleteLandmark(
        @Field("_method") method: String = "DELETE",
        @Field("id") id: Int
    ): Response<Unit>
}
