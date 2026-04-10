import ExpoModulesCore

public class ReactNativeBleAdvertiserModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ReactNativeBleAdvertiser")

    Events(
      "onConnectionStateChange",
      "onCharacteristicReadRequest",
      "onCharacteristicWriteRequest",
      "onNotificationSent"
    )

    Function("isSupported") {
      return false
    }

    AsyncFunction("getPermissionsAsync") { (promise: Promise) in
      promise.reject(
        "ERR_UNSUPPORTED_PLATFORM",
        "BLE advertising is not yet supported on iOS"
      )
    }

    AsyncFunction("requestPermissionsAsync") { (promise: Promise) in
      promise.reject(
        "ERR_UNSUPPORTED_PLATFORM",
        "BLE advertising is not yet supported on iOS"
      )
    }

    AsyncFunction("startAdvertising") { (advertiseData: [String: Any], scanResponseData: [String: Any]?, settings: [String: Any]?, promise: Promise) in
      promise.reject(
        "ERR_UNSUPPORTED_PLATFORM",
        "BLE advertising is not yet supported on iOS"
      )
    }

    Function("stopAdvertising") {
      // no-op on iOS
    }

    // --- GATT Server ---

    AsyncFunction("startGattServer") { (promise: Promise) in
      promise.reject(
        "ERR_UNSUPPORTED_PLATFORM",
        "GATT server is not yet supported on iOS"
      )
    }

    Function("stopGattServer") {
      // no-op on iOS
    }

    AsyncFunction("addService") { (service: [String: Any], promise: Promise) in
      promise.reject(
        "ERR_UNSUPPORTED_PLATFORM",
        "GATT server is not yet supported on iOS"
      )
    }

    Function("removeService") { (serviceUuid: String) in
      // no-op on iOS
    }

    Function("sendResponse") { (requestId: Int, status: Int, offset: Int, value: [Int]?) in
      // no-op on iOS
    }

    Function("notifyCharacteristicChanged") { (serviceUuid: String, characteristicUuid: String, value: [Int], confirm: Bool) in
      // no-op on iOS
    }
  }
}
