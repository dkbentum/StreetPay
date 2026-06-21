package com.example.streetpay.usb

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UsbSerialManager(private val context: Context) : SerialInputOutputManager.Listener {
    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val commands = _commands.asSharedFlow()

    private var usbIoManager: SerialInputOutputManager? = null
    private var usbPort: UsbSerialPort? = null

    fun connect() {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) return

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device) ?: return

        val port = driver.ports[0]
        port.open(connection)
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        
        usbPort = port
        usbIoManager = SerialInputOutputManager(port, this)
        usbIoManager?.start()
        Log.d("UsbSerialManager", "Connected to USB Serial")
    }

    fun disconnect() {
        usbIoManager?.stop()
        usbIoManager = null
        usbPort?.close()
        usbPort = null
    }

    override fun onNewData(data: ByteArray) {
        val message = String(data).trim()
        if (message.isNotEmpty()) {
            _commands.tryEmit(message)
        }
    }

    override fun onRunError(e: Exception) {
        Log.e("UsbSerialManager", "USB Error", e)
    }
}
