package com.example.aplikasistory.view.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.aplikasistory.data.model.UserRepository
import com.example.aplikasistory.data.model.response.ErrorResponse
import com.example.aplikasistory.data.model.response.ListStoryItem

class StoryViewModel(private val repository: UserRepository): ViewModel() {


    private val _error = MutableLiveData<ErrorResponse>()
    val error: LiveData<ErrorResponse> = _error

    val story: LiveData<PagingData<ListStoryItem>> = repository.getStory().cachedIn(viewModelScope)

}
