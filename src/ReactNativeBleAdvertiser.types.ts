type RequireAtLeastOne<T> = {
  [K in keyof T]-?: Required<Pick<T, K>> & Partial<Pick<T, Exclude<keyof T, K>>>;
}[keyof T];

type AdvertiseDataFields = {
  serviceUuids?: string[];
  includeDeviceName?: boolean;
  /** Android-only. Ignored on iOS. */
  manufacturerData?: Array<{ id: number; data: Uint8Array }>;
  /** Android-only. Ignored on iOS. */
  serviceData?: Array<{ uuid: string; data: Uint8Array }>;
  /** Android-only. Ignored on iOS. */
  includeTxPowerLevel?: boolean;
};

export type AdvertiseData = RequireAtLeastOne<AdvertiseDataFields>;

/** Android-only. Ignored on iOS. */
export type AdvertiseSettings = {
  mode?: 'lowPower' | 'balanced' | 'lowLatency';
  txPowerLevel?: 'ultraLow' | 'low' | 'medium' | 'high';
  connectable?: boolean;
  timeout?: number;
};

// --- GATT Server Types ---

export type CharacteristicProperty =
  | 'read'
  | 'write'
  | 'writeWithoutResponse'
  | 'notify'
  | 'indicate';

export type CharacteristicPermission = 'read' | 'write';

export type GattCharacteristic = {
  uuid: string;
  properties: CharacteristicProperty[];
  permissions: CharacteristicPermission[];
  /** Initial value as byte array. */
  value?: number[];
};

export type GattService = {
  uuid: string;
  characteristics: GattCharacteristic[];
};

export type ConnectionStateChangeEvent = {
  deviceAddress: string;
  connected: boolean;
};

export type CharacteristicReadRequestEvent = {
  requestId: number;
  deviceAddress: string;
  serviceUuid: string;
  characteristicUuid: string;
  offset: number;
};

export type CharacteristicWriteRequestEvent = {
  requestId: number;
  deviceAddress: string;
  serviceUuid: string;
  characteristicUuid: string;
  offset: number;
  value: number[];
  responseNeeded: boolean;
};

export type NotificationSentEvent = {
  deviceAddress: string;
  status: number;
};

export type GattServerEvents = {
  onConnectionStateChange: ConnectionStateChangeEvent;
  onCharacteristicReadRequest: CharacteristicReadRequestEvent;
  onCharacteristicWriteRequest: CharacteristicWriteRequestEvent;
  onNotificationSent: NotificationSentEvent;
};
