// https://developer.apple.com/forums/thread/104065
// https://medium.com/@shu223/core-bluetooth-snippets-with-swift-9be8524600b2

import Flutter
import UIKit
import CoreBluetooth

public class SwiftScannerPlugin: NSObject, FlutterPlugin, FlutterStreamHandler {
    
    // private var peripheral = Peripheral()
    private var central = Central()


    // MARK: - Private properties
    private var cbuuids: Array<CBUUID>?
    private var eventSink: FlutterEventSink?

    public static func register(with registrar: FlutterPluginRegistrar) {
        let instance = SwiftScannerPlugin()
        let methodChannel = FlutterMethodChannel(name: "roktok.immu.dev/bluetoothScanner",
                                                 binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "roktok.immu.dev/bluetoothScannerResponse",
                                               binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(instance)
        registrar.addMethodCallDelegate(instance, channel: methodChannel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch (call.method) {
        case "startScanning":
            startScanning(call, result)
        case "stopScanning":
            stopScanning(call, result)
        /*
        case "startAdvertising":
            startAdvertising(call, result)
        case "stopAdvertising":
            stopAdvertising(call, result)
        */
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    public func onListen(withArguments arguments: Any?,
                         eventSink: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = eventSink
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        eventSink = nil
        return nil
    }

    func startScanning(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        // let uuids = call.arguments as! Array<String>
        let params = (call.arguments as? Dictionary<String, Any>)!
        // let uuids = map?["uuids"] as! Array<String>
        print("SWIFT SCANNER PLUGIN - START SCANNING CALLED")
        central.startScanning(params: params);
        result(nil)
    }
    func stopScanning(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        central.stopScanning();
        result(nil)
    }
    
    /*
    func startAdvertising(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        peripheral.startAdvertising();
        result(nil)
    }
    func stopAdvertising(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        peripheral.stopAdvertising();
        result(nil)
    }
     */
}
