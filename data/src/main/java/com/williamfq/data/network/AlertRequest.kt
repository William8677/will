package com.williamfq.data.network

data class AlertRequest(
    val message: String,
    val userId: String,
    val location: LocationRequest?
)

data class LocationRequest(
    val latitude: Double,
    val longitude: Double
)
