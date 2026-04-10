import { createPermissionHook, type EventSubscription } from 'expo-modules-core';

import ReactNativeBleAdvertiserModule from './ReactNativeBleAdvertiserModule';
import type {
  AdvertiseData,
  AdvertiseSettings,
  GattService,
  GattServerEvents,
} from './ReactNativeBleAdvertiser.types';

export type {
  AdvertiseData,
  AdvertiseSettings,
  GattService,
  GattServerEvents,
  CharacteristicProperty,
  CharacteristicPermission,
  GattCharacteristic,
  ConnectionStateChangeEvent,
  CharacteristicReadRequestEvent,
  CharacteristicWriteRequestEvent,
  NotificationSentEvent,
} from './ReactNativeBleAdvertiser.types';

// --- Advertising ---

export function isSupported(): boolean {
  return ReactNativeBleAdvertiserModule.isSupported();
}

export async function getPermissionsAsync() {
  return ReactNativeBleAdvertiserModule.getPermissionsAsync();
}

export async function requestPermissionsAsync() {
  return ReactNativeBleAdvertiserModule.requestPermissionsAsync();
}

export const usePermissions = createPermissionHook({
  getMethod: getPermissionsAsync,
  requestMethod: requestPermissionsAsync,
});

export async function startAdvertising(
  advertiseData: AdvertiseData,
  scanResponseData?: AdvertiseData,
  settings?: AdvertiseSettings
): Promise<void> {
  return ReactNativeBleAdvertiserModule.startAdvertising(
    advertiseData,
    scanResponseData,
    settings
  );
}

export function stopAdvertising(): void {
  ReactNativeBleAdvertiserModule.stopAdvertising();
}

// --- GATT Server ---

export async function startGattServer(): Promise<void> {
  return ReactNativeBleAdvertiserModule.startGattServer();
}

export function stopGattServer(): void {
  ReactNativeBleAdvertiserModule.stopGattServer();
}

export async function addService(service: GattService): Promise<void> {
  return ReactNativeBleAdvertiserModule.addService(service);
}

export function removeService(serviceUuid: string): void {
  ReactNativeBleAdvertiserModule.removeService(serviceUuid);
}

export function sendResponse(
  requestId: number,
  status: number,
  offset: number,
  value?: number[]
): void {
  ReactNativeBleAdvertiserModule.sendResponse(requestId, status, offset, value);
}

export function notifyCharacteristicChanged(
  serviceUuid: string,
  characteristicUuid: string,
  value: number[],
  confirm: boolean = false
): void {
  ReactNativeBleAdvertiserModule.notifyCharacteristicChanged(
    serviceUuid,
    characteristicUuid,
    value,
    confirm
  );
}

export function addGattServerListener<T extends keyof GattServerEvents>(
  eventName: T,
  listener: (event: GattServerEvents[T]) => void
): EventSubscription {
  return ReactNativeBleAdvertiserModule.addListener(eventName, listener);
}

export const GATT_SUCCESS = 0x00;
export const GATT_READ_NOT_PERMITTED = 0x02;
export const GATT_WRITE_NOT_PERMITTED = 0x03;
export const GATT_INSUFFICIENT_AUTHENTICATION = 0x05;
export const GATT_REQUEST_NOT_SUPPORTED = 0x06;
export const GATT_INVALID_OFFSET = 0x07;
export const GATT_INVALID_ATTRIBUTE_LENGTH = 0x0d;
export const GATT_INSUFFICIENT_ENCRYPTION = 0x0f;
export const GATT_FAILURE = 0x101;
