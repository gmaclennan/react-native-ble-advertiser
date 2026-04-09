import { registerWebModule, NativeModule } from 'expo';

import { ReactNativeBleAdvertiserModuleEvents } from './ReactNativeBleAdvertiser.types';

class ReactNativeBleAdvertiserModule extends NativeModule<ReactNativeBleAdvertiserModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ReactNativeBleAdvertiserModule, 'ReactNativeBleAdvertiserModule');
