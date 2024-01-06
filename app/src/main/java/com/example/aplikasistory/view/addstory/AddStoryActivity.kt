package com.example.aplikasistory.view.addstory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.aplikasistory.data.ResultState
import com.example.aplikasistory.databinding.ActivityAddStoryBinding
import com.example.aplikasistory.utils.getImageUri
import com.example.aplikasistory.utils.reduceFileImage
import com.example.aplikasistory.utils.uriToFile
import com.example.aplikasistory.view.ViewModelFactory
import com.example.aplikasistory.view.main.MainActivity
import com.example.aplikasistory.view.story.StoryViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class AddStoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStoryBinding

    private lateinit var storyViewModel: StoryViewModel


    private var currentImageUri: Uri? = null

    private var currentLocation: Location? = null

    private lateinit var token: String

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private var lat: Double? = null
    private var lon: Double? = null
    private val viewModel by viewModels<AddStoryViewModel> {
        ViewModelFactory.getInstance(this)
    }

    private var isLocationChecked = false

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            currentLocation = location
                        } else {
                            Log.e("UploadStory", "Failed to get location.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UploadStory", "Error: ${e.message}")
                    }
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = location
                        lat = currentLocation?.latitude
                        lon = currentLocation?.longitude
                    } else {
                        Log.e("UploadStory", "Failed to get location.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UploadStory", "Error: ${e.message}")
                }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocation()

        storyViewModel = ViewModelProvider(
            this,
            ViewModelFactory.getInstance(this)
        )[StoryViewModel::class.java]

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.uploadButton.setOnClickListener { uploadStory() }
        binding.location.setOnCheckedChangeListener { _, isChecked ->
            isLocationChecked = isChecked
        }

        token = intent.getStringExtra("TOKEN_KEY") ?: ""

        viewModel.uploadResult.observe(this) { result ->
            when (result) {
                is ResultState.Success -> {
                    showToast("Story uploaded successfully")
                    showLoading(false)

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                is ResultState.Error -> {
                    showToast("Error uploading story: ${result.error}")
                    showLoading(false)
                }

                is ResultState.Loading -> {
                    showLoading(true)
                }

                else -> {
                    showToast("Unexpected result state: $result")
                    showLoading(false)
                }
            }
        }
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        checkPermissionLoct()
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
        checkPermissionLoct()
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun uploadStory() {
        val description = binding.descriptionEditText.text.toString().trim()

        if (currentImageUri != null && description.isNotEmpty()) {
            val imageFile = uriToFile(currentImageUri!!, this).reduceFileImage()
            val requestBody = description.toRequestBody("text/plain".toMediaType())
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody =
                MultipartBody.Part.createFormData("photo", imageFile.name, requestImageFile)
            var latRequestBody = lat?.toString()?.toRequestBody("text/plain".toMediaType())
            var lonRequestBody = lon?.toString()?.toRequestBody("text/plain".toMediaType())
            if (isLocationChecked) {
                viewModel.uploadStory(token, multipartBody, requestBody, latRequestBody, lonRequestBody)
            }
            else {
                viewModel.uploadStory(token, multipartBody, requestBody, null, null)
            }
        } else {
            showToast("Please select a photo and enter description.")
            Log.e("UploadStory", "Invalid input: Image URI is null or description is empty")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        currentLocation = location
                    }
                    .addOnFailureListener { e ->
                        Log.e("Location", "Error: ${e.message}")
                    }
            } else {
                Toast.makeText(
                    this@AddStoryActivity,
                    "Location is not permitted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun checkPermissionLoct() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    currentLocation = location
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error: ${e.message}")
                }
        }

    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}