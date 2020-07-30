/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package com.formingstudies.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log as mLog
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

    private var discoveredDevices: ArrayList<String> = ArrayList()


    val serviceUuid = "0000FF01-0000-1000-8000-00805F9B34FB".toUuid()
    val characteristicUuid_A = "08590F7E-DB05-467E-8757-72F6FAEB13D4".toUuid()
    val characteristicUuid_B = "E20A39F4-73F5-4BC4-A12F-17D1AD07A961".toUuid()


    // private val apiLevel: Int = android.os.Build.VERSION.SDK_INT

    fun init(context: Context) {
        mLog.i(TAG, "SCANNER INIT")
        this.context = context
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
        }
    }

    suspend fun connect(context: Context, device: BluetoothDevice) = GlobalScope.launch {
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
                mLog.i(TAG, "result.value = ${String(result.value, Charsets.UTF_8)}")
            } else {
                // read characteristic failed
                mLog.i(TAG, "ELSE CASE")
            }
        }
        gatt.disconnect()
        gatt.close()
    }


//    fun start(uuids: Array<String>) {
    fun startScanning(params: Map<*, *>) {
        mLog.i(TAG, "ANDROID SCANNER START")
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            mLog.i(TAG, "API LEVEL <21")
            // API Level unter 21
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            mLog.i(TAG, "API LEVEL <26")
            // API Level 21
            val scanFilters = buildScanFilters(params);
            val scanSettings = buildScanSettings();
            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, mScanCallback)
        } else {
            mLog.i(TAG, "API LEVEL 26+")
            // API Level 26 -> TODO startScan with pending intend
            val scanFilters = buildScanFilters(params);
            val scanSettings = buildScanSettings();
            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, mScanCallback)
        }
    }
    fun stopScanning() {
        mLog.i(TAG, "ANDROID SCANNER STOP")
        mBluetoothLeScanner!!.stopScan(mScanCallback)
        discoveredDevices.clear()
        // scanCallback = null
    }

    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result);
            var key = result?.device.toString()

            try {
                if(!discoveredDevices.contains(key)) {
                    discoveredDevices.add(result.device.toString())
                    mLog.i(TAG, result?.toString());
                    context?.let {
                        launch(context) {
                            connect(it, result.device)
                        }
                    }
                } else {
                    val message = "KNOWN DEVICE : " + result?.toString()
                    mLog.i(TAG, message);
                }
            } catch(err: Exception) {
                mLog.e(TAG, err.toString())
            }
        }
    }

    private fun buildScanSettings(): ScanSettings {
        val builder = ScanSettings.Builder()
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);//SCAN_MODE_LOW_LATENCY
        return builder.build();
    }

    private fun buildScanFilters(params: Map<*, *>): ArrayList<ScanFilter>? {
        var scanFilters = ArrayList<ScanFilter>()
        val uuids = params["uuids"] as ArrayList<String>
        for (uuid in uuids) {
            val dataBuilder = ScanFilter.Builder()
            dataBuilder.setServiceUuid(ParcelUuid.fromString(uuid));
            scanFilters.add(dataBuilder.build())
        }
        return scanFilters
    }
}