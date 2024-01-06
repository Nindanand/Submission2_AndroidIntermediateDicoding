package com.example.aplikasistory.view.addstory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasistory.data.ResultState
import com.example.aplikasistory.data.model.UserRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class AddStoryViewModel(private val repository: UserRepository): ViewModel() {
    private val _navigateToMain = MutableLiveData<Boolean>()
    val navigateToMain: LiveData<Boolean>
        get() = _navigateToMain

    fun onStoryUploaded() {
        _navigateToMain.value = true
    }

    private val _uploadResult = MutableLiveData<ResultState<Unit>>()
    val uploadResult: LiveData<ResultState<Unit>> = _uploadResult

    fun uploadStory(token: String, file: MultipartBody.Part, desc: RequestBody, lat: RequestBody?, lon: RequestBody?) {
        viewModelScope.launch {
            try {
                repository.uploadStory(token, file, desc, lat, lon)
                _uploadResult.postValue(ResultState.Success(Unit))
                onStoryUploaded()

            } catch (e: Exception) {
                _uploadResult.postValue(ResultState.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }
}



