package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var chatListener: ListenerRegistration? = null

    fun listenToChat(shipmentId: String) {
        chatListener?.remove()
        chatListener = FirestoreManager.listenToChat(shipmentId) { msgs ->
            _messages.value = msgs
        }
    }

    fun sendMessage(shipmentId: String, message: String) {
        if (message.isNotBlank()) {
            FirestoreManager.sendMessage(shipmentId, message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
    }
}
