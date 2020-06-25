// https://developer.apple.com/forums/thread/104065
// https://medium.com/@shu223/core-bluetooth-snippets-with-swift-9be8524600b2

import Flutter
import UIKit
import CoreBluetooth

public class SwiftScannerPlugin: NSObject, FlutterPlugin, FlutterStreamHandler,
CBCentralManagerDelegate {
    
    // MARK: - Private properties
    private var centralManager: CBCentralManager?
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
            startScan(call, result)
        case "stopScanning":
            stopScan(call, result)
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
    func stopScan(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        print("XCODE : STOP SCAN CALLED");
        if #available(iOS 9.0, *) {
            if (centralManager?.isScanning == true) {
                centralManager?.stopScan()
            }
        } else {
            // Fallback on earlier versions
            centralManager?.stopScan()
        }
        peripherals.removeAll()
        // now disconnect
        for peripheral in connectedPeripherals {
            disconnect(peripheral: peripheral);
        }
        
        result(nil)
    }
    
    func startScan(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        // let uuids = call.arguments as! Array<String>
        let map = call.arguments as? Dictionary<String, Any>
        let uuids = map?["uuids"] as! Array<String>
        print("XCODE start scan called with \(String(describing: uuids))")
        
        self.cbuuids = uuids.map({ (uuid) -> CBUUID in
            return CBUUID(string: uuid);
        })
        // turn scanning on
        centralManager = CBCentralManager(delegate: self, queue: nil,                                           options:[CBCentralManagerOptionRestoreIdentifierKey: "roktok.immu.dev"])
        result(nil)
    }
    public func disconnect(peripheral: CBPeripheral) {
        centralManager?.cancelPeripheralConnection(peripheral)
    }
    
    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        connectedPeripherals.remove(peripheral)
    }
    
    public func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        connectedPeripherals.remove(peripheral)
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        peripheral.discoverServices(nil)
    }
    
    
    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        
        if(peripheral.state == CBPeripheralState.disconnected) {
            peripheral.delegate = self
            connectedPeripherals.insert(peripheral)
            central.connect(peripheral, options: nil)
        }
        
        /*
        // var detectedServiceUUID: String!;
        var detectedServiceUUIDS: Array<String>! = [];
        var isInBackground: Bool = false;
        
        // first check for serviceUUIDs in the overflow area
        if advertisementData[CBAdvertisementDataOverflowServiceUUIDsKey] != nil {
            
            if let overflowServiceUUIDs: [CBUUID?] = advertisementData[CBAdvertisementDataOverflowServiceUUIDsKey]!  as? [CBUUID?] {
                for uuid in overflowServiceUUIDs {
                    if self.cbuuids!.contains(uuid!) {
                        
                        detectedServiceUUIDS.append(uuid!.uuidString)
                        
                        isInBackground = true
                    }
                }
            }
        }
        
        // if nothing has been detected yet, check the normal serviceUUIDs
        if detectedServiceUUIDS.count == 0 {
            if let serviceUUIDs: [CBUUID?] = advertisementData[CBAdvertisementDataServiceUUIDsKey]  as? [CBUUID?] {
                for uuid in serviceUUIDs {
                    if self.cbuuids!.contains(uuid!) {
                        detectedServiceUUIDS.append(uuid!.uuidString)
                        isInBackground = false
                    }
                }
            }
        }
        
        if detectedServiceUUIDS.count != 0 {
            let jsonObject: [String: Any?] = [
                "id": peripheral.identifier.uuidString,
                "name": peripheral.name,
                "rssi": RSSI,
                "txLevel": advertisementData[CBAdvertisementDataTxPowerLevelKey],
                "isInBackground" : isInBackground,
                // "detectedServiceUUIDs" : detectedServiceUUIDS
            ]
            
            peripherals[peripheral.identifier.uuidString] = jsonObject;
        }
         */
        
    }
    
    public func centralManager(_ central: CBCentralManager, willRestoreState dict: [String : Any]) {
        
    }
    
    open func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            // print("XCODE CM State is poweredON")
            central.scanForPeripherals(withServices: cbuuids, options: [CBCentralManagerScanOptionAllowDuplicatesKey: true, CBCentralManagerScanOptionSolicitedServiceUUIDsKey: cbuuids!])
        case .poweredOff:
            // print("XCODE CM State is poweredOFF")
            central.stopScan()
        case .unsupported: print("XCODE Unsupported BLE module")
        default: break
        }
    }
    
    private(set) var connectedPeripherals = Set<CBPeripheral>()
    private(set) var peripherals = Dictionary<String, [String: Any?]>()
}

extension SwiftScannerPlugin: CBPeripheralDelegate {
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        var detectedServiceUUIDs: Array<String> = []
        for service in peripheral.services ?? [] {
            if self.cbuuids!.contains(service.uuid) {
                detectedServiceUUIDs.append(service.uuid.uuidString);
            }
        }
        var obj = peripherals[peripheral.identifier.uuidString]
        obj?["detectedServiceUUIDs"] = detectedServiceUUIDs
        print("XCODE Service detected :")
        print(detectedServiceUUIDs);
        
        // send discovered data back to Flutter
        /*
        if (self.eventSink != nil) {
            do {
                if(peripherals.count != 0) {
                    let jsonData = try JSONSerialization.data(withJSONObject: obj!, options: .prettyPrinted)
                    
                    let jsonString = String(data: jsonData, encoding: String.Encoding.ascii)!
                    self.eventSink!(jsonString)
                    
                }
            } catch {
                print(error.localizedDescription)
            }
        }
        */
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
      print("Peripheral services changed...")
      peripheral.discoverServices(nil)
    }
    
    
    
    
    /*
     public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
     guard error == nil else {
     print("Error discovering characteristics...")
     return
     }
     
     guard let characteristics = service.characteristics, !characteristics.isEmpty else {
     print("No characteristics found for service...")
     return
     }
     
     for characteristic in service.characteristics ?? [] {
     // Subscribe to characteristic changes, etc...
     }
     }
     */
}
