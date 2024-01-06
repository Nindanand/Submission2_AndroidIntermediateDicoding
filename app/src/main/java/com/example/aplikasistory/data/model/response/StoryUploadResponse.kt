package com.example.aplikasistory.data.model.response

import com.google.gson.annotations.SerializedName

class StoryUploadResponse (
    @field:SerializedName("error")
    val error: Boolean,

    @field:SerializedName("message")
    val message: String
)