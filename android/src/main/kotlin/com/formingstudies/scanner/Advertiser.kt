/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */
/* https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23 */

package com.formingstudies.scanner

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.juul.able.experimental.toUuid
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import android.util.Log as mLog


class Advertiser {

    private val TAG = "BLE ADVERTISER"
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var context: Context? = null

    private var gatt: BluetoothGattServer? = null
    val serviceUuid = "0000FF01-0000-1000-8000-00805F9B34FB".toUuid()
    val characteristicUuid_A = "08590F7E-DB05-467E-8757-72F6FAEB13D4".toUuid()
    val characteristicUuid_B = "E20A39F4-73F5-4BC4-A12F-17D1AD07A961".toUuid()

    private var advertiseServiceUuid: String? = null

    fun init(context: Context) {
        mLog.i(TAG, "ADVERTISER INIT")
        this.context = context
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothLeAdvertiser = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeAdvertiser
        }
    }

    fun expose(
            context: Context,
            uuids: ArrayList<String>,
            transmissionPowerIncluded: Boolean
    ) = GlobalScope.launch {
        // first create gatt server
        try {
            gatt = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).openGattServer(context, mGattServerCallback)

            // then add services with readable characteristics
            val uuidsIterator = uuids.iterator()
            while (uuidsIterator.hasNext()) {
                val service = buildService(uuidsIterator.next())
                service?.let {
                    val characteristic = BluetoothGattCharacteristic(
                            characteristicUuid_B,
                            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                            BluetoothGattCharacteristic.PERMISSION_READ)
                    val characteristicConfig = BluetoothGattDescriptor(characteristicUuid_A, BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE)
                    characteristic.addDescriptor(characteristicConfig)
                    service.addCharacteristic(characteristic)
                    gatt?.addService(service)
                }
            }
        } catch(err: Exception) {
            mLog.e(TAG, err.toString())
        }
    }

    fun startAdvertising(params: Map<*, *>) {
        mLog.i(TAG, "START ADVERTISING")
        val uuids = params["uuids"] as ArrayList<String>
        advertiseServiceUuid = uuids!!.first()
        val transmissionPowerIncluded: Boolean = true;
        context?.let { expose(it,
                uuids,
                transmissionPowerIncluded
        ) }
    }

    fun stopAdvertising() {
        mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
        gatt!!.close()
        gatt == null;
    }

    // Callbacks on own GATT server
    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            mLog.i(TAG, "GATT-SERVER CALLBACK called Connection Did Change")
            mLog.i(TAG, device?.toString())
            mLog.i(TAG, status.toString())
            mLog.i(TAG, newState.toString())
        }
        // fires when the service in expose() has been added
        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            mLog.i(TAG, "ON-SERVICE-ADDED ... now start advertising")
            // finally advertise the service
            val settings = buildAdvertiseSettings()
            advertiseServiceUuid?.let {
                val advertiseData = buildAdvertiseData(
                        advertiseServiceUuid!!,
                        true)
                mBluetoothLeAdvertiser!!.startAdvertising(settings, advertiseData, mAdvertiseCallback)
            }
        }


        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            mLog.i(TAG, "READREQUEST RECEIVED a")
            mLog.i(TAG, "requestId $requestId.toString()")
            mLog.i(TAG, "offset $offset.toString()")
            mLog.i(TAG, "characteristic $characteristic.toString()")


            if (characteristicUuid_B == characteristic?.uuid) {
                try {
                    if(gatt == null) {
                        mLog.e(TAG, "GATT SERVER IS NULL")
                    } else {
                        mLog.i(TAG, "GATT SERVER IS NOT NULL")
                    }
                    val string = "HelloHank from Android"
                    val value: ByteArray =  string.toByteArray(Charsets.UTF_8)
                    gatt?.sendResponse(device, requestId, GATT_SUCCESS, 0, value)
                } catch(err: Exception) {
                    mLog.e(TAG, err.toString())
                }
            }
        }


    }


    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            mLog.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            mLog.e(TAG, "ERROR while starting advertising: $errorCode")
            val statusText: String

            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    statusText = "ADVERTISE_FAILED_ALREADY_STARTED"
                }
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                    statusText = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                }
                ADVERTISE_FAILED_INTERNAL_ERROR -> {
                    statusText = "ADVERTISE_FAILED_INTERNAL_ERROR"
                }
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                    statusText = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                }
                ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                    statusText = "ADVERTISE_FAILED_DATA_TOO_LARGE"
                }

                else -> {
                    statusText = "UNDOCUMENTED"
                }
            }

            mLog.e(TAG, "ERROR while starting advertising: $errorCode - $statusText")
        }
    }


    private fun buildService(uuidString: String): BluetoothGattService? {
        val uuid = ParcelUuid.fromString(uuidString).uuid
        val type = BluetoothGattService.SERVICE_TYPE_PRIMARY
        return BluetoothGattService(uuid, type);
    }

    private fun buildAdvertiseData(
            advertiseServiceUuid: String,
            transmissionPowerIncluded: Boolean
    ): AdvertiseData? {

        // val serviceData = data.serviceData?.let { intArrayToByteArray(it) }
        // val manufacturerData = data.manufacturerData?.let { intArrayToByteArray(it) }
        val dataBuilder = AdvertiseData.Builder()
        advertiseServiceUuid?.let { dataBuilder.addServiceUuid(ParcelUuid.fromString(advertiseServiceUuid!!)) }
        transmissionPowerIncluded?.let { dataBuilder.setIncludeTxPowerLevel(it) }
        // manufacturerId?.let { dataBuilder.addManufacturerData(it, manufacturerData) }
        // includeDeviceName?.let { dataBuilder.setIncludeDeviceName(it) }
        return dataBuilder.build()
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings? {
        val settingsBuilder = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
        return settingsBuilder.build()
    }

    private fun intArrayToByteArray(ints: List<Int>): ByteArray {
        return ints.foldIndexed(ByteArray(ints.size)) { i, a, v -> a.apply { set(i, v.toByte()) } }
    }
}