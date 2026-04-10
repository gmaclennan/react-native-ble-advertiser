package app.comapeo.bleadvertiser

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.interfaces.permissions.Permissions
import java.util.UUID

class AdvertiseDataRecord : Record {
  @Field
  var serviceUuids: List<String>? = null

  @Field
  var includeDeviceName: Boolean? = null

  @Field
  var manufacturerData: List<HashMap<String, Any>>? = null

  @Field
  var serviceData: List<HashMap<String, Any>>? = null

  @Field
  var includeTxPowerLevel: Boolean? = null
}

class AdvertiseSettingsRecord : Record {
  @Field
  var mode: String? = null

  @Field
  var txPowerLevel: String? = null

  @Field
  var connectable: Boolean? = null

  @Field
  var timeout: Int? = null
}

class ReactNativeBleAdvertiserModule : Module() {
  private var advertiseCallback: AdvertiseCallback? = null

  private val bluetoothAdapter: BluetoothAdapter?
    get() {
      val context = appContext.reactContext ?: return null
      val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      return manager?.adapter
    }

  private val advertiser: BluetoothLeAdvertiser?
    get() = bluetoothAdapter?.bluetoothLeAdvertiser

  private val permissionsManager: Permissions
    get() = appContext.permissions ?: throw CodedException("ERR_PERMISSIONS", "Permissions module not found", null)

  override fun definition() = ModuleDefinition {
    Name("ReactNativeBleAdvertiser")

    Function("isSupported") {
      bluetoothAdapter?.isMultipleAdvertisementSupported == true
    }

    AsyncFunction("getPermissionsAsync") { promise: Promise ->
      Permissions.getPermissionsWithPermissionsManager(
        permissionsManager,
        promise,
        *getRequiredPermissions()
      )
    }

    AsyncFunction("requestPermissionsAsync") { promise: Promise ->
      Permissions.askForPermissionsWithPermissionsManager(
        permissionsManager,
        promise,
        *getRequiredPermissions()
      )
    }

    AsyncFunction("startAdvertising") { advertiseData: AdvertiseDataRecord, scanResponseData: AdvertiseDataRecord?, settings: AdvertiseSettingsRecord?, promise: Promise ->
      val currentAdvertiser = advertiser
      if (currentAdvertiser == null) {
        promise.reject(CodedException("ERR_BLE_NOT_AVAILABLE", "BLE advertising is not available on this device", null))
        return@AsyncFunction
      }

      if (advertiseCallback != null) {
        promise.reject(CodedException("ERR_BLE_ALREADY_ADVERTISING", "Already advertising. Call stopAdvertising() first.", null))
        return@AsyncFunction
      }

      val builtSettings = buildAdvertiseSettings(settings)
      val builtData = buildAdvertiseData(advertiseData)
      val builtScanResponse = scanResponseData?.let { buildAdvertiseData(it) }

      val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
          advertiseCallback = this
          promise.resolve(null)
        }

        override fun onStartFailure(errorCode: Int) {
          advertiseCallback = null
          val message = when (errorCode) {
            ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertise data too large"
            ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
            ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
            ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
            else -> "Unknown error"
          }
          promise.reject(CodedException("ERR_BLE_ADVERTISE_FAILED", "$message (code: $errorCode)", null))
        }
      }

      if (builtScanResponse != null) {
        currentAdvertiser.startAdvertising(builtSettings, builtData, builtScanResponse, callback)
      } else {
        currentAdvertiser.startAdvertising(builtSettings, builtData, callback)
      }
    }

    Function("stopAdvertising") {
      val cb = advertiseCallback
      if (cb != null) {
        advertiser?.stopAdvertising(cb)
        advertiseCallback = null
      }
    }

    OnDestroy {
      val cb = advertiseCallback
      if (cb != null) {
        advertiser?.stopAdvertising(cb)
        advertiseCallback = null
      }
    }
  }

  private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
      arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }
  }

  private fun buildAdvertiseSettings(settings: AdvertiseSettingsRecord?): AdvertiseSettings {
    val builder = AdvertiseSettings.Builder()

    val mode = when (settings?.mode) {
      "balanced" -> AdvertiseSettings.ADVERTISE_MODE_BALANCED
      "lowLatency" -> AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
      else -> AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
    }
    builder.setAdvertiseMode(mode)

    val txPower = when (settings?.txPowerLevel) {
      "ultraLow" -> AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW
      "medium" -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
      "high" -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
      else -> AdvertiseSettings.ADVERTISE_TX_POWER_LOW
    }
    builder.setTxPowerLevel(txPower)

    builder.setConnectable(settings?.connectable ?: false)
    builder.setTimeout(settings?.timeout ?: 0)

    return builder.build()
  }

  @Suppress("UNCHECKED_CAST")
  private fun buildAdvertiseData(data: AdvertiseDataRecord): AdvertiseData {
    val builder = AdvertiseData.Builder()

    builder.setIncludeDeviceName(data.includeDeviceName ?: false)
    builder.setIncludeTxPowerLevel(data.includeTxPowerLevel ?: false)

    data.serviceUuids?.forEach { uuid ->
      builder.addServiceUuid(ParcelUuid(UUID.fromString(uuid)))
    }

    data.manufacturerData?.forEach { entry ->
      val id = (entry["id"] as Number).toInt()
      val bytes = toByteArray(entry["data"])
      builder.addManufacturerData(id, bytes)
    }

    data.serviceData?.forEach { entry ->
      val uuid = entry["uuid"] as String
      val bytes = toByteArray(entry["data"])
      builder.addServiceData(ParcelUuid(UUID.fromString(uuid)), bytes)
    }

    return builder.build()
  }

  private fun toByteArray(value: Any?): ByteArray {
    return when (value) {
      is ByteArray -> value
      is List<*> -> ByteArray(value.size) { (value[it] as Number).toByte() }
      else -> ByteArray(0)
    }
  }
}
