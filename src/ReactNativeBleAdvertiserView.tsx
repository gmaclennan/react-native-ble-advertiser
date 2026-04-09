import { requireNativeView } from 'expo';
import * as React from 'react';

import { ReactNativeBleAdvertiserViewProps } from './ReactNativeBleAdvertiser.types';

const NativeView: React.ComponentType<ReactNativeBleAdvertiserViewProps> =
  requireNativeView('ReactNativeBleAdvertiser');

export default function ReactNativeBleAdvertiserView(props: ReactNativeBleAdvertiserViewProps) {
  return <NativeView {...props} />;
}
