package com.example.data.remote

import com.example.data.model.RequestedTripEntity

class RequestedTripRemoteRepository(
    private val api: ApiService = ApiClient.api
) {
    suspend fun getOpenTrips(): Result<List<RequestedTripDto>> = runCatching {
        val response = api.getRequestedTrips()
        if (!response.isSuccessful) {
            error("GET_REQUESTS_HTTP_${response.code()}")
        }
        response.body()?.data ?: emptyList()
    }

    suspend fun createTrip(trip: RequestedTripEntity, backendUserId: Long): Result<RequestedTripDto> = runCatching {
        val response = api.createRequestedTrip(trip.toCreateRequest(backendUserId))
        if (!response.isSuccessful) {
            error("CREATE_REQUEST_HTTP_${response.code()}")
        }
        response.body()?.data ?: error("CREATE_REQUEST_EMPTY")
    }

    suspend fun acceptTrip(
        requestId: String,
        driverId: Long,
        driverName: String
    ): Result<RequestedTripDto> = runCatching {
        val response = api.acceptRequestedTrip(
            requestId,
            AcceptRequestedTripRequest(driverId, driverName)
        )
        if (!response.isSuccessful) {
            error("ACCEPT_REQUEST_HTTP_${response.code()}")
        }
        response.body()?.data ?: error("ACCEPT_REQUEST_EMPTY")
    }
}
