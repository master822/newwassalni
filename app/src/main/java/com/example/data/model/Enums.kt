package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String) {
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe")
}

enum class RideStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class TransactionType {
    TOP_UP,
    DEDUCTION,
    REFUND
}

enum class NotificationType {
    APPROVAL,
    DEDUCTION,
    BOOKING,
    REMINDER,
    SYSTEM
}
