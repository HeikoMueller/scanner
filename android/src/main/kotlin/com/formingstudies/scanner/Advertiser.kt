/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */
/* https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23 */

package com.formingstudies.scanner

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import io.flutter.Log
import java.util.*


class Advertiser {

    private var isAdvertising = false
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private val TAG = "BLE ADVERTISER"
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var context: Context? = null
    private val MAX_TRIES = 10
    private var bleHandler: Handler = Handler()

    private val commandQueue: Queue<Runnable>? = null
    private var commandQueueBusy = false
    private var isRetrying: Boolean = false
    private var bluetoothGatt: BluetoothGatt? = null
    private var nrTries: Int = 0

    private fun completedCommand() {
        commandQueueBusy = false
        isRetrying = false
        commandQueue!!.poll()
        nextCommand()
    }

    private fun retryCommand() {
        commandQueueBusy = false
        val currentCommand = commandQueue!!.peek()
        if (currentCommand != null) {
            if (nrTries >= MAX_TRIES) {
                // Max retries reached, give up on this one and proceed
                Log.v(TAG, "Max number of tries reached")
                commandQueue.poll()
            } else {
                isRetrying = true
            }
        }
        nextCommand()
    }

    private fun nextCommand() {
        // If there is still a command being executed then bail out
        if (commandQueueBusy) {
            return
        }

        // Check if we still have a valid gatt object
        if (bluetoothGatt == null) {
            Log.e(TAG, java.lang.String.format("ERROR: GATT is 'null' for peripheral '%s', clearing command queue", getAddress()))
            commandQueue!!.clear()
            commandQueueBusy = false
            return
        }

        // Execute the next command in the queue
        if (commandQueue!!.size > 0) {
            val bluetoothCommand = commandQueue.peek()
            commandQueueBusy = true
            nrTries = 0
            bleHandler.post(Runnable {
                try {
                    bluetoothCommand.run()
                } catch (err) {
                    Log.e(TAG, "bleHandler CATCH ERROR " + err.toString())
                }
            })
        }
    }


    // the readCharacteristic command
    open fun readCharacteristic(characteristic: BluetoothGattCharacteristic?): Boolean {
        Log.i(TAG, "READ CHARACTERISTICS START")

        if (bluetoothGatt == null) {
            Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request")
            return false
        }

        // Check if characteristic is valid
        if (characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request")
            return false
        }

        // Check if this characteristic actually has READ property
        if (characteristic.properties and PROPERTY_READ === 0) {
            Log.e(TAG, "ERROR: Characteristic cannot be read")
            return false
        }

        // Enqueue the read command now that all checks have been passed
        val result = commandQueue!!.add(Runnable {
            if (!bluetoothGatt!!.readCharacteristic(characteristic)) {
                Log.e(TAG, java.lang.String.format("ERROR: readCharacteristic failed for characteristic: %s", characteristic.uuid))
                completedCommand()
            } else {
                Log.d(TAG, java.lang.String.format("reading characteristic <%s>", characteristic.uuid))
                nrTries++
            }
        })
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command")
        }
        return result
    }

    private val mBluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered (gatt: BluetoothGatt , status: Int) {
            Log.i(TAG, "EXTERNAL SERVICE called Connection Did Change")
            for(service in gatt.services.orEmpty()) {
                Log.i(TAG, "Service discovered " + service.uuid.toString())
                // now get characteristics
                var characteristic = service.getCharacteristic(ParcelUuid.fromString("00001800-0000-1000-8000-00805f9b34fb").uuid)
                Log.i(TAG,characteristic.toString())
                Log.i(TAG, "CHARACTERISTIC ?")
                /*
                for(characteristic in service.characteristics) {
                    readCharacteristic(characteristic)
                }
                */
                 */
            }
            Log.i(TAG, "Service discovered END HANK ===============================================")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "EXTERNAL SERVICE connection did change")
            bluetoothGatt = gatt
            gatt.discoverServices()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.i(TAG, "EXTERNAL SERVICE characteristic changed")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i(TAG, "EXTERNAL SERVICE characteristic READ")
        }

        /*
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i(TAG, "EXTERNAL SEVICE characteristic READ")
            if (status != GATT_SUCCESS) {
                Log.e(TAG, String.format(Locale.ENGLISH,"ERROR: Read failed for characteristic: %s, status %d", characteristic.getUuid(), status));
                completedCommand();
                return;
            }

            // Characteristic has been read so processes it
            ...
            // We done, complete the command
            completedCommand();
            val value = characteristic.value;
            if(value != null) {
                Log.i(TAG, "EXTERNAL SERVICE VALUE $value")
            }

         */
    }



    /* Callbacks on own GATT server */
    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.i(TAG, "GATT-SERVER CALLBACK called Connection Did Change")
            Log.i(TAG, device?.toString())
            Log.i(TAG, status.toString())
            Log.i(TAG, newState.toString())
            try {
                // try to connect
                device?.connectGatt(context,true, mBluetoothGattCallback)
            } catch(err: Exception) {
                Log.e(TAG, err.toString())
            }
        }
    }

    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.i(TAG, "LE Advertise Started.")
            //advertisingCallback(true)
            isAdvertising = true
        }
        
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "ERROR while starting advertising: $errorCode")
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

            Log.e(TAG, "ERROR while starting advertising: $errorCode - $statusText")
            //advertisingCallback(false)
            isAdvertising = false
        }
    }
    
    fun init(context: Context) {
        Log.i(TAG, "ADVERTISER INIT")
        this.context = context
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothLeAdvertiser = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeAdvertiser
        }
        if (mBluetoothGattServer == null) {
            mBluetoothGattServer = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).openGattServer(context, mGattServerCallback)
        }   
    }
    
    fun start(data: Data) {
        // Log.i(TAG, "START ADVERTISING " + data.uuid)

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
//            .setIncludeTxPowerLevel(it)
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