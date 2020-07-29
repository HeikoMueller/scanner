/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package com.formingstudies.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.juul.able.experimental.ConnectGattResult
import com.juul.able.experimental.android.connectGatt
import com.juul.able.experimental.toUuid
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class Scanner {

    private val TAG = "BLE SCANNER"
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var context: Context? = null

    val serviceUuid = "0000FF01-0000-1000-8000-00805F9B34FB".toUuid()
    val characteristicUuid_A = "08590F7E-DB05-467E-8757-72F6FAEB13D4".toUuid()
    val characteristicUuid_B = "E20A39F4-73F5-4BC4-A12F-17D1AD07A961".toUuid()


    // private val apiLevel: Int = android.os.Build.VERSION.SDK_INT

    fun init(context: Context) {
        Log.i(TAG, "SCANNER INIT")
        this.context = context
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
        }
    }

    fun connect(context: Context, device: BluetoothDevice) = GlobalScope.launch {
        val gatt = device.connectGatt(context, autoConnect = false).let { result ->
            when (result) {
                is ConnectGattResult.Success -> result.gatt
                is ConnectGattResult.Canceled -> throw IllegalStateException("Connection canceled.", result.cause)
                is ConnectGattResult.Failure -> throw IllegalStateException("Connection failed.", result.cause)
            }
        }

        if (gatt.discoverServices() != BluetoothGatt.GATT_SUCCESS) {
            // discover services failed
        }

        val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid_B)

        val result = characteristic?.let { gatt.readCharacteristic(it) }
        if (result != null) {
            if (result.status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "result.value = ${result.value}")
            } else {
                // read characteristic failed
                Log.i(TAG, "ELSE CASE")
            }
        }

        gatt.disconnect()
        gatt.close()
    }


//    fun start(uuids: Array<String>) {
    fun startScanning(data: Data) {
        Log.i(TAG, "ANDROID SCANNER START")
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Log.i(TAG, "API LEVEL <21")
            // API Level unter 21
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            Log.i(TAG, "API LEVEL <26")
            // API Level 21
        } else {
            Log.i(TAG, "API LEVEL 26+")
            // API Level 26
            val scanFilters = buildScanFilters(data);
            val scanSettings = buildScanSettings();
            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, mScanCallback)

        }
    }
    fun stopScanning() {
        Log.i(TAG, "ANDROID SCANNER STOP")
        scanCallback = null

    }

    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, result?.toString());
        }
    }

    private fun buildScanSettings(): ScanSettings {
        val builder = ScanSettings.Builder()
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);//SCAN_MODE_LOW_LATENCY
        return builder.build();
    }


    private fun buildScanFilters(data: Data): ArrayList<ScanFilter>? {
        var scanFilters = ArrayList<ScanFilter>()
        if(data.advertiseServiceUUID != null) {
            val dataBuilder = ScanFilter.Builder()
            dataBuilder.setServiceUuid(ParcelUuid.fromString(data.advertiseServiceUUID));
            scanFilters.add(dataBuilder.build())        
        }
        /*    
        for (uuid in data.uuids.orEmpty()) {
            val dataBuilder = ScanFilter.Builder()
            dataBuilder.setServiceUuid(ParcelUuid.fromString(uuid));
            scanFilters.add(dataBuilder.build())
        }
        */
        return scanFilters
    }
}