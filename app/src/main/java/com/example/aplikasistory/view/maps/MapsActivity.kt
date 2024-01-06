package com.example.aplikasistory.view.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.aplikasistory.R
import com.example.aplikasistory.data.model.response.ListStoryItem
import com.example.aplikasistory.data.pref.UserPreference
import com.example.aplikasistory.data.pref.dataStore
import com.example.aplikasistory.databinding.ActivityMapsBinding
import com.example.aplikasistory.view.ViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var viewModel: MapsViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var token: String
    private val boundsBuilder = LatLngBounds.Builder()
    private var isGPSDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this, ViewModelFactory.getInstance(this)
        )[MapsViewModel::class.java]

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermissionAndInitMap()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                checkLocationPermissionAndInitMap()
            }

            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> {
                checkLocationPermissionAndInitMap()
            }
        }
    }

    private fun checkLocationPermissionAndInitMap() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) && checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (isGPSEnabled()) {
                initMap()
            } else if (!isGPSDialogShown) {
                showGPSDisabledDialog()
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initMap() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) && checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location == null) {
                    Toast.makeText(
                        this@MapsActivity,
                        "Failed to get location. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            loadDataAndAddMarkers()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadDataAndAddMarkers() {
        val userPreference = UserPreference.getInstance(this.dataStore)
        lifecycleScope.launch {
            val user = userPreference.getSession().first()
            token = user.token
            Log.d("MapsActivity", "Token dari UserPreference: $token")
            viewModel.getLocation(token)
        }
    }

    private fun showGPSDisabledDialog() {
        isGPSDialogShown = true
        AlertDialog.Builder(this).setMessage("GPS is disabled. Enable it in settings.")
            .setPositiveButton("OK") { _, _ ->
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isIndoorLevelPickerEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        getMyLocation()

        val token = intent.getStringExtra("TOKEN_EXTRA") ?: ""

        viewModel.getLocation(token)

        viewModel.listLoctUser.observe(this) { user ->
            user?.let {
                addManyMarker(user)
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getMyLocation() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) && checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (isGPSEnabled()) {
                initMap()
            } else if (!isGPSDialogShown) {
                showGPSDisabledDialog()
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun addManyMarker(listStoryItems: List<ListStoryItem>) {
        listStoryItems.forEach { storyItem ->
            storyItem.lat.let { lat ->
                storyItem.lon.let { lon ->
                    val latLng = LatLng(lat, lon)
                    mMap.addMarker(
                        MarkerOptions().position(latLng).title(storyItem.name)
                            .snippet(storyItem.description)
                    )
                    boundsBuilder.include(latLng)
                }
            }
        }

        val bounds: LatLngBounds = boundsBuilder.build()
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                300
            )
        )
    }
}
