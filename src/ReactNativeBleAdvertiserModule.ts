import { NativeModule, requireNativeModule } from 'expo';

import type { AdvertiseData, AdvertiseSettings } from './ReactNativeBleAdvertiser.types';

declare class ReactNativeBleAdvertiserModule extends NativeModule {
  isSupported(): boolean;
  getPermissionsAsync(): Promise<import('expo-modules-core').PermissionResponse>;
  requestPermissionsAsync(): Promise<import('expo-modules-core').PermissionResponse>;
  startAdvertising(
    advertiseData: AdvertiseData,
    scanResponseData?: AdvertiseData,
    settings?: AdvertiseSettings
  ): Promise<void>;
  stopAdvertising(): void;
}

export default requireNativeModule<ReactNativeBleAdvertiserModule>('ReactNativeBleAdvertiser');
