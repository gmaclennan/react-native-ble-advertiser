// Reexport the native module. On web, it will be resolved to ReactNativeBleAdvertiserModule.web.ts
// and on native platforms to ReactNativeBleAdvertiserModule.ts
export { default } from './ReactNativeBleAdvertiserModule';
export { default as ReactNativeBleAdvertiserView } from './ReactNativeBleAdvertiserView';
export * from  './ReactNativeBleAdvertiser.types';
