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
