import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

import 'data.dart';

class Scanner {

  factory Scanner() {
    if (_instance == null) {
      final MethodChannel methodChannel = const MethodChannel(
          'roktok.immu.dev/bluetoothScanner');

      final EventChannel eventChannel = const EventChannel(
          'roktok.immu.dev/bluetoothScannerResponse');
      _instance = Scanner.private(methodChannel, eventChannel);
    }
    return _instance;
  }

  @visibleForTesting
  Scanner.private(this._methodChannel, this._eventChannel);

  static Scanner _instance;
  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;

  Future<void> startScanning({@required ScanData data}) async {
    Map params = <String, dynamic>{
      "uuids": data.uuids,
    };
    try {
      await _methodChannel.invokeMethod('startScanning', params);
    } catch(err) {
      print("[Scanner Plugin] Start Scanning - CATCH ERROR : " + err.error.toString());
    }
  }
  Future<void> stopScanning() async {
    try {
      await _methodChannel.invokeMethod('stopScanning');
    } catch(err) {
      print("[Scanner Plugin] Stop Scanning - CATCH ERROR : " + err.error.toString());
    }
  }
  Future<void> startAdvertising({@required List uuids}) async {
    Map params = <String, dynamic>{
      "uuids": uuids,
    };
    try {
      await _methodChannel.invokeMethod('startAdvertising', params);
    } catch(err) {
      print("[Scanner Plugin] Start Advertising - CATCH ERROR : " + err.error.toString());
    }

  }
  Future<void> stopAdvertising() async {
    try {
      await _methodChannel.invokeMethod('stopAdvertising');
    } catch(err) {
      print("[Scanner Plugin] Stop Advertising - CATCH ERROR : " + err.error.toString());
    }
  }    

  Stream<dynamic> getDetected() {
    return _eventChannel.receiveBroadcastStream().cast<String>();
  }
}
