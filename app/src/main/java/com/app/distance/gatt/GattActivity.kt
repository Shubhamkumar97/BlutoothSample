package com.app.distance.gatt

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.app.distance.R
import com.app.distance.utils.convertFromInteger
import kotlinx.android.synthetic.main.activity_gatt.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess


class GattActivity : AppCompatActivity() {


    private val BLUETTOTH_SERVICE_UUID =
        convertFromInteger(0x180D)
    //    private val HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D)
    private val BLUETTOTH_MEASUREMENT_CHAR_UUID =
        convertFromInteger(0x2A37)
    //    private val HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37)
    private val HEART_RATE_CONTROL_POINT_CHAR_UUID =
        convertFromInteger(0x2A39)

    private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
        convertFromInteger(0x2902)


    private val REQUEST_ENABLE_BT = 123
    private val PERMISSION_REQUEST_LOCATION = 123
    private val PERMISSION_REQUEST_LOCATION_KEY = "PERMISSION_REQUEST_LOCATION"
    private var alreadyAskedForPermission = false

    private var myDevice: BluetoothDevice? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothManager: BluetoothManager
    private var mBluetoothGatt: BluetoothGatt? = null

    // scanCallback
    private val scanCallBack = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->

        device.let {
            Log.e(
                "test-address", it.name ?: it
                    ?.address!!
            )
        }
        myDevice = device
        mBluetoothGatt = myDevice?.connectGatt(this, true, bluetoothGattCalBack)


        Log.e("test", rssi.toString())
        Log.e("test", getSignalStrength(rssi).toString())
    }


    // bluetoothGattCalback
    private val bluetoothGattCalBack = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            // set characteristic
            val characteristic = mBluetoothGatt?.getService(BLUETTOTH_SERVICE_UUID)
                ?.getCharacteristic(BLUETTOTH_MEASUREMENT_CHAR_UUID)
            gatt?.setCharacteristicNotification(characteristic, true)


            // write descriptor
            val descriptor = characteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
        }


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt)

        // get BlueTooth Manager
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        // get Bluetooth Adapter
        mBluetoothAdapter = bluetoothManager.adapter

        // check permission
        checkPermissions()

        //start scanning
        startScanning()

    }


    private fun showAlertAndExit() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.not_compatible))
            .setMessage(getString(R.string.no_support))
            .setPositiveButton("Exit") { _, _ -> exitProcess(0) }
            .show()
    }


    private fun startScanning() {

        val action = intent.action

        if (checkBluetoothStatus().not()) {
            return
        }

        val bluetoothScanner = mBluetoothAdapter?.bluetoothLeScanner
        val builder = ScanFilter.Builder()

        val filter = arrayListOf<ScanFilter>()
        filter.add(builder.build())
        val builderScanSettings = ScanSettings.Builder()
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        builderScanSettings.setReportDelay(0)
        bluetoothScanner?.startScan(filter, builderScanSettings.build(), object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                result.device.apply {
                    Log.i("test-address", this?.name ?: this?.address!!)
                }
                if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                    Toast.makeText(
                        this@GattActivity,
                        "Strength ${getSignalStrength(result.rssi)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i("test-rssi", result.rssi.toString())
                    Log.i("test", getSignalStrength(result.rssi).toString())
                }
            }
        })

//        mBluetoothAdapter?.startLeScan(scanCallBack)
    }


    private fun checkPermissions() {

        if (alreadyAskedForPermission) {
            // don't check again because the dialog is still open
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.need_loc_access))
                builder.setMessage(getString(R.string.please_grant_loc_access))
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    // the dialog will be opened so we have to save that
                    alreadyAskedForPermission = true
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ), PERMISSION_REQUEST_LOCATION
                    )
                }
                builder.show()

            } else {
                startScanning()
            }
        } else {
            startScanning()
            alreadyAskedForPermission = true
        }
    }

    private fun checkBluetoothStatus(): Boolean {
        // check whether bluetooth is turned on

        var blueToothStatus = false

        if (mBluetoothAdapter == null)
            showAlertAndExit()
        else {
            if (mBluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                tvStatus.text = getString(R.string.not_connected)
                blueToothStatus = true
            }
        }
        return blueToothStatus
    }

    private fun getSignalStrength(rssi: Int): Int {


        return min(max(2 * (rssi + 100), 0), 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            //Bluetooth is now connected.
            Toast.makeText(this, "Bluetooth turned on", Toast.LENGTH_LONG).show()
            startScanning()
        }
    }


}



