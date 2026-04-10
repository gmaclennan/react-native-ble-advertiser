import { createPermissionHook } from 'expo-modules-core';

import ReactNativeBleAdvertiserModule from './ReactNativeBleAdvertiserModule';
import type { AdvertiseData, AdvertiseSettings } from './ReactNativeBleAdvertiser.types';

export type { AdvertiseData, AdvertiseSettings } from './ReactNativeBleAdvertiser.types';

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
