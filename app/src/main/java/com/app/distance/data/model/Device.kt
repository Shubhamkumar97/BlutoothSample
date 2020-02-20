package com.app.distance.data.model

import android.bluetooth.BluetoothDevice
import kotlin.math.max
import kotlin.math.min

data class Device(
    val name: String? = "",
    val isPaired: Boolean,
    val address: String,
    val signal: Short,
    var bluetoothDevice: BluetoothDevice
) {
    val signal2: String
        get() = min(max(2 * (signal + 100), 0), 100).toString()

    val signal3: String
        get() = signal.toString() + "dBm"
}