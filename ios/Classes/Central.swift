import UIKit
import CoreBluetooth
import os

/* SCANNING FOR PERIPHERALS new version */

class Central : NSObject {
    var centralManager: CBCentralManager!
    private var shouldStartScanning: Bool = false
    private var scanParams: Dictionary<String, Any>!
    private var cbuuids: Array<CBUUID>?
//    private var discoveredPeripherals: Array<CBPeripheral> = []
    
    private var currentlyDiscoveredPeripheral: CBPeripheral?
    
    var transferCharacteristic: CBCharacteristic?
    var writeIterationsComplete = 0
    var connectionIterationsComplete = 0
    
    let defaultIterations = 5     // change this value based on test usecase
    
    var data = Data()
    
    /*
     override init() {
     super.init()
     print("XCODE CENTRAL INIT")
     centralManager = CBCentralManager(delegate: self, queue: nil, options:[CBCentralManagerOptionRestoreIdentifierKey: "roktok.immu.dev"])
     }
     */
    
    public func startScanning(params: Dictionary<String, Any>) {
        print("[CENTRAL] Now Scanning... $s", String(describing: params["uuids"]))
        let serviceUuids = params["uuids"] as! Array<String>
        self.cbuuids = serviceUuids.map({ (uuid) -> CBUUID in
            return CBUUID(string: uuid);
        })
        
        shouldStartScanning = true
        if(centralManager == nil) {
            centralManager = CBCentralManager(delegate: self, queue: nil)
                //    options:[CBCentralManagerOptionRestoreIdentifierKey: "roktok.immu.dev"])
                
        }
        centralManagerDidUpdateState(centralManager)
    }
    
    public func stopScanning() {
        shouldStartScanning = false
        if (centralManager != nil) {
            print("[CENTRAL] Stop Scanning...")
            centralManager.stopScan()
            // we clean up all connections for getting ready to re-detect peripherals in the next round
            cleanup()
            // onAdvertisingStateChanged!(false)
        } else {
            print("[CENTRAL] Cannot stop because centralManager is nil")
        }
        
    }
    
    
    
    
    
    
    
    
    /*
     * We will first check if we are already connected to our counterpart
     * Otherwise, scan for peripherals - specifically for our service's 128bit CBUUID
     */
    private func retrievePeripheral() {
        
        let connectedPeripherals: [CBPeripheral] = (centralManager.retrieveConnectedPeripherals(withServices: cbuuids!))
        
        print("[CENTRAL] Found connected Peripherals : %@", connectedPeripherals)
        
        // we first consider peripherals that are connected
        // then we start the process which ends with disconnecting this particular peripheral
        // and coming back here for taking the next connected peripheral until all habe been
        // completed. Then we scan for new peripherals.
        
        if let connectedPeripheral = connectedPeripherals.last {
            print("[CENTRAL] Connecting to peripheral %@", connectedPeripheral)
            self.currentlyDiscoveredPeripheral = connectedPeripheral
            // self.discoveredPeripherals.append(connectedPeripheral)
            centralManager.connect(connectedPeripheral, options: nil)
        } else if currentlyDiscoveredPeripheral != nil {
            centralManager.connect(currentlyDiscoveredPeripheral!, options: nil)
        } else {
            print("[CENTRAL] call scanForPeripherals : %s", String(describing: cbuuids))
            // All connected peripherals have been completed. Now start scanning.
            centralManager.scanForPeripherals(withServices: cbuuids,
                                              options: [
                                                CBCentralManagerScanOptionAllowDuplicatesKey: true,
                                                ])
        }
    }
    
    /*
     *  Call this when things either go wrong, or you're done with the connection.
     *  This cancels any subscriptions if there are any, or straight disconnects if not.
     *  (didUpdateNotificationStateForCharacteristic will cancel the connection if a subscription is involved)
     */
    
    /*
    private func cleanup() {
        print("[CENTRAL] cleanup called")
        // Don't do anything if we're not connected
        for discoveredPeripheral in discoveredPeripherals {
            if(discoveredPeripheral.state != .connected) { return }
            
            for service in (discoveredPeripheral.services ?? [] as [CBService]) {
                for characteristic in (service.characteristics ?? [] as [CBCharacteristic]) {
                    if characteristic.uuid == TransferService.characteristicUUID && characteristic.isNotifying {
                        // It is notifying, so unsubscribe
                        // self.discoveredPeripheral?.setNotifyValue(false, for: characteristic)
                        discoveredPeripheral.setNotifyValue(false, for: characteristic)
                    }
                }
            }
            
            // If we've gotten this far, we're connected, but we're not subscribed, so we just disconnect
            centralManager.cancelPeripheralConnection(discoveredPeripheral)
        }
    }
    */
    private func cleanup() {
        // Don't do anything if we're not connected
        guard let currentlyDiscoveredPeripheral = currentlyDiscoveredPeripheral,
            case .connected = currentlyDiscoveredPeripheral.state else { return }
        
        for service in (currentlyDiscoveredPeripheral.services ?? [] as [CBService]) {
            for characteristic in (service.characteristics ?? [] as [CBCharacteristic]) {
                if characteristic.uuid == TransferService.characteristicUUID_A && characteristic.isNotifying {
                    // It is notifying, so unsubscribe
                    self.currentlyDiscoveredPeripheral?.setNotifyValue(false, for: characteristic)
                }
            }
        }
        
        // If we've gotten this far, we're connected, but we're not subscribed, so we just disconnect
        centralManager.cancelPeripheralConnection(currentlyDiscoveredPeripheral)
    }
    /*
     *  Write some test data to peripheral
     */
    private func writeData(peripheral: CBPeripheral) {
        
        //        guard let discoveredPeripheral = discoveredPeripheral,
        //                let transferCharacteristic = transferCharacteristic
        //            else { return }
        
        // check to see if number of iterations completed and peripheral can accept more data
        
        if #available(iOS 11.0, *) {
            while writeIterationsComplete < defaultIterations && peripheral.canSendWriteWithoutResponse {
                
                let mtu = peripheral.maximumWriteValueLength (for: .withoutResponse)
                var rawPacket = [UInt8]()
                
                let bytesToCopy: size_t = min(mtu, data.count)
                data.copyBytes(to: &rawPacket, count: bytesToCopy)
                let packetData = Data(bytes: &rawPacket, count: bytesToCopy)
                
                let stringFromData = String(data: packetData, encoding: .utf8)
                print("[CENTRAL] Writing %d bytes: %s", bytesToCopy, String(describing: stringFromData))
                
                peripheral.writeValue(packetData, for: transferCharacteristic!, type: .withoutResponse)
                writeIterationsComplete += 1
            }
        } else {
            print("[CENTRAL] writeIterationsComplete Fallback missing for < iOS 11.0")
        }
        
        
        if writeIterationsComplete == defaultIterations {
            // Cancel our subscription to the characteristic
            peripheral.setNotifyValue(false, for: transferCharacteristic!)
        }
    }
}

extension Central : CBCentralManagerDelegate {
    // implementations of the CBCentralManagerDelegate methods
    
    /*
     *  centralManagerDidUpdateState is a required protocol method.
     *  Usually, you'd check for other states to make sure the current device supports LE, is powered on, etc.
     *  In this instance, we're just using it to wait for CBCentralManagerStatePoweredOn, which indicates
     *  the Central is ready to be used.
     */
    internal func centralManagerDidUpdateState(_ central: CBCentralManager) {
        
        if (central.state == .poweredOn && shouldStartScanning && cbuuids != nil) {
            print("[CENTRAL] CBManager Central is powered on - START SCANNING $s", String(describing: cbuuids))
            retrievePeripheral()

//            centralManager.scanForPeripherals(withServices: cbuuids!,
//                                              options: [
//                                                CBCentralManagerScanOptionAllowDuplicatesKey: true,
//                                                CBCentralManagerScanOptionSolicitedServiceUUIDsKey: cbuuids!
//            ])
            // shouldStartScanning = false
        } else {
            print("[CENTRAL] CENTRAL STATE IS NOT POWERED ON")
            switch central.state {
            case .poweredOn:
                // ... so start working with the peripheral
                print("[CENTRAL] CBManager Central is powered on")
                // retrievePeripheral()
            // startScanning()
            case .poweredOff:
                print("[CENTRAL] CBManager is not powered on")
                // In a real app, you'd deal with all the states accordingly
                return
            case .resetting:
                print("[CENTRAL] CBManager is resetting")
                // In a real app, you'd deal with all the states accordingly
                return
            case .unauthorized:
                // In a real app, you'd deal with all the states accordingly
                if #available(iOS 13.0, *) {
                    switch central.authorization {
                    case .denied:
                        print("[CENTRAL] You are not authorized to use Bluetooth")
                    case .restricted:
                        print("[CENTRAL] Bluetooth is restricted")
                    default:
                        print("[CENTRAL] Unexpected authorization")
                    }
                } else {
                    // Fallback on earlier versions
                }
                return
            case .unknown:
                print("[CENTRAL] CBManager state is unknown")
                // In a real app, you'd deal with all the states accordingly
                return
            case .unsupported:
                print("[CENTRAL] Bluetooth is not supported on this device")
                // In a real app, you'd deal with all the states accordingly
                return
            @unknown default:
                print("[CENTRAL] A previously unknown central manager state occurred")
                // In a real app, you'd deal with yet unknown cases that might occur in the future
                return
            }
            
        }
        
        
        
        
        
    }
    
    /*
     *  This callback comes whenever a peripheral that is advertising the transfer serviceUUID is discovered.
     *  We check the RSSI, to make sure it's close enough that we're interested in it, and if it is,
     *  we start the connection process
     */
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                        advertisementData: [String: Any], rssi RSSI: NSNumber) {
        
        // Reject if the signal strength is too low to attempt data transfer.
        // Change the minimum RSSI value depending on your app’s use case.
        //        guard RSSI.intValue >= -50
        //            else {
        //                print("Discovered perhiperal not in expected range, at %d", RSSI.intValue)
        //                return
        //        }
        
        // Hier wollen wir erstmal nach ServiceUUIDs filtern
        // print("[CENTRAL] Discovered %s at %d", String(describing: peripheral.name), RSSI.intValue)
        // Device is in range - have we already seen it?
        print("[CENTRAL] peripheral discovered : %@", peripheral)
        // Device is in range - have we already seen it?
        if currentlyDiscoveredPeripheral != peripheral {
            // Save a local copy of the peripheral, so CoreBluetooth doesn't get rid of it.
            currentlyDiscoveredPeripheral = peripheral
            // And finally, connect to the peripheral.
            print("Connecting to perhiperal %@", peripheral)
            centralManager.connect(peripheral, options: nil)
        }
        
        
        /*
              
        // first we check if the peripheral already has been registered
        if !discoveredPeripherals.contains(peripheral) {
            // if not, we check for the serviceUuids we are interrested in
            var detectedServiceUUIDS: Array<String>! = [];
            var isInBackground: Bool = false;
            
            // first we check for serviceUUIDs in the overflow area
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
                // we found something. Let's register the peripheral now
                print("[CENTRAL] detectedServiceIds $s - $s", String(describing: detectedServiceUUIDS), String(describing: isInBackground))
                
                
                // Save a local copy of the peripheral, so CoreBluetooth doesn't get rid of it.
                discoveredPeripherals.append(peripheral)
                // And finally, connect to the peripheral.
                print("[CENTRAL] Connecting to perhiperal %s : $@", String(describing: peripheral.name), peripheral)
                centralManager.connect(peripheral, options: nil)
                
                //            let jsonObject: [String: Any?] = [
                //                "id": peripheral.identifier.uuidString,
                //                "name": peripheral.name,
                //                "rssi": RSSI,
                //                "txLevel": advertisementData[CBAdvertisementDataTxPowerLevelKey],
                //                "isInBackground" : isInBackground,
                //                //                 "detectedServiceUUIDs" : detectedServiceUUIDS
                //            ]
                //
                //            peripherals[peripheral.identifier.uuidString] = jsonObject;
            }
        }
         */
    }
    
//    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String : Any]) {
//        print("[CENTRAL]  CBCentralManager willRestoreState called")
//    }
    
    
    /*
     *  If the connection fails for whatever reason, we need to deal with it.
     */
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("[CENTRAL] Failed to connect to %@. %s", peripheral, String(describing: error))
        cleanup()
    }
    
    /*
     *  We've connected to the peripheral, now we need to discover the services and characteristics to find the 'transfer' characteristic.
     */
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("[CENTRAL] Peripheral Connected")
        
        // we do not stop scan here since we are interrested in more than one peripheral
        centralManager.stopScan()
        print("[CENTRAL] Scanning stopped")
        
        // set iteration info
        connectionIterationsComplete += 1
        writeIterationsComplete = 0
        
        // Clear the data that we may already have
        data.removeAll(keepingCapacity: false)
        
        // Make sure we get the discovery callbacks
        peripheral.delegate = self
        
        // Search only for services that match our UUID
        peripheral.discoverServices(cbuuids!)
        
    }
    
    /*
     *  Once the disconnection happens, we need to clean up our local copy of the peripheral
     */
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        print("[CENTRAL] Perhiperal Disconnected")
        // we keep the discovered  peripheral
        // currentlyDiscoveredPeripheral = nil
        
        // discoveredPeripherals = discoveredPeripherals.filter {$0 != peripheral}
        
        // We're disconnected. If stopScanning has not been called yet, start scanning again
        if shouldStartScanning {
        // if connectionIterationsComplete < defaultIterations {
            retrievePeripheral()
        }
        // else {
        //            print("Connection iterations completed")
        // }
    }
}


extension Central: CBPeripheralDelegate {
    // implementations of the CBPeripheralDelegate methods
    
    /*
     *  The peripheral letting us know when services have been invalidated.
     */
    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        
        for service in invalidatedServices where service.uuid == TransferService.serviceUUID {
            print("[CENTRAL] Transfer service is invalidated - rediscover services")
            peripheral.discoverServices([TransferService.serviceUUID] + cbuuids!)
        }
    }
    
    /*
     *  The Transfer Service was discovered
     */
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error {
            print("[CENTRAL] Error discovering services")
            cleanup()
            return
        }
        
        // Discover the characteristic we want...
        
        // Loop through the newly filled peripheral.services array, just in case there's more than one.
        guard let peripheralServices = peripheral.services else { return }
        
        print("[CENTRAL] Service discovered")
        for service in peripheralServices {
            print("[CENTRAL] Service discovered : " + service.uuid.uuidString)
            
            peripheral.discoverCharacteristics([TransferService.characteristicUUID_B], for: service)
        }
    }
    
    /*
     *  The Transfer characteristic was discovered.
     *  Once this has been found, we want to subscribe to it, which lets the peripheral know we want the data it contains
     */
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        // Deal with errors (if any).
        if let error = error {
            print("[CENTRAL] Error discovering characteristics")
            cleanup()
            return
        }
        
        // Again, we loop through the array, just in case and check if it's the right one
        guard let serviceCharacteristics = service.characteristics else { return }
        for characteristic in serviceCharacteristics where characteristic.uuid == TransferService.characteristicUUID_A {
            // If it is, subscribe to it
            transferCharacteristic = characteristic
            peripheral.setNotifyValue(true, for: characteristic)
        }
        
        // Once this is complete, we just need to wait for the data to come in.
    }
    
    /*
     *   This callback lets us know more data has arrived via notification on the characteristic
     */
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        // Deal with errors (if any)
        if let error = error {
            print("[CENTRAL] Error discovering characteristics: ")
            cleanup()
            return
        }
        
        guard let characteristicData = characteristic.value,
            let stringFromData = String(data: characteristicData, encoding: .utf8) else { return }
        
        print("[CENTRAL] Received data: " +  stringFromData)
        
        // Have we received the end-of-message token?
        if stringFromData == "EOM" {
            // End-of-message case: show the data.
            // Dispatch the text view update to the main queue for updating the UI, because
            // we don't know which thread this method will be called back on.
            // // DispatchQueue.main.async() {
            // //     self.textView.text = String(data: self.data, encoding: .utf8)
            // // }
            
            // Write test data
            writeData(peripheral: peripheral)
        } else {
            // Otherwise, just append the data to what we have previously received.
            data.append(characteristicData)
        }
    }
    
    /*
     *  The peripheral letting us know whether our subscribe/unsubscribe happened or not
     */
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        // Deal with errors (if any)
        if let error = error {
            print("[CENTRAL] Error changing notification state:")
            return
        }
        
        // Exit if it's not the transfer characteristic
        guard characteristic.uuid == TransferService.characteristicUUID_A else { return }
        
        if characteristic.isNotifying {
            // Notification has started
            print("[CENTRAL] Notification began on ")
        } else {
            // Notification has stopped, so disconnect from the peripheral
            print("[CENTRAL] Notification stopped on ")
            cleanup()
        }
        
    }
    
    /*
     *  This is called when peripheral is ready to accept more data when using write without response
     */
    func peripheralIsReady(toSendWriteWithoutResponse peripheral: CBPeripheral) {
        print("[CENTRAL] Peripheral is ready, send data")
        writeData(peripheral: peripheral)
    }
    
}




