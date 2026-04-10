package app.comapeo.bleadvertiser

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import java.util.concurrent.ConcurrentHashMap

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

class GattCharacteristicRecord : Record {
  @Field
  var uuid: String = ""

  @Field
  var properties: List<String> = emptyList()

  @Field
  var permissions: List<String> = emptyList()

  @Field
  var value: List<Int>? = null
}

class GattServiceRecord : Record {
  @Field
  var uuid: String = ""

  @Field
  var characteristics: List<GattCharacteristicRecord> = emptyList()
}

class ReactNativeBleAdvertiserModule : Module() {
  companion object {
    private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
  }

  private var advertiseCallback: AdvertiseCallback? = null

  // GATT server state
  private var gattServer: BluetoothGattServer? = null
  private val connectedDevices = ConcurrentHashMap<String, BluetoothDevice>()
  private val requestDevices = ConcurrentHashMap<Int, BluetoothDevice>()
  private val subscribedDevices = ConcurrentHashMap<String, MutableSet<String>>()
  private var addServicePromise: Promise? = null

  private val bluetoothManager: BluetoothManager?
    get() {
      val context = appContext.reactContext ?: return null
      return context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

  private val bluetoothAdapter: BluetoothAdapter?
    get() = bluetoothManager?.adapter

  private val advertiser: BluetoothLeAdvertiser?
    get() = bluetoothAdapter?.bluetoothLeAdvertiser

  private val permissionsManager: Permissions
    get() = appContext.permissions ?: throw CodedException("ERR_PERMISSIONS", "Permissions module not found", null)

  @SuppressLint("MissingPermission")
  private val gattServerCallback = object : BluetoothGattServerCallback() {
    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        connectedDevices[device.address] = device
        sendEvent("onConnectionStateChange", mapOf(
          "deviceAddress" to device.address,
          "connected" to true
        ))
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        connectedDevices.remove(device.address)
        subscribedDevices.values.forEach { it.remove(device.address) }
        sendEvent("onConnectionStateChange", mapOf(
          "deviceAddress" to device.address,
          "connected" to false
        ))
      }
    }

    override fun onCharacteristicReadRequest(
      device: BluetoothDevice,
      requestId: Int,
      offset: Int,
      characteristic: BluetoothGattCharacteristic
    ) {
      requestDevices[requestId] = device
      sendEvent("onCharacteristicReadRequest", mapOf(
        "requestId" to requestId,
        "deviceAddress" to device.address,
        "serviceUuid" to characteristic.service.uuid.toString(),
        "characteristicUuid" to characteristic.uuid.toString(),
        "offset" to offset
      ))
    }

    override fun onCharacteristicWriteRequest(
      device: BluetoothDevice,
      requestId: Int,
      characteristic: BluetoothGattCharacteristic,
      preparedWrite: Boolean,
      responseNeeded: Boolean,
      offset: Int,
      value: ByteArray?
    ) {
      requestDevices[requestId] = device
      sendEvent("onCharacteristicWriteRequest", mapOf(
        "requestId" to requestId,
        "deviceAddress" to device.address,
        "serviceUuid" to characteristic.service.uuid.toString(),
        "characteristicUuid" to characteristic.uuid.toString(),
        "offset" to offset,
        "value" to (value?.map { it.toInt() and 0xFF } ?: emptyList<Int>()),
        "responseNeeded" to responseNeeded
      ))
    }

    override fun onDescriptorReadRequest(
      device: BluetoothDevice,
      requestId: Int,
      offset: Int,
      descriptor: BluetoothGattDescriptor
    ) {
      if (descriptor.uuid == CCCD_UUID) {
        val key = "${descriptor.characteristic.service.uuid}:${descriptor.characteristic.uuid}"
        val subscribed = subscribedDevices[key]?.contains(device.address) == true
        val value = if (subscribed) {
          BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
          BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
      } else {
        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
      }
    }

    override fun onDescriptorWriteRequest(
      device: BluetoothDevice,
      requestId: Int,
      descriptor: BluetoothGattDescriptor,
      preparedWrite: Boolean,
      responseNeeded: Boolean,
      offset: Int,
      value: ByteArray?
    ) {
      if (descriptor.uuid == CCCD_UUID) {
        val key = "${descriptor.characteristic.service.uuid}:${descriptor.characteristic.uuid}"
        if (value != null && value.size >= 2) {
          if (value[0].toInt() != 0 || value[1].toInt() != 0) {
            subscribedDevices.getOrPut(key) { ConcurrentHashMap.newKeySet() }.add(device.address)
          } else {
            subscribedDevices[key]?.remove(device.address)
          }
        }
        if (responseNeeded) {
          gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }
      } else {
        if (responseNeeded) {
          gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
        }
      }
    }

    override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
      gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
    }

    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
      sendEvent("onNotificationSent", mapOf(
        "deviceAddress" to device.address,
        "status" to status
      ))
    }

    override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
      // MTU changes are informational; not emitted as events for now.
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService) {
      val promise = addServicePromise
      addServicePromise = null
      if (status == BluetoothGatt.GATT_SUCCESS) {
        promise?.resolve(null)
      } else {
        promise?.reject(CodedException("ERR_GATT_ADD_SERVICE_FAILED", "Failed to add service (status: $status)", null))
      }
    }
  }

  override fun definition() = ModuleDefinition {
    Name("ReactNativeBleAdvertiser")

    Events(
      "onConnectionStateChange",
      "onCharacteristicReadRequest",
      "onCharacteristicWriteRequest",
      "onNotificationSent"
    )

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

    // --- GATT Server Functions ---

    @SuppressLint("MissingPermission")
    AsyncFunction("startGattServer") { promise: Promise ->
      if (gattServer != null) {
        promise.reject(CodedException("ERR_GATT_SERVER_ALREADY_RUNNING", "GATT server is already running. Call stopGattServer() first.", null))
        return@AsyncFunction
      }

      val manager = bluetoothManager
      if (manager == null) {
        promise.reject(CodedException("ERR_BLE_NOT_AVAILABLE", "Bluetooth is not available on this device", null))
        return@AsyncFunction
      }

      val context = appContext.reactContext
      if (context == null) {
        promise.reject(CodedException("ERR_BLE_NOT_AVAILABLE", "Application context is not available", null))
        return@AsyncFunction
      }

      val server = manager.openGattServer(context, gattServerCallback)
      if (server == null) {
        promise.reject(CodedException("ERR_GATT_SERVER_FAILED", "Failed to open GATT server", null))
        return@AsyncFunction
      }

      gattServer = server
      promise.resolve(null)
    }

    @SuppressLint("MissingPermission")
    Function("stopGattServer") {
      closeGattServer()
    }

    @SuppressLint("MissingPermission")
    AsyncFunction("addService") { service: GattServiceRecord, promise: Promise ->
      val server = gattServer
      if (server == null) {
        promise.reject(CodedException("ERR_GATT_SERVER_NOT_RUNNING", "GATT server is not running. Call startGattServer() first.", null))
        return@AsyncFunction
      }

      if (addServicePromise != null) {
        promise.reject(CodedException("ERR_GATT_ADD_SERVICE_BUSY", "Another addService call is in progress. Wait for it to complete.", null))
        return@AsyncFunction
      }

      val gattService = buildGattService(service)
      addServicePromise = promise

      if (!server.addService(gattService)) {
        addServicePromise = null
        promise.reject(CodedException("ERR_GATT_ADD_SERVICE_FAILED", "Failed to initiate adding service", null))
      }
      // Promise will be resolved/rejected in onServiceAdded callback
    }

    @SuppressLint("MissingPermission")
    Function("removeService") { serviceUuid: String ->
      val server = gattServer ?: return@Function
      val uuid = UUID.fromString(serviceUuid)
      val service = server.getService(uuid)
      if (service != null) {
        server.removeService(service)
        // Clean up subscriptions for this service
        val keysToRemove = subscribedDevices.keys.filter { it.startsWith("$uuid:") }
        keysToRemove.forEach { subscribedDevices.remove(it) }
      }
    }

    @SuppressLint("MissingPermission")
    Function("sendResponse") { requestId: Int, status: Int, offset: Int, value: List<Int>? ->
      val server = gattServer ?: return@Function
      val device = requestDevices.remove(requestId) ?: return@Function
      val bytes = value?.let { ByteArray(it.size) { i -> it[i].toByte() } }
      server.sendResponse(device, requestId, status, offset, bytes)
    }

    @SuppressLint("MissingPermission")
    Function("notifyCharacteristicChanged") { serviceUuid: String, characteristicUuid: String, value: List<Int>, confirm: Boolean ->
      val server = gattServer ?: return@Function
      val service = server.getService(UUID.fromString(serviceUuid)) ?: return@Function
      val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid)) ?: return@Function
      val bytes = ByteArray(value.size) { value[it].toByte() }

      val key = "$serviceUuid:$characteristicUuid"
      val subscribers = subscribedDevices[key] ?: return@Function

      for (address in subscribers) {
        val device = connectedDevices[address] ?: continue
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          server.notifyCharacteristicChanged(device, characteristic, confirm, bytes)
        } else {
          @Suppress("DEPRECATION")
          characteristic.value = bytes
          @Suppress("DEPRECATION")
          server.notifyCharacteristicChanged(device, characteristic, confirm)
        }
      }
    }

    OnDestroy {
      val cb = advertiseCallback
      if (cb != null) {
        advertiser?.stopAdvertising(cb)
        advertiseCallback = null
      }
      closeGattServer()
    }
  }

  private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
      arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }
  }

  @SuppressLint("MissingPermission")
  private fun closeGattServer() {
    gattServer?.close()
    gattServer = null
    connectedDevices.clear()
    requestDevices.clear()
    subscribedDevices.clear()
    addServicePromise = null
  }

  private fun buildGattService(record: GattServiceRecord): BluetoothGattService {
    val service = BluetoothGattService(
      UUID.fromString(record.uuid),
      BluetoothGattService.SERVICE_TYPE_PRIMARY
    )

    for (charRecord in record.characteristics) {
      var properties = 0
      var permissions = 0

      for (prop in charRecord.properties) {
        properties = properties or when (prop) {
          "read" -> BluetoothGattCharacteristic.PROPERTY_READ
          "write" -> BluetoothGattCharacteristic.PROPERTY_WRITE
          "writeWithoutResponse" -> BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
          "notify" -> BluetoothGattCharacteristic.PROPERTY_NOTIFY
          "indicate" -> BluetoothGattCharacteristic.PROPERTY_INDICATE
          else -> 0
        }
      }

      for (perm in charRecord.permissions) {
        permissions = permissions or when (perm) {
          "read" -> BluetoothGattCharacteristic.PERMISSION_READ
          "write" -> BluetoothGattCharacteristic.PERMISSION_WRITE
          else -> 0
        }
      }

      val characteristic = BluetoothGattCharacteristic(
        UUID.fromString(charRecord.uuid),
        properties,
        permissions
      )

      charRecord.value?.let { value ->
        characteristic.value = ByteArray(value.size) { value[it].toByte() }
      }

      // Add CCCD for characteristics that support notifications or indications
      if (properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
        val cccd = BluetoothGattDescriptor(
          CCCD_UUID,
          BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic.addDescriptor(cccd)
      }

      service.addCharacteristic(characteristic)
    }

    return service
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
