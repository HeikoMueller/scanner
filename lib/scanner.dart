import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

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
  static const TAG = "[Scanner Plugin]";
  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;

  Future<void> startScanning({@required Map params}) async {
    try {
      await _methodChannel.invokeMethod('startScanning', params);
    } on PlatformException catch(err){
      print(err.toString());
    } 
  }
  Future<void> stopScanning() async {
    try {
      await _methodChannel.invokeMethod('stopScanning');
    } on PlatformException catch(err){
      print(err.toString());
    } 
  }
  //Future<void> startAdvertising({@required String serviceUUID, @required String characteristicUUID, @required String characteristicValue}) async {
  Future<void> startAdvertising({@required Map params}) async {
    try {
      await _methodChannel.invokeMethod('startAdvertising', params);
    } on PlatformException catch(err){
      print(err.toString());
    } 

  }
  Future<void> stopAdvertising() async {
    try {
      await _methodChannel.invokeMethod('stopAdvertising');
    } on PlatformException catch(err){
      print(err.toString());
    } 
  }    

  Stream<dynamic> getDetected() {
    return _eventChannel.receiveBroadcastStream().cast<String>();
  }
}
