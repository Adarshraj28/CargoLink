package com.cargolink.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
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
