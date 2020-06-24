/*
 * Copyright (c) 2020. Heiko Müller.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package com.formingstudies.scanner

import android.bluetooth.le.BluetoothLeScanner

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import io.flutter.Log
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattServer

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES

class Scanner {

    private val tag = "Scanner"
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanSettingsBuilder: ScanSettings.Builder

    // private val apiLevel: Int = android.os.Build.VERSION.SDK_INT


    fun init(context: Context) {
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
        }
        if(mScanSettingsBuilder == null) {
            mScanSettingsBuilder = new ScanSettings.Builder();
            mScanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);//SCAN_MODE_LOW_LATENCY
            mScanSettingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
       }        
    }
    
//    fun start(uuids: Array<String>) {
    fun start(data: ScanData) {
        Log.i(tag, "ANDROID SCANNER START")
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Lollipop){
            Log.i(tag, "API LEVEL <21")
            // API Level unter 21
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.OREO){
            Log.i(tag, "API LEVEL <26")
            // API Level 21
        } else {
            Log.i(tag, "API LEVEL 26+")
            // API Level 26
            val scanFilters = builScanFilters(data);
            mBluetoothLeScanner.start(scanFilters, mScanSettingsBuilder.build(), mScanCallback)

        }
    }
    fun stop() {
        Log.i(tag, "ANDROID SCANNER STOP")
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(tag, "scan RESULT !!!");
            // prepareScanResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(tag, "Scanned failed:= " + errorCode);
        }
    };

    private fun builScanFilters(data: ScanData): ScanFilter? {
        var scanFilters = List<ScanFilder>
        for (uuid in data.uuids) {
            val dataBuilder = ScanFilter.Builder()
            dataBuilder.setServiceUuid(ParcelUuid.fromString(uuid));
            results.add(databuilder.build())
        }
        return scanFilters
    }
}