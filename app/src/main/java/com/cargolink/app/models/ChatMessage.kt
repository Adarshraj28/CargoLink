package com.cargolink.app.models

data class ChatMessage(
    val id: String = "",
    val shipmentId: String = "",
    val senderEmail: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
