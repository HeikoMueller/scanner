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
    await _methodChannel.invokeMethod('startScanning', params);
  }
  Future<void> stopScanning() async {
    await _methodChannel.invokeMethod('stopScanning');
  }

  Stream<dynamic> getDetected() {
    return _eventChannel.receiveBroadcastStream().cast<String>();
  }
}
