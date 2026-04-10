import { NativeModule, requireNativeModule } from 'expo';

import type {
  AdvertiseData,
  AdvertiseSettings,
  GattService,
  GattServerEvents,
} from './ReactNativeBleAdvertiser.types';

declare class ReactNativeBleAdvertiserModule extends NativeModule<GattServerEvents> {
  isSupported(): boolean;
  getPermissionsAsync(): Promise<import('expo-modules-core').PermissionResponse>;
  requestPermissionsAsync(): Promise<import('expo-modules-core').PermissionResponse>;
  startAdvertising(
    advertiseData: AdvertiseData,
    scanResponseData?: AdvertiseData,
    settings?: AdvertiseSettings
  ): Promise<void>;
  stopAdvertising(): void;

  // GATT Server
  startGattServer(): Promise<void>;
  stopGattServer(): void;
  addService(service: GattService): Promise<void>;
  removeService(serviceUuid: string): void;
  setCharacteristicValue(
    serviceUuid: string,
    characteristicUuid: string,
    value: number[]
  ): void;
  notifyCharacteristicChanged(
    serviceUuid: string,
    characteristicUuid: string,
    value: number[],
    confirm: boolean
  ): void;
}

export default requireNativeModule<ReactNativeBleAdvertiserModule>('ReactNativeBleAdvertiser');
