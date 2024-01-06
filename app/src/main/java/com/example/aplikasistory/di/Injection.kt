package com.example.aplikasistory.di

import android.content.Context
import com.example.aplikasistory.data.database.StoryDatabase
import com.example.aplikasistory.data.model.UserRepository
import com.example.aplikasistory.data.pref.UserPreference
import com.example.aplikasistory.data.pref.dataStore

object Injection {
    fun provideRepository(context: Context): UserRepository {
        val pref = UserPreference.getInstance(context.dataStore)
        val storyDatabase = StoryDatabase.getDatabase(context)
        return UserRepository.getInstance(pref, storyDatabase)
    }
}