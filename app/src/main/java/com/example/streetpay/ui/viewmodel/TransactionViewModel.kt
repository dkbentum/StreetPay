package com.example.streetpay.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.streetpay.data.AppDatabase
import com.example.streetpay.data.TransactionEntity
import com.example.streetpay.usb.UsbSerialManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.transactionDao()
    private val usbManager = UsbSerialManager(application)
    private val prefs = application.getSharedPreferences("streetpay_settings", Context.MODE_PRIVATE)

    private val _selectedTransactionId = MutableStateFlow<String?>(null)
    val selectedTransactionId = _selectedTransactionId.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedNetworks = MutableStateFlow(setOf("MTN", "Telecel", "AirtelTigo"))
    val selectedNetworks = _selectedNetworks.asStateFlow()

    private val _isGestureEnabled = MutableStateFlow(prefs.getBoolean("gesture_enabled", true))
    val isGestureEnabled = _isGestureEnabled.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    private val _notificationSoundUri = MutableStateFlow(prefs.getString("sound_uri", null))
    val notificationSoundUri = _notificationSoundUri.asStateFlow()

    val notificationSoundName = _notificationSoundUri.map { uriString ->
        if (uriString == null) "Default"
        else {
            try {
                val ringtone = RingtoneManager.getRingtone(application, android.net.Uri.parse(uriString))
                ringtone.getTitle(application) ?: "Unknown"
            } catch (e: Exception) {
                "Default"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "Default")

    val isUsbConnected = usbManager.isConnected

    val transactions = combine(_selectedDate, _selectedNetworks) { dateMillis, networks ->
        dateMillis to networks
    }.flatMapLatest { (dateMillis, networks) ->
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis - 1
        dao.getTransactionsByDateRange(startOfDay, endOfDay)
            .map { list -> list.filter { it.network in networks } }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            usbManager.commands.collect { command ->
                handleExternalCommand(command)
            }
        }
    }

    fun setDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
        _selectedTransactionId.value = null
    }

    fun toggleNetwork(network: String) {
        val current = _selectedNetworks.value.toMutableSet()
        if (current.contains(network)) {
            if (current.size > 1) { // Keep at least one selected
                current.remove(network)
            }
        } else {
            current.add(network)
        }
        _selectedNetworks.value = current
    }

    fun toggleGestureEnabled() {
        _isGestureEnabled.value = !_isGestureEnabled.value
        prefs.edit().putBoolean("gesture_enabled", _isGestureEnabled.value).apply()
    }

    fun toggleSoundEnabled() {
        _isSoundEnabled.value = !_isSoundEnabled.value
        prefs.edit().putBoolean("sound_enabled", _isSoundEnabled.value).apply()
    }

    fun setNotificationSound(uriString: String?) {
        _notificationSoundUri.value = uriString
        prefs.edit().putString("sound_uri", uriString).apply()
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun toggleSeen(transaction: TransactionEntity) {
        viewModelScope.launch {
            dao.update(transaction.copy(isSeen = !transaction.isSeen))
        }
    }

    fun setSeen(transaction: TransactionEntity, isSeen: Boolean) {
        viewModelScope.launch {
            dao.update(transaction.copy(isSeen = isSeen))
        }
    }

    fun handleExternalCommand(command: String) {
        val currentList = transactions.value
        val currentIndex = currentList.indexOfFirst { it.transactionId == _selectedTransactionId.value }

        when (command.uppercase()) {
            "NEXT" -> {
                if (currentIndex < currentList.size - 1) {
                    _selectedTransactionId.value = currentList[currentIndex + 1].transactionId
                } else if (currentList.isNotEmpty()) {
                    _selectedTransactionId.value = currentList[0].transactionId
                }
            }
            "PREV" -> {
                if (currentIndex > 0) {
                    _selectedTransactionId.value = currentList[currentIndex - 1].transactionId
                } else if (currentList.isNotEmpty()) {
                    _selectedTransactionId.value = currentList.last().transactionId
                }
            }
            "SEEN" -> {
                currentList.find { it.transactionId == _selectedTransactionId.value }?.let {
                    setSeen(it, true)
                }
            }
            "UNSEEN" -> {
                currentList.find { it.transactionId == _selectedTransactionId.value }?.let {
                    setSeen(it, false)
                }
            }
        }
    }

    fun connectUsb() = usbManager.connect()
    fun disconnectUsb() = usbManager.disconnect()

    override fun onCleared() {
        super.onCleared()
        usbManager.unregisterReceiver()
    }
}
