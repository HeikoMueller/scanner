package com.formingstudies.scanner

import android.R.bool
import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.*
import io.flutter.Log


/** ScannerPlugin */
class ScannerPlugin: FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private val tag = "ScannerPlugin"
  private var applicationContext: Context? = null
  private var methodChannel: MethodChannel? = null
  private var eventChannel: EventChannel? = null
  private var scanner: Scanner? = null
  private var eventSink: EventChannel.EventSink? = null

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
    eventChannel = EventChannel(messenger, "roktok.immu.dev/bluetoothScannerResponse")
    methodChannel!!.setMethodCallHandler(this);
    eventChannel!!.setStreamHandler(this)
    // eventChannel.setMethodCallHandler(this);
    scanner = Scanner()
    scanner!!.init(applicationContext)
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
      Log.i(tag, "ANDROID startScanning called")
      startScanning(call, result)
    } else if (call.method == "stopScanning") {
      Log.i(tag, "ANDROID stopScanning called")
      stopScanning(result)
    }
    else {
      result.notImplemented()
    }
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
