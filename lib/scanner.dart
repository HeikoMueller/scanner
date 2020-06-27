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
    } on PlatformException catch(err){
      print("[Scanner Plugin] Start Scanning - CATCH Message : " + err.message);
      print("[Scanner Plugin] Start Scanning - CATCH Details : " + err.details.toString());
    } catch(err) {
      print("[Scanner Plugin] Start Scanning - CATCH ERROR : " + err.toString());
    }
  }
  Future<void> stopScanning() async {
    try {
      await _methodChannel.invokeMethod('stopScanning');
    } on PlatformException catch(err){
      print("[Scanner Plugin] Stop Scanning - CATCH Message : " + err.message);
      print("[Scanner Plugin] Stop Scanning - CATCH Details : " + err.details.toString());
    } catch(err) {
      print("[Scanner Plugin] Stop Scanning - CATCH ERROR : " + err.toString());
    }
  }
  Future<void> startAdvertising({@required List uuids}) async {
    Map params = <String, dynamic>{
      "uuids": uuids,
    };
    try {
      await _methodChannel.invokeMethod('startAdvertising', params);
    } on PlatformException catch(err){
      print("[Scanner Plugin] Start Advertising - CATCH Message : " + err.message);
      print("[Scanner Plugin] Start Advertising - CATCH Details : " + err.details.toString());
    } catch(err) {
      print("[Scanner Plugin] Start Advertising - CATCH ERROR : " + err.toString());
    }

  }
  Future<void> stopAdvertising() async {
    try {
      await _methodChannel.invokeMethod('stopAdvertising');
    } on PlatformException catch(err){
      print("[Scanner Plugin] Stop Advertising - CATCH Message : " + err.message);
      print("[Scanner Plugin] Stop Advertising - CATCH Details : " + err.details.toString());
    } catch(err) {
      print("[Scanner Plugin] Stop Advertising - CATCH ERROR : " + err.toString());
    }
  }    

  Stream<dynamic> getDetected() {
    return _eventChannel.receiveBroadcastStream().cast<String>();
  }
}
