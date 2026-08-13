package com.example.data.remote

import com.example.data.model.RequestedTripEntity

fun RequestedTripDto.toEntity(): RequestedTripEntity = RequestedTripEntity(
    id = id,
    userId = userId.toString(),
    userName = userName ?: "راكب",
    userPhone = userPhone ?: "",
    startCity = startCity,
    endCity = endCity,
    departureDate = departureDate,
    departureTime = departureTime,
    menCount = menCount,
    womenCount = womenCount,
    childrenCount = childrenCount,
    status = status,
    acceptedByDriverId = acceptedByDriverId?.toString(),
    acceptedByDriverName = acceptedByDriverName,
    createdAt = createdAt * 1000
)

fun RequestedTripEntity.toCreateRequest(backendUserId: Long): CreateRequestedTripRequest =
    CreateRequestedTripRequest(
        userId = backendUserId,
        startCity = startCity,
        endCity = endCity,
        departureDate = departureDate,
        departureTime = departureTime,
        menCount = menCount,
        womenCount = womenCount,
        childrenCount = childrenCount
    )
