/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package com.formingstudies.scanner

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import io.flutter.Log

class Advertiser {

    private var isAdvertising = false
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private val tag = "BLE ADVERTISER"
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var context: Context? = null

    private val mBluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered (gatt: BluetoothGatt , status: Int) {
            Log.i(tag, "EXTERNAL SEVICE called Connection Did Change")
            for(service in gatt.services.orEmpty()) {
                Log.i(tag, "Service discovered " + service.uuid.toString())
            }
        }
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(tag, "EXTERNAL SEVICE connection did change")
            gatt.discoverServices()
            // Log.i(tag, gatt.services.toString())

        }
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.i(tag, "EXTERNAL SEVICE characteristic changed")
        }
    }

    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.i(tag, "GATT-SERVER CALLBACK called Connection Did Change")
            Log.i(tag, device?.toString())
            Log.i(tag, status.toString())
            Log.i(tag, newState.toString())
            try {
                device?.connectGatt(context,true, mBluetoothGattCallback)
            } catch(err: Exception) {
                Log.e(tag, err.toString())
            }
        }
    }

    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.i(tag, "LE Advertise Started.")
            //advertisingCallback(true)
            isAdvertising = true
        }
        
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(tag, "ERROR while starting advertising: $errorCode")
            val statusText: String

            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    statusText = "ADVERTISE_FAILED_ALREADY_STARTED"
                    isAdvertising = true
                }
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                    statusText = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                    isAdvertising = false
                }
                ADVERTISE_FAILED_INTERNAL_ERROR -> {
                    statusText = "ADVERTISE_FAILED_INTERNAL_ERROR"
                    isAdvertising = false
                }
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                    statusText = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                    isAdvertising = false
                }
                ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                    statusText = "ADVERTISE_FAILED_DATA_TOO_LARGE"
                    isAdvertising = false
                }

                else -> {
                    statusText = "UNDOCUMENTED"
                }
            }

            Log.e(tag, "ERROR while starting advertising: $errorCode - $statusText")
            //advertisingCallback(false)
            isAdvertising = false
        }
    }
    
    fun init(context: Context) {
        this.context = context
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothLeAdvertiser = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeAdvertiser
        }
        if (mBluetoothGattServer == null) {
            mBluetoothGattServer = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).openGattServer(context, mGattServerCallback)
        }        
    }
    
    fun start(data: Data) {
        // Log.i(tag, "START ADVERTISING " + data.uuid)

        val settings = buildAdvertiseSettings()
        val advertiseData = buildAdvertiseData(data)

        // first remove all services
        mBluetoothGattServer!!.clearServices()
        for(uuidString in data.uuids!!) {
            val service = buildService(uuidString)
            mBluetoothGattServer!!.addService(service)
        }

        if(!isAdvertising) {
            mBluetoothLeAdvertiser!!.startAdvertising(settings, advertiseData, mAdvertiseCallback)
        }
    }

    fun isAdvertising(): Boolean {
        return isAdvertising
    }

    // TODO: Fix transmission supported type
//    fun isTransmissionSupported(): Int {
//        return checkTransmissionSupported(context)
//    }

    fun stop() {
        mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
        mBluetoothGattServer!!.clearServices()
        advertiseCallback = null
        isAdvertising = false
    }

    private fun buildService(uuidString: String): BluetoothGattService? {
        val uuid = ParcelUuid.fromString(uuidString).uuid
        val type = BluetoothGattService.SERVICE_TYPE_PRIMARY
        return BluetoothGattService(uuid, type);
    }

    private fun buildAdvertiseData(data: Data): AdvertiseData? {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         * This includes everything put into AdvertiseData including UUIDs, device info, &
         * arbitrary service or manufacturer data.
         * Attempting to send packets over this limit will result in a failure with error code
         * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         * onStartFailure() method of an AdvertiseCallback implementation.
         */
        // val serviceData = data.serviceData?.let { intArrayToByteArray(it) }
        // val manufacturerData = data.manufacturerData?.let { intArrayToByteArray(it) }
        val uuid = data.uuids!!.first()    
        val dataBuilder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid.fromString(uuid!!))
            .setIncludeTxPowerLevel(it)
        /*
        dataBuilder.addServiceUuid(ParcelUuid.fromString(data.uuid))
        data.serviceDataUuid?.let { dataBuilder.addServiceData(ParcelUuid.fromString(it), serviceData) }
        */
        // data.manufacturerId?.let { dataBuilder.addManufacturerData(it, manufacturerData) }
        // data.includeDeviceName?.let { dataBuilder.setIncludeDeviceName(it) }
        // data.transmissionPowerIncluded?.let { dataBuilder.setIncludeTxPowerLevel(it) }
        return dataBuilder.build()
    }

    /** TODO: make settings configurable */
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