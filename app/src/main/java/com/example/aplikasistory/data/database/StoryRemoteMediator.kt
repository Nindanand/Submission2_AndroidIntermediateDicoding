package com.example.aplikasistory.data.database

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.aplikasistory.data.model.response.ListStoryItem
import com.example.aplikasistory.data.retrofit.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
class StoryRemoteMediator(
    private val database: StoryDatabase,
    private val apiService: ApiService
) : RemoteMediator<Int, ListStoryItem>() {
    private companion object {
        const val INITIAL_PAGE_INDEX = 1
    }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ListStoryItem>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: INITIAL_PAGE_INDEX
            }

            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey
            }

            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val callapi = apiService.getStories(page, state.config.pageSize)
                val response = callapi.execute()

                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData != null) {
                        database.withTransaction {
                            if (loadType == LoadType.REFRESH) {
                                database.remoteKeysDao().deleteRemoteKeys()
                                database.storyDao().deleteAll()
                            }

                            val prevKey = if (page == 1) null else page - 1
                            val nextKey = if (responseData.listStory.isEmpty()) null else page + 1

                            val remoteKeys = responseData.listStory.map {
                                RemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey)
                            }
                            database.remoteKeysDao().insertAll(remoteKeys)

                            val listStoryItems = responseData.listStory.map {
                                ListStoryItem(
                                    id = it.id,
                                    photoUrl = it.photoUrl,
                                    createdAt = it.createdAt,
                                    name = it.name,
                                    description = it.description,
                                    lon = it.lon,
                                    lat = it.lat
                                )
                            }
                            database.storyDao().insertStory(listStoryItems)
                        }

                        MediatorResult.Success(endOfPaginationReached = responseData.listStory.isEmpty())
                    } else {
                        Log.e("StoryRemoteMediator", "Error loading data: Response body is null")
                        MediatorResult.Error(Exception("Error loading data: Response body is null"))
                    }
                } else {
                    Log.e("StoryRemoteMediator", "Error loading data: ${response.message()}")
                    MediatorResult.Error(Exception("Error loading data: ${response.message()}"))
                }
            } catch (exception: Exception) {
                Log.e("StoryRemoteMediator", "Error loading data: ${exception.message}")
                MediatorResult.Error(exception)
            }
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ListStoryItem>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { data ->
            database.remoteKeysDao().getRemoteKeysId(data.id)
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, ListStoryItem>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { data ->
            database.remoteKeysDao().getRemoteKeysId(data.id)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, ListStoryItem>
    ): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                database.remoteKeysDao().getRemoteKeysId(id)
            }
        }
    }
}