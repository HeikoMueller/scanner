package com.formingstudies.scanner

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

import io.flutter.plugin.common.FlutterEventChannel

/** ScannerPlugin */
public class ScannerPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var eventChannel : FlutterEventChannel

  private var scanner: Scanner? = null


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "roktok.immu.dev/bluetoothScanner")
    channel.setMethodCallHandler(this);
    eventChannel = FlutterEventChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "roktok.immu.dev/bluetoothScannerResponse")
    eventChannel.setMethodCallHandler(this);
    scanner = Scanner()
    scanner!!.init(applicationContext)

  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "roktok.immu.dev/bluetoothScanner")
      channel.setMethodCallHandler(ScannerPlugin())
      val eventChannel = MethodChannel(registrar.messenger(), "roktok.immu.dev/bluetoothScannerResponse")
      eventChannel.setMethodCallHandler(ScannerPlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "startScanning") {
      Log.i(tag, "ANDROID startScanning called")
      startScanning(result)
    } else if (call.method == "stopScanning") {
      Log.i(tag, "ANDROID stopScanning called")
      stopScanning(result)
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    eventChannel.setMethodCallHandler(null)
  }

  private fun startScanning(result: MethodChannel.Result) {
    scanner!!.start()
    result.success(null)
  }

  private fun stopScanning(result: MethodChannel.Result) {
    scanner!!.stop()
    result.success(null)
  }

}
