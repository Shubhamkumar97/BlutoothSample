package com.app.distance.activity

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.app.distance.R
import com.app.distance.adapter.DeviceAdapter
import com.app.distance.data.model.Device
import com.app.distance.services.BluetoothService
import com.app.distance.utils.GeneralFunctions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    DeviceAdapter.BlurryCallBack {


    //region Variables
    private val ACCESS_COARSE_LOCATION_CODE = 1

    private val APP_OVERLAY_SETTING_REQUEST_CODE = 20

    private val REQUEST_ENABLE_BLUETOOTH = 2

    private val REQUEST_ENABLE_BT = 123

    private val SCAN_MODE_ERROR = 3

    private var bluetoothReceiverRegistered: Boolean = false

    private val scanModeReceiverRegistered: Boolean = false

    private var swipeRefreshLayout: SwipeRefreshLayout? = null


    private var mBluetoothAdapter: BluetoothAdapter? = null

    private var mConnectedBluetoothDevice: BluetoothDevice? = null

    private var recyclerView: RecyclerView? = null

    private var deviceAdapter: DeviceAdapter? = null

    private var bluetoothManager: BluetoothManager? = null

    private val devices = ArrayList<Device>()





    private var mBlueToothService: BluetoothService? = null

    private var isServiceBound: Boolean = false

    private val handler = Handler()
    private var scanTask: Runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 500)
            scanBluetooth()
        }
    }
    //endregion


    private var bluetoothServiceIntent: Intent? = null
    private var mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.BToothServiceBinder
            mBlueToothService = binder.service
            isServiceBound = true
        }

    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: Execute")
            val action = intent.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device!!.name
                val paired = device.bondState == BluetoothDevice.BOND_BONDED
                val deviceAddress = device.address
                val deviceRSSI = intent.extras!!.getShort(BluetoothDevice.EXTRA_RSSI, 0.toShort())
                val mDevice = Device(deviceName, paired, deviceAddress, deviceRSSI, device)

//                if (device.bondState == BluetoothDevice.BOND_BONDED) {
//                    Log.i("test", "bonded")
//
//                }
//
//
//                if (deviceAddress == "1C:CC:D6:EE:CA:87") {
//
//                    Log.i("test", "device found")
//                    devices.clear()
//
//                }

                devices.remove(scannedDevice(mDevice))
                devices.add(mDevice)
                deviceAdapter!!.notifyDataSetChanged()
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                if (devices.size == 0) {
                    Log.d(TAG, "onReceive: No device")
                }
            }


        }
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//            if (BluetoothDevice.ACTION_FOUND == action) {
//                val device =
//                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                val deviceName = device!!.name
//                val paired = device.bondState == BluetoothDevice.BOND_BONDED
//                val deviceAddress = device.address
//                val deviceRSSI = intent.extras!!.getShort(BluetoothDevice.EXTRA_RSSI, 0.toShort())
//                val mDevice = Device(deviceName, paired, deviceAddress, deviceRSSI, device)
//                val state: Int =
//                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
//                val prevState = intent.getIntExtra(
//                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
//                    BluetoothDevice.ERROR
//                )
//
//                var isNew = true
//
//
//                LoopOuter@
//                for (i in devices) {
//                    if (i.address.contentEquals(mDevice.address)) {
//                        isNew = false
//                        break@LoopOuter
//                    }
//                }
//
//                if (isNew) {
//                    devices.add(mDevice)
//                }
//
//                deviceAdapter!!.notifyDataSetChanged()
//
//                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
//
//                    devices.clear()
//                    Toast.makeText(
//                        context,
//                        "Paired",
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//
//                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
//                    Toast.makeText(
//                        context,
//                        "Unpaired",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }


        private fun scannedDevice(d: Device): Device? {
            for (device in devices) {
                if (d.address == device.address) {
                    return device
                }
            }
            return null
        }

        /*private boolean containsDevice(Device d) {
            for (Device device : devices) {
                if (d.getAddress().equals(device.getAddress())) {
                    return true;
                }
            }
            return false;
        }*/
    }

    private val scanModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, SCAN_MODE_ERROR)
            if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE || scanMode == BluetoothAdapter.SCAN_MODE_NONE) {
                Toast.makeText(
                    context,
                    "The device is not visible to the outside\n",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val toolbar = toolbar
        setSupportActionBar(toolbar)
        initView()
        //Request Permission
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Get permission", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), ACCESS_COARSE_LOCATION_CODE
                )
            }
        }

        initData()
        handler.post(scanTask)
        deviceAdapter!!.notifyDataSetChanged()


        // check whether bluetooth is on then turn on service
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        when {
            mBluetoothAdapter == null -> {
                // Device does not support Bluetooth
                Toast.makeText(this, "Bluetooth Support not found !", Toast.LENGTH_LONG).show()
            }
            !mBluetoothAdapter.isEnabled -> {
                // Bluetooth is not enabled :)
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            else ->
                // Bluetooth is enabled
                bindAndStartService()
        }


    }

    override fun onResume() {
        super.onResume()
        handler.post(scanTask)
    }

    override fun onRestart() {
        super.onRestart()
        handler.post(scanTask)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (bluetoothReceiverRegistered) {
            unregisterReceiver(bluetoothReceiver)
        }
        if (scanModeReceiverRegistered) {
            unregisterReceiver(scanModeReceiver)
        }
        if (mConnectedBluetoothDevice != null) {
            unpairDevice(mConnectedBluetoothDevice!!)
            GeneralFunctions.connectedDeviceAddress = ""
        }


    }

    private fun initView() {
        swipeRefreshLayout = swipe_refresh
        swipeRefreshLayout!!.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout!!.setOnRefreshListener(this)
        recyclerView = recycler_view
        deviceAdapter = DeviceAdapter(devices, this)
        recyclerView!!.adapter = deviceAdapter
        val layoutManager = LinearLayoutManager(this)
        recyclerView!!.layoutManager = layoutManager
    }

    private fun initData() {
        bluetoothServiceIntent = Intent(this, BluetoothService::class.java)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    private fun scanBluetooth() {
        bluetoothReceiverRegistered = true
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
        if (mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter!!.cancelDiscovery()
        }
        mBluetoothAdapter!!.startDiscovery()
    }

    override fun onRefresh() {
        runOnUiThread {
            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter!!.isEnabled) {
                    //mBluetoothAdapter.enable();
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
                }
                handler.post(scanTask)
            }
            deviceAdapter!!.notifyDataSetChanged()
            swipeRefreshLayout!!.isRefreshing = false
        }
    }

    companion object {

        var TAG = "MainActivity"

    }


    //region Service Related Methods
    private fun startBluetoothNotificationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showDialog()
            } else {
                ContextCompat.startForegroundService(this, bluetoothServiceIntent!!)
            }
        }

    }


    private fun unboundService() {
        if (isServiceBound) {
            unbindService(mServiceConnection)
            isServiceBound = false
        }
        Log.d("ServiceDemo", "in unBoundService()")
    }

    private fun bindAndStartService() {
        bindService(bluetoothServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        startBluetoothNotificationService()
        Log.d("ServiceDemo", "in BindService()")
    }
    //endregion


    @TargetApi(Build.VERSION_CODES.M)
    private fun showDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Draw over Other Apps")
            .setMessage("Grant permissions to draw app over other apps")
// The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                android.R.string.yes
            ) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${this.packageName}")
                )
                startActivityForResult(intent, APP_OVERLAY_SETTING_REQUEST_CODE)
                // Continue with delete operation
            } // A null listener allows the button to dismiss the dialog and take no further action.
            .setNegativeButton(android.R.string.no, null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    override fun onStop() {
        super.onStop()
        unboundService()
    }

    override fun toggleBlurryView(shouldMakeBlurry: Boolean) {
        mBlueToothService?.toggleBlurryView(shouldMakeBlurry)
    }


    //For Pairing
    private fun pairDevice(device: BluetoothDevice) {

        try {
            mBluetoothAdapter?.cancelDiscovery()
            Log.d("pairDevice()", "Start Pairing...")
            if (device.createBond())
                GeneralFunctions.connectedDeviceAddress = device.address
            deviceAdapter?.notifyDataSetChanged()
//            val m: Method = device.javaClass.getMethod(
//                "createBond",
//                null as Class<*>?
//            )
//            m.invoke(device, null as Array<Any?>?)
            Log.d("pairDevice()", "Pairing finished.")
        } catch (e: Exception) {
            Log.e("pairDevice()", e.message)
        } finally {
            mBluetoothAdapter?.startDiscovery()

        }


    }


    override fun itemClicked(deviceData: Device) {
        mConnectedBluetoothDevice = deviceData.bluetoothDevice
        pairDevice(mConnectedBluetoothDevice!!)
        scanBluetooth()
    }

    //For UnPairing
//    private fun unpairDevice(device: BluetoothDevice) {
//        try {
//            val m = device.javaClass.getMethod("rmovebondnative()", null as Class<*>?)
//            m.invoke(device, null as Array<Any>?)
//        } catch (e: Exception) {
//            Log.e(TAG, e.message!!)
//        }
//    }

    private fun unpairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod(
                "removeBond", null as Class<*>?
            )
            method.invoke(device, null as Array<Any>?)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}