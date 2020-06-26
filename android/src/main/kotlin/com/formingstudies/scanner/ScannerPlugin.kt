package com.formingstudies.scanner

import android.R.bool
import android.content.Context
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
    scanner = Scanner().init(applicationContext)
    advertiser = Advertiser().init(applicationContext)
   }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    applicationContext = null
    methodChannel!!.setMethodCallHandler(null)
    methodChannel = null
    eventChannel!!.setStreamHandler(null)
    eventChannel = null
    scanner = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "startScanning") {
      Log.i(TAG, "ANDROID startScanning called")
      startScanning(call, result)
    } else if (call.method == "stopScanning") {
      Log.i(TAG, "ANDROID stopScanning called")
      stopScanning(result)
    } else if (call.method == "startAdvertising") {
      Log.i(TAG, "ANDROID startAdvertising called")
      startAdvertise(call, result)
    } else if (call.method == "stopAdvertising") {
      Log.i(TAG, "ANDROID stopAdvertising called")
      stopAdvertise(result)
    } else {
      result.notImplemented()
    }
  }

  private fun startAdvertise(call: MethodCall, result: MethodChannel.Result) {
    if (call.arguments !is Map<*, *>) {
      throw IllegalArgumentException("Arguments are not a map " + call.arguments)
    }
    val arguments = call.arguments as Map<String, Any>
    val advertiseData = Data(
      arguments["uuids"] as List<String>?  
    )
    if(advertisser == null) {
      Log.e(TAG, "ADVERTISER IS NULL !!");
    }
    try {
      advertiser!!.start(advertiseData)
    } catch(err: Exception) {
      Log.e(TAG, "Error in startAdvertise" + err.toString())
    }
    result.success(null)
  }

  private fun stopAdvertise(result: MethodChannel.Result) {
    try {
      advertiser!!.stop()
    } catch(err: Exception) {
      Log.e(TAG, err.toString())
    }

    result.success(null)
  }




  private fun startScanning(call: MethodCall, result: MethodChannel.Result) {
    if (call.arguments !is Map<*, *>) {
      throw IllegalArgumentException("Arguments are not a map " + call.arguments)
    }
    val arguments = call.arguments as Map<String, Any>
    val scanData = Data(
      arguments["uuids"] as List<String>?  
    )
    scanner!!.start(scanData)
    result.success(null)
  }

  private fun stopScanning(result: MethodChannel.Result) {
    scanner!!.stop()
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
