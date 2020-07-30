/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Transfer service and characteristics UUIDs
*/

import Foundation
import CoreBluetooth

struct TransferService {
	static let serviceUUID = CBUUID(string: "0000FF01-0000-1000-8000-00805F9B34FB")
	static let characteristicUUID_A = CBUUID(string: "08590F7E-DB05-467E-8757-72F6FAEB13D4")
    static let characteristicUUID_B = CBUUID(string: "E20A39F4-73F5-4BC4-A12F-17D1AD07A961")
}
