package com.example.brave_sailors.domain.use_case

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

@SuppressLint("MissingPermission")
fun detectCountryFromLocation(context: Context, onCountryFound: (String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cancelTokenSource = CancellationTokenSource()

    fun handleFailure() {
        onCountryFound("IT")
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            decodeAndNotify(context, location, onCountryFound) { handleFailure() }
        } else {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            fusedLocationClient.getCurrentLocation(priority, cancelTokenSource.token)
                .addOnSuccessListener { newLocation ->
                    if (newLocation != null) {
                        decodeAndNotify(context, newLocation, onCountryFound) { handleFailure() }
                    } else {
                        handleFailure()
                    }
                }
                .addOnFailureListener { handleFailure() }
        }
    }.addOnFailureListener { handleFailure() }
}

fun decodeAndNotify(context: Context, location: Location, onCountryFound: (String) -> Unit, onError: () -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                if (addresses.isNotEmpty() && addresses[0].countryCode != null) {
                    onCountryFound(addresses[0].countryCode.uppercase())
                } else {
                    onError()
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty() && addresses[0].countryCode != null) {
                onCountryFound(addresses[0].countryCode.uppercase())
            } else {
                onError()
            }
        }
    } catch (e: Exception) {
        onError()
    }
}