package com.example.aplikasistory.view.maps

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplikasistory.data.model.response.ErrorResponse
import com.example.aplikasistory.data.model.response.ListStoryItem
import com.example.aplikasistory.data.model.response.StoryResponse
import com.example.aplikasistory.data.retrofit.ApiConfig
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsViewModel: ViewModel() {

    private val _listLoctUser = MutableLiveData<List<ListStoryItem>>()
    val listLoctUser: LiveData<List<ListStoryItem>> = _listLoctUser

    private val _error = MutableLiveData<ErrorResponse>()
    val error: LiveData<ErrorResponse> = _error

    fun getLocation(token: String) {
        val apiService = ApiConfig.getApiService(token)
        val client = apiService.getStoriesWithLocation()
        client.enqueue(object : Callback<StoryResponse> {
            override fun onResponse(call: Call<StoryResponse>, response: Response<StoryResponse>) {
                if (response.isSuccessful) {
                    response.body()?.listStory?.let {
                        _listLoctUser.postValue(it)
                    }
                } else {
                    response.errorBody()?.let {
                        val jsonInString = response.errorBody()?.string()
                        val errorBody = Gson().fromJson(jsonInString, ErrorResponse::class.java)
                        val errorMessage = errorBody?.message ?: "Unknown error occurred"
                        _error.postValue(ErrorResponse(message = errorMessage))
                    }
                }
            }

            override fun onFailure(call: Call<StoryResponse>, t: Throwable) {
                Log.e("API Call", "Failed: ${t.message}")
                _error.postValue(ErrorResponse(message = t.message))
            }
        })
    }

}