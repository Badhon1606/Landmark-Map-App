package com.example.landmark_app.network

import com.example.landmark_app.model.Landmark
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
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
    ): Response<ResponseBody>

    @Multipart
    @POST("api.php")
    suspend fun updateLandmarkWithImage(
        @Part("_method") method: RequestBody,
        @Part("id") id: RequestBody,
        @Part("title") title: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("api.php")
    suspend fun updateLandmarkNoImage(
        @Field("_method") method: String = "PUT",
        @Field("id") id: Int,
        @Field("title") title: String,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double
    ): Response<ResponseBody>

    @DELETE("api.php")
    suspend fun deleteLandmark(
        @Query("id") id: Int,
        @Query("method") method: String = "DELETE"
    ): Response<ResponseBody>
}
