import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:log_4_dart_2/log_4_dart_2.dart';

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
    if(_log == null) {
      _log = Logger();
      _log.init({
        "appenders": [
          {
            "type": "CONSOLE",
            "dateFormat": "yyyy-MM-dd HH:mm:ss",
            "format": "%d %i %t %l %m",
            "level": "TRACE"
          }]
      });
    }
    return _instance;
  }

  @visibleForTesting
  Scanner.private(this._methodChannel, this._eventChannel);

  static Scanner _instance;
  static Logger _log;
  static const TAG = "[Scanner Plugin]";
  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;

  Future<void> startScanning({@required ScanData data}) async {
    Map params = <String, dynamic>{
      "uuids": data.uuids,
    };
    try {
      await _methodChannel.invokeMethod('startScanning', params);
    } on PlatformException catch(err){
      _log.error(TAG,"Start Scanning - CATCH ERROR : " + err.toString());
      _log.error(TAG,"Start Scanning - CATCH Message : " + err.message);
      _log.error(TAG,"Start Scanning - CATCH Details : " + err.details.toString());
    } 
  }
  Future<void> stopScanning() async {
    try {
      await _methodChannel.invokeMethod('stopScanning');
    } on PlatformException catch(err){
      _log.error(TAG,"Stop Scanning - CATCH ERROR : " + err.toString());
      _log.error(TAG,"Stop Scanning - CATCH Message : " + err.message);
      _log.error(TAG,"Stop Scanning - CATCH Details : " + err.details.toString());
    } 
  }
  Future<void> startAdvertising({@required List uuids}) async {
    Map params = <String, dynamic>{
      "uuids": uuids,
    };
    try {
      await _methodChannel.invokeMethod('startAdvertising', params);
    } on PlatformException catch(err){
      _log.error(TAG,"Start Advertising - CATCH ERROR : " + err.toString());
      _log.error(TAG,"Start Advertising - CATCH Message : " + err.message);
      _log.error(TAG,"Start Advertising - CATCH Details : " + err.details.toString());
    } 

  }
  Future<void> stopAdvertising() async {
    try {
      await _methodChannel.invokeMethod('stopAdvertising');
    } on PlatformException catch(err){
      _log.error(TAG,"Stop Advertising - CATCH ERROR : " + err.toString());
      _log.error(TAG,"Stop Advertising - CATCH Message : " + err.message);
      _log.error(TAG,"Stop Advertising - CATCH Details : " + err.details.toString());
    } 
  }    

  Stream<dynamic> getDetected() {
    return _eventChannel.receiveBroadcastStream().cast<String>();
  }
}
