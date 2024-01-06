package com.example.aplikasistory.data.retrofit

import com.example.aplikasistory.data.model.response.LoginResponse
import com.example.aplikasistory.data.model.response.RegisterResponse
import com.example.aplikasistory.data.model.response.StoryResponse
import com.example.aplikasistory.data.model.response.StoryUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @FormUrlEncoded
    @POST("register")
    fun register(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @Multipart
    @POST("stories")
    fun uploadStory(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody,
        @Part("lat") lat: RequestBody?,
        @Part("lon") lon: RequestBody?,
    ): Call<StoryUploadResponse>

    @GET("stories")
    fun getStoriesWithLocation(
        @Query("location") location : Int = 1,
    ): Call<StoryResponse>

    @GET("stories")
    fun getStories(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Call<StoryResponse>
}