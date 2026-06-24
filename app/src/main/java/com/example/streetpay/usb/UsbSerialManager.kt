package com.example.streetpay.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbSerialManager(private val context: Context) : SerialInputOutputManager.Listener {
    private val ACTION_USB_PERMISSION = "com.example.streetpay.USB_PERMISSION"
    
    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val commands = _commands.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var usbIoManager: SerialInputOutputManager? = null
    private var usbPort: UsbSerialPort? = null
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    private val readBuffer = StringBuilder()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("UsbSerialManager", "Received Broadcast: ${intent.action}")
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            Log.d("UsbSerialManager", "Permission granted for device")
                            device?.apply { connect() }
                        } else {
                            Log.d("UsbSerialManager", "Permission denied for device")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d("UsbSerialManager", "USB Device Attached")
                    connect()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d("UsbSerialManager", "USB Device Detached")
                    disconnect()
                }
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(usbReceiver, filter, flags)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }

        // Try to connect immediately in case the device is already plugged in
        connect()
    }

    fun connect() {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            _isConnected.value = false
            return
        }

        val driver = availableDrivers[0]
        val device = driver.device

        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
            return
        }

        val connection = usbManager.openDevice(device) ?: return
        val port = driver.ports[0]

        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            
            usbPort = port
            usbIoManager = SerialInputOutputManager(port, this)
            usbIoManager?.start()
            _isConnected.value = true
            Log.d("UsbSerialManager", "Connected to USB Serial")
        } catch (e: Exception) {
            Log.e("UsbSerialManager", "Connection failed", e)
            _isConnected.value = false
        }
    }

    fun disconnect() {
        usbIoManager?.stop()
        usbIoManager = null
        try {
            usbPort?.close()
        } catch (e: Exception) {
            Log.e("UsbSerialManager", "Error closing port", e)
        }
        usbPort = null
        _isConnected.value = false
    }

    /**
     * Call this from Activity.onDestroy to clean up the receiver
     */
    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e("UsbSerialManager", "Error unregistering receiver", e)
        }
    }

    override fun onNewData(data: ByteArray) {
        val incoming = String(data)
        readBuffer.append(incoming)
        
        // Process lines
        while (readBuffer.contains("\n")) {
            val newlineIndex = readBuffer.indexOf("\n")
            val line = readBuffer.substring(0, newlineIndex).trim()
            readBuffer.delete(0, newlineIndex + 1)
            
            if (line.isNotEmpty()) {
                _commands.tryEmit(line)
                Log.d("UsbSerialManager", "Emitted command: $line")
            }
        }
    }

    override fun onRunError(e: Exception) {
        Log.e("UsbSerialManager", "USB Error", e)
    }
}
