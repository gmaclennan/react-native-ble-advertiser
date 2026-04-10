import { ConfigPlugin, withInfoPlist } from 'expo/config-plugins';

type PluginProps = {
  bluetoothAlwaysDescription?: string;
} | void;

const withBleAdvertiser: ConfigPlugin<PluginProps> = (config, props) => {
  const description =
    (props && props.bluetoothAlwaysDescription) ||
    'Allow $(PRODUCT_NAME) to use Bluetooth';

  config = withInfoPlist(config, (config) => {
    config.modResults['NSBluetoothAlwaysUsageDescription'] =
      config.modResults['NSBluetoothAlwaysUsageDescription'] || description;
    return config;
  });

  return config;
};

export default withBleAdvertiser;
