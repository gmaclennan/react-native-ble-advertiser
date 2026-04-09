import * as React from 'react';

import { ReactNativeBleAdvertiserViewProps } from './ReactNativeBleAdvertiser.types';

export default function ReactNativeBleAdvertiserView(props: ReactNativeBleAdvertiserViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
