package com.example.aplikasistory.view.story

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.aplikasistory.data.model.response.ListStoryItem
import com.example.aplikasistory.databinding.ActivityDetailStoryBinding

class DetailStoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listStoryItem = intent.getParcelableExtra<ListStoryItem>(EXTRA_LIST_STORY_ITEM)

        listStoryItem?.let {
            binding.tvName.text = it.name
            binding.tvStory.text = it.description

            Glide.with(this)
                .load(it.photoUrl)
                .into(binding.ivUser)
        }
    }

    companion object {
        const val EXTRA_LIST_STORY_ITEM = "extra_list_story_item"
    }
}
