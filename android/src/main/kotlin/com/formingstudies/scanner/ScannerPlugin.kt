package com.formingstudies.scanner

import android.R.bool
import android.content.Context
import android.util.Log as mLog
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.*
import io.flutter.Log



/** ScannerPlugin */
class ScannerPlugin: FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private val TAG = "ScannerPlugin"
  private var applicationContext: Context? = null
  private var methodChannel: MethodChannel? = null
  private var eventChannel: EventChannel? = null
  private var eventSink: EventChannel.EventSink? = null
  private var scanner: Scanner? = null
  private var advertiser: Advertiser? = null

  /** Plugin registration embedding v1 */
  companion object {
    @JvmStatic
    fun registerWith(registrar: PluginRegistry.Registrar) {
      ScannerPlugin().onAttachedToEngine(registrar.context(), registrar.messenger())
    }
  }

  /** Plugin registration embedding v2 */
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
  }

  private fun onAttachedToEngine(applicationContext: Context, messenger: BinaryMessenger) {
    this.applicationContext = applicationContext
    methodChannel = MethodChannel(messenger, "roktok.immu.dev/bluetoothScanner")
    methodChannel!!.setMethodCallHandler(this);
    eventChannel = EventChannel(messenger, "roktok.immu.dev/bluetoothScannerResponse")
    eventChannel!!.setStreamHandler(this)
    advertiser = Advertiser()
    advertiser!!.init(applicationContext)
    scanner = Scanner()
    scanner!!.init(applicationContext)
   }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    applicationContext = null
    methodChannel!!.setMethodCallHandler(null)
    methodChannel = null
    eventChannel!!.setStreamHandler(null)
    advertiser = null
    scanner = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "startScanning") {
      mLog.i(TAG, "ANDROID startScanning called")
      startScanning(call, result)
    } else if (call.method == "stopScanning") {
      mLog.i(TAG, "ANDROID stopScanning called")
      stopScanning(result)
    } else if (call.method == "startAdvertising") {
      mLog.i(TAG, "ANDROID startAdvertising called")
      startAdvertising(call, result)
    } else if (call.method == "stopAdvertising") {
      mLog.i(TAG, "ANDROID stopAdvertising called")
      stopAdvertising(result)
    } else {
      result.notImplemented()
    }
  }

  private fun startAdvertising(call: MethodCall, result: MethodChannel.Result) {
    mLog.i(TAG, "startAdvertise in ScannerPlugin called")
    if (call.arguments !is Map<*, *>) {
      throw IllegalArgumentException("Arguments are not a map " + call.arguments)
    }
    val arguments = call.arguments as Map<String, Any>
    try {
      advertiser!!.startAdvertising(arguments)
    } catch(err: Exception) {
      mLog.e(TAG, "Error in startAdvertise" + err.toString())
    }
    result.success(null)
  }

  private fun stopAdvertising(result: MethodChannel.Result) {
    mLog.i(TAG, "stopAdvertise in ScannerPlugin called")
    try {
      advertiser!!.stopAdvertising()
    } catch(err: Exception) {
      mLog.e(TAG, err.toString())
    }

    result.success(null)
  }




  private fun startScanning(call: MethodCall, result: MethodChannel.Result) {
    mLog.i(TAG, "startScanning in ScannerPlugin called")
    if (call.arguments !is Map<*, *>) {
      throw IllegalArgumentException("Arguments are not a map " + call.arguments)
    }
    val arguments = call.arguments as Map<String, Any>
    scanner!!.startScanning(arguments)
    result.success(null)
  }

  private fun stopScanning(result: MethodChannel.Result) {
    mLog.i(TAG, "stopScanning in ScannerPlugin called")
    scanner!!.stopScanning()
    result.success(null)
  }

  // TODO: Fix listeners
  override fun onListen(event: Any?, eventSink: EventChannel.EventSink) {
    this.eventSink = eventSink
  }

  override fun onCancel(event: Any?) {
    this.eventSink = null
  }
}
