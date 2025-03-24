package com.williamfq.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface AlertApiService {
    @POST("alerts/send")
    suspend fun sendPanicAlert(@Body request: AlertRequest)
}
