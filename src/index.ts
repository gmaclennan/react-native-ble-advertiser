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

export function setCharacteristicValue(
  serviceUuid: string,
  characteristicUuid: string,
  value: number[]
): void {
  ReactNativeBleAdvertiserModule.setCharacteristicValue(
    serviceUuid,
    characteristicUuid,
    value
  );
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
