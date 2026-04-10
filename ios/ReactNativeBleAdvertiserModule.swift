import ExpoModulesCore

public class ReactNativeBleAdvertiserModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ReactNativeBleAdvertiser")

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
  }
}
