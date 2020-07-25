import UIKit
import CoreBluetooth
import os

/* new version */

class Peripheral : NSObject {
    
    var peripheralManager: CBPeripheralManager!
    var shouldStartAdvertise: Bool = false
    private var advertiseParams: Dictionary<String, Any>!
    private var dataToBeAdvertised: [String: Array<String>]!
//    private var dataToBeAdvertised: [String: [CBUUID]]!
    private var peripheralSetUp: Bool = false
    private var isInBackground: Bool = false
    
    
    var transferCharacteristic: CBMutableCharacteristic?
    var connectedCentral: CBCentral?
    var dataToSend = Data()
    var sendDataIndex: Int = 0
    
    override init() {
        super.init()
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
    }
    
    public func startAdvertising(params: Dictionary<String, Any>) {
        print("[PERIPHERAL] Start Advertising Called")
        let serviceUuids = params["uuids"] as! Array<String>
        // peripheralManager.startAdvertising([CBAdvertisementDataServiceUUIDsKey: serviceUuids])
        dataToBeAdvertised = [
            CBAdvertisementDataServiceUUIDsKey : serviceUuids,
        ]
//        let cbuuids = serviceUuids.map({ (uuid) -> CBUUID in
//            return CBUUID(string: uuid);
//        })
//        dataToBeAdvertised = [
//            CBAdvertisementDataServiceUUIDsKey : cbuuids,
//        ]
        
        var services: [Dictionary<String, Any>] = []
        for serviceUuid in serviceUuids {
            let service: Dictionary<String, Any> = ["serviceUuid":serviceUuid]
            services.append(service)
        }
        advertiseParams = [
            "services" : services
        ]
        shouldStartAdvertise = true
        peripheralManagerDidUpdateState(peripheralManager)
    }
    
    public func stopAdvertising() {
        print("[PERIPHERAL] STOP ADVERTISING")
        peripheralManager.stopAdvertising()
        let state = UIApplication.shared.applicationState
        if state == .background {
            if !isInBackground {
                print("[PERIPHERAL] App is now in Background - REMOVE SERVICE")
                isInBackground = true;
            }
        } else {
            if isInBackground {
                print("[PERIPHERAL] App is now in Foreground - REMOVE SERVICE")
                isInBackground = false;
            }
        }
    }
    
    // MARK: - Helper Methods
    
    /*
     *  Sends the next amount of data to the connected central
     */
    static var sendingEOM = false
    
    private func sendData() {
        
        guard let transferCharacteristic = transferCharacteristic else {
            return
        }
        
        // First up, check if we're meant to be sending an EOM
        
        if Peripheral.sendingEOM {
            // send it
            let didSend = peripheralManager.updateValue("EOM".data(using: .utf8)!, for: transferCharacteristic, onSubscribedCentrals: nil)
            // Did it send?
            if didSend {
                // It did, so mark it as sent
                Peripheral.sendingEOM = false
                print("[PERIPHERAL] Sent: EOM")
            }
            // It didn't send, so we'll exit and wait for peripheralManagerIsReadyToUpdateSubscribers to call sendData again
            return
        }
        
        // We're not sending an EOM, so we're sending data
        // Is there any left to send?
        if sendDataIndex >= dataToSend.count {
            // No data left.  Do nothing
            return
        }
        
        // There's data left, so send until the callback fails, or we're done.
        var didSend = true
        while didSend {
            
            // Work out how big it should be
            var amountToSend = dataToSend.count - sendDataIndex
            if let mtu = connectedCentral?.maximumUpdateValueLength {
                amountToSend = min(amountToSend, mtu)
            }
            
            // Copy out the data we want
            let chunk = dataToSend.subdata(in: sendDataIndex..<(sendDataIndex + amountToSend))
            
            // Send it
            didSend = peripheralManager.updateValue(chunk, for: transferCharacteristic, onSubscribedCentrals: nil)
            
            // If it didn't work, drop out and wait for the callback
            if !didSend {
                return
            }
            
            let stringFromData = String(data: chunk, encoding: .utf8)
            print("[PERIPHERAL] Sent %d bytes: %s", chunk.count, String(describing: stringFromData))
            
            // It did send, so update our index
            sendDataIndex += amountToSend
            // Was it the last one?
            if sendDataIndex >= dataToSend.count {
                // It was - send an EOM
                
                // Set this so if the send fails, we'll send it next time
                Peripheral.sendingEOM = true
                
                //Send it
                let eomSent = peripheralManager.updateValue("EOM".data(using: .utf8)!,
                                                            for: transferCharacteristic, onSubscribedCentrals: nil)
                
                if eomSent {
                    // It sent; we're all done
                    Peripheral.sendingEOM = false
                    print("[PERIPHERAL] Sent: EOM")
                }
                return
            }
        }
        
    }
    
    private func setupPeripheral() {
        print("[PERIPHERAL] XCODE SETUP PERIPHERAL")
        // Build our service.
        
        if let services = advertiseParams!["services"] as! Array<Dictionary<String,Any>>? {
            for service in services {
                let cbuuid = CBUUID(string:service["serviceUuid"] as! String)
                // Start with the CBMutableCharacteristic.
                let transferCharacteristic = CBMutableCharacteristic(type: TransferService.characteristicUUID,
                                                                     properties: [.notify, .writeWithoutResponse],
                                                                     value: nil,
                                                                     permissions: [.readable, .writeable])
                
                // Create a service from the characteristic.
                let transferService = CBMutableService(type: cbuuid, primary: true)
                
                // Add the characteristic to the service.
                transferService.characteristics = [transferCharacteristic]
                
                // And add it to the peripheral manager.
                peripheralManager.add(transferService)
                
                // Save the characteristic for later.
                // TODO : multiple transfer characteristics
                self.transferCharacteristic = transferCharacteristic
            }
        }
        peripheralSetUp = true;
    }
}


extension Peripheral : CBPeripheralManagerDelegate {
    // implementations of the CBPeripheralManagerDelegate methods
    
    /*
     *  Required protocol method.  A full app should take care of all the possible states,
     *  but we're just waiting for to know when the CBPeripheralManager is ready
     *
     *  Starting from iOS 13.0, if the state is CBManagerStateUnauthorized, you
     *  are also required to check for the authorization state of the peripheral to ensure that
     *  your app is allowed to use bluetooth
     */
    internal func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if (peripheral.state == .poweredOn && shouldStartAdvertise &&  dataToBeAdvertised != nil) {
            print("[PERIPHERAL] Start advertising ...")
            if(!peripheralSetUp) {
                print("[PERIPHERAL] Peripheral SET UP ...")
                setupPeripheral()
            }
            peripheralManager.startAdvertising(dataToBeAdvertised)
            shouldStartAdvertise = false
        } else {
            switch peripheral.state {
            case .poweredOn:
                // ... so start working with the peripheral
                print("[PERIPHERAL] CBManager is powered on")
            // setupPeripheral()
            case .poweredOff:
                print("[PERIPHERAL] CBManager is not powered on")
                // In a real app, you'd deal with all the states accordingly
                return
            case .resetting:
                print("[PERIPHERAL] CBManager is resetting")
                // In a real app, you'd deal with all the states accordingly
                return
            case .unauthorized:
                // In a real app, you'd deal with all the states accordingly
                if #available(iOS 13.0, *) {
                    switch peripheral.authorization {
                    case .denied:
                        print("[PERIPHERAL] You are not authorized to use Bluetooth")
                    case .restricted:
                        print("[PERIPHERAL] Bluetooth is restricted")
                    default:
                        print("[PERIPHERAL] Unexpected authorization")
                    }
                } else {
                    // Fallback on earlier versions
                }
                return
            case .unknown:
                print("[PERIPHERAL] CBManager state is unknown")
                // In a real app, you'd deal with all the states accordingly
                return
            case .unsupported:
                print("[PERIPHERAL] Bluetooth is not supported on this device")
                // In a real app, you'd deal with all the states accordingly
                return
            @unknown default:
                print("[PERIPHERAL] A previously unknown peripheral manager state occurred")
                // In a real app, you'd deal with yet unknown cases that might occur in the future
                return
            }
            
        }
        //// advertisingSwitch.isEnabled = peripheral.state == .poweredOn
        
    }
    
    /*
     *  Catch when someone subscribes to our characteristic, then start sending them data
     */
    func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
        print("[PERIPHERAL] Central subscribed to characteristic")
        
        // Get the data
        dataToSend = "Hello Hank from PERIPHERAL!".data(using: .utf8)!
        
        // Reset the index
        sendDataIndex = 0
        
        // save central
        connectedCentral = central
        
        // Start sending
        sendData()
    }
    
    /*
     *  Recognize when the central unsubscribes
     */
    func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFrom characteristic: CBCharacteristic) {
        print("[PERIPHERAL] Central unsubscribed from characteristic")
        connectedCentral = nil
    }
    
    /*
     *  This callback comes in when the PeripheralManager is ready to send the next chunk of data.
     *  This is to ensure that packets will arrive in the order they are sent
     */
    func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager) {
        // Start sending again
        sendData()
    }
    
    /*
     * This callback comes in when the PeripheralManager received write to characteristics
     */
    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        for aRequest in requests {
            guard let requestValue = aRequest.value,
                let stringFromData = String(data: requestValue, encoding: .utf8) else {
                    continue
            }
            
            print("[PERIPHERAL] Received write request of %d bytes: %s", requestValue.count, stringFromData)
            // self.textView.text = stringFromData
        }
    }
}
