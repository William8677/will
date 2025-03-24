package com.williamfq.xhat.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location as AndroidLocation
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.williamfq.domain.models.Location as DomainLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }


    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val location = getLastLocation() ?: return null
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            } else {


                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            }

            addresses?.firstOrNull()?.let { address ->
                LocationData(
                    country = address.countryName ?: "",
                    state = address.adminArea ?: "",
                    city = address.locality ?: "",
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
        } catch (e: Exception) {

            println("Error obteniendo ubicación: ${e.message}")
            null
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startLocationUpdates(callback: (DomainLocation) -> Unit) {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Actualizar cada 10 segundos
            fastestInterval = 5000 // Intervalo más rápido
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    callback(DomainLocation(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }


    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private suspend fun getLastLocation(): DomainLocation? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: AndroidLocation? ->
                    if (location != null) {
                        val domainLocation = DomainLocation(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        continuation.resume(domainLocation)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(Exception("Fallo al obtener ubicación: ${e.message}"))
                }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }
}


data class LocationData(
    val country: String,
    val state: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)