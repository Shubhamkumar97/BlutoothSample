package com.app.distance.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.distance.R
import com.app.distance.data.model.Device
import com.app.distance.utils.Constants.BLUETOOTH_MIN_STRENGTH_RANGE_TO_SEE_VIDEO
import com.app.distance.utils.GeneralFunctions
import kotlinx.android.synthetic.main.device_item.view.*

class DeviceAdapter(
    private var mDeviceList: List<Device>,
    private val mBlurryCallBack: BlurryCallBack
) :
    RecyclerView.Adapter<DeviceAdapter.DeviceHolder>() {

    class DeviceHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return DeviceHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
        val device = mDeviceList[position]
        holder.itemView.text_name.text = device.name
        holder.itemView.text_address.text = "MAC: " + device.address

        holder.itemView.text_signal.text = device.signal2 + device.signal3


        // toggle BlurryView
        if (mDeviceList[position].address == GeneralFunctions.connectedDeviceAddress)
            mBlurryCallBack.toggleBlurryView(device.signal2.toInt() > BLUETOOTH_MIN_STRENGTH_RANGE_TO_SEE_VIDEO)

        holder.itemView.setOnClickListener {

            mBlurryCallBack.itemClicked(mDeviceList[position])
        }


    }

    override fun getItemCount(): Int {
        return mDeviceList.size
    }


    interface BlurryCallBack {
        fun toggleBlurryView(shouldMakeBlurry: Boolean)
        fun itemClicked(deviceData: Device)
    }
}