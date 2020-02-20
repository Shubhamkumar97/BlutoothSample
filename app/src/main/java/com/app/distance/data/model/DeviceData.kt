package com.app.distance.data.model

/**
 * @author Shubham
 * 15/2/20
 */
data class DeviceData(
    val deviceName: String?,
    val deviceHardwareAddress: String
) {

    override fun equals(other: Any?): Boolean {

//        BluetoothGattCallback
        
        val deviceData = other as DeviceData
        return deviceHardwareAddress == deviceData.deviceHardwareAddress
    }

    override fun hashCode(): Int {
        return deviceHardwareAddress.hashCode()
    }

}