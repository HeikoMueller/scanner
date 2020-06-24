/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package com.formingstudies.scanner

import android.bluetooth.le.BluetoothLeScanner

import android.bluetooth.BluetoothManager
// import android.bluetooth.le.AdvertiseCallback
// import android.bluetooth.le.AdvertiseSettings
// import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import io.flutter.Log
// import android.bluetooth.BluetoothGattServerCallback
// import android.bluetooth.BluetoothDevice
// import android.bluetooth.BluetoothGattService
// import android.bluetooth.BluetoothGattServer

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanResult
import android.os.*
// import android.os.Build.VERSION
// import android.os.Build.VERSION_CODES

class Scanner {

    private val tag = "Scanner"
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null


    // private val apiLevel: Int = android.os.Build.VERSION.SDK_INT


    fun init(context: Context) {
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
        }
    }
    
//    fun start(uuids: Array<String>) {
    fun start(data: Data) {
        Log.i(tag, "ANDROID SCANNER START")
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Log.i(tag, "API LEVEL <21")
            // API Level unter 21
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            Log.i(tag, "API LEVEL <26")
            // API Level 21
        } else {
            Log.i(tag, "API LEVEL 26+")
            // API Level 26
            val scanFilters = buildScanFilters(data);
            val scanSettings = buildScanSettings();
            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, mScanCallback)

        }
    }
    fun stop() {
        Log.i(tag, "ANDROID SCANNER STOP")
        scanCallback = null

    }

    private val mScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result);
            Log.i(tag, result?.toString());
        }
    }

    private fun buildScanSettings(): ScanSettings {
        val builder = ScanSettings.Builder()
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);//SCAN_MODE_LOW_LATENCY
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        return builder.build();
    }


    private fun buildScanFilters(data: Data): ArrayList<ScanFilter>? {
        var scanFilters = ArrayList<ScanFilter>()
        for (uuid in data.uuids.orEmpty()) {
            val dataBuilder = ScanFilter.Builder()
            dataBuilder.setServiceUuid(ParcelUuid.fromString(uuid));
            scanFilters.add(dataBuilder.build())
        }
        return scanFilters
    }
}