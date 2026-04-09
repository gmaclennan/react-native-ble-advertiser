import { NativeModule, requireNativeModule } from 'expo';

import { ReactNativeBleAdvertiserModuleEvents } from './ReactNativeBleAdvertiser.types';

declare class ReactNativeBleAdvertiserModule extends NativeModule<ReactNativeBleAdvertiserModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ReactNativeBleAdvertiserModule>('ReactNativeBleAdvertiser');
