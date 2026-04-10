import { useEffect, useRef, useState } from "react";
import {
  isSupported,
  startAdvertising,
  stopAdvertising,
  startGattServer,
  stopGattServer,
  addService,
  addGattServerListener,
} from "react-native-ble-advertiser";
import BleManager from "react-native-ble-manager";
import {
  FlatList,
  KeyboardAvoidingView,
  PermissionsAndroid,
  Platform,
  Pressable,
  StatusBar,
  Text,
  TextInput,
  View,
} from "react-native";

const SERVICE_UUID = "ed91f27c-4a1c-2e9d-4add-139c1075b442";
const CHAR_UUID = "ed91f27c-4a1c-2e9d-4add-139c1075b443";

type DiscoveredDevice = {
  id: string;
  name: string;
};

// --- Header ---

function Header({ title, onBack }: { title: string; onBack?: () => void }) {
  return (
    <View
      style={{
        backgroundColor: "#fff",
        paddingTop: StatusBar.currentHeight ?? 48,
        paddingHorizontal: 16,
        paddingBottom: 12,
        flexDirection: "row",
        alignItems: "center",
        borderBottomWidth: 1,
        borderBottomColor: "#ddd",
      }}
    >
      {onBack ? (
        <Pressable
          onPress={onBack}
          hitSlop={{ top: 16, bottom: 16, left: 16, right: 16 }}
          style={{ marginRight: 12, padding: 4 }}
        >
          <Text style={{ fontSize: 28, color: "#007AFF" }}>&#x2039;</Text>
        </Pressable>
      ) : null}
      <Text style={{ fontSize: 18, fontWeight: "600" }}>{title}</Text>
    </View>
  );
}

// --- Start Screen ---

function StartScreen({ onStart }: { onStart: (name: string) => void }) {
  const [name, setName] = useState("");
  const inputRef = useRef<TextInput>(null);
  const byteLength = new TextEncoder().encode(name).length;
  const valid = byteLength > 0 && byteLength <= 10;

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  return (
    <View style={{ flex: 1, backgroundColor: "#eee" }}>
      <Header title="BLE Advertiser" />
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : "height"}
      >
        <View
          style={{
            flex: 1,
            justifyContent: "flex-start",
            padding: 40,
            paddingTop: 80,
            gap: 8,
          }}
        >
          <Text style={{ fontSize: 16 }}>Enter your name (max 10 bytes):</Text>
          <TextInput
            ref={inputRef}
            style={{
              backgroundColor: "#fff",
              borderRadius: 8,
              padding: 12,
              fontSize: 18,
              borderWidth: 1,
              borderColor: "#ccc",
            }}
            value={name}
            onChangeText={setName}
            placeholder="Your name"
            maxLength={10}
          />
          <Text style={{ textAlign: "right", color: "#666" }}>
            {byteLength}/10 bytes
          </Text>
          <Pressable
            onPress={() => onStart(name)}
            disabled={!valid}
            style={{
              marginTop: 8,
              backgroundColor: "#007AFF",
              borderRadius: 8,
              paddingVertical: 12,
              alignItems: "center",
              opacity: valid ? 1 : 0.4,
            }}
          >
            <Text style={{ color: "#fff", fontSize: 16, fontWeight: "600" }}>
              Start
            </Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

// --- Discovery Screen ---

function DiscoveryScreen({
  name,
  onBack,
}: {
  name: string;
  onBack: () => void;
}) {
  const [devices, setDevices] = useState<DiscoveredDevice[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [started, setStarted] = useState(false);
  const [gattStatus, setGattStatus] = useState("starting");
  const seenIds = useRef(new Set<string>());

  useEffect(() => {
    let subscription: { remove: () => void };
    let stopped = false;

    async function init() {
      try {
        const results = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        ]);

        const denied = Object.entries(results).filter(
          ([_, v]) => v !== PermissionsAndroid.RESULTS.GRANTED,
        );
        if (denied.length > 0) {
          setError(
            "Bluetooth permissions denied: " +
              denied.map(([k]) => k).join(", "),
          );
          return;
        }

        if (stopped) return;

        const nameBytes = new TextEncoder().encode(name);

        // Each BLE operation is independent — one failing shouldn't
        // prevent the others from being attempted.
        try {
          await startAdvertising(
            { serviceUuids: [SERVICE_UUID] },
            { manufacturerData: [{ id: 0xffff, data: nameBytes }] },
            { mode: "lowPower", connectable: true },
          );
        } catch (e: any) {
          if (!stopped) setError(e.message);
        }

        try {
          await startGattServer();
          await addService({
            uuid: SERVICE_UUID,
            characteristics: [
              {
                uuid: CHAR_UUID,
                properties: ["read", "write", "notify"],
                permissions: ["read", "write"],
                value: Array.from(nameBytes),
              },
            ],
          });
          if (!stopped) setGattStatus("running");
        } catch (e: any) {
          if (!stopped) setGattStatus("error");
        }

        try {
          await BleManager.start();
          subscription = BleManager.onDiscoverPeripheral((peripheral: any) => {
            console.log("Discovered peripheral:", peripheral);
            if (seenIds.current.has(peripheral.id)) return;

            const mfgData = peripheral.advertising?.manufacturerData;
            let deviceName: string;
            if (mfgData?.bytes) {
              const bytes = new Uint8Array(mfgData.bytes);
              deviceName = new TextDecoder().decode(bytes.slice(2));
            }
            seenIds.current.add(peripheral.id);
            setDevices((prev) => [
              ...prev,
              {
                id: peripheral.id,
                name: deviceName || peripheral.name || "Unknown",
              },
            ]);
          });
          await BleManager.scan({ serviceUUIDs: [SERVICE_UUID], seconds: 0 });
        } catch {
          // Scanning unavailable (e.g. on emulator)
        }

        if (!stopped) setStarted(true);
      } catch (e: any) {
        setError(e.message);
      }
    }

    const writeSub = addGattServerListener(
      "onCharacteristicWriteRequest",
      (event) => {
        console.log("GATT write:", event.characteristicUuid, event.value);
      },
    );

    init();

    return () => {
      stopped = true;
      subscription?.remove();
      writeSub.remove();
      stopAdvertising();
      stopGattServer();
      BleManager.stopScan();
    };
  }, []);

  const handleBack = () => {
    stopAdvertising();
    stopGattServer();
    BleManager.stopScan();
    onBack();
  };

  return (
    <View style={{ flex: 1, backgroundColor: "#eee" }}>
      <Header title="Discovery" onBack={handleBack} />
      <FlatList
        data={devices}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 20, gap: 8 }}
        ListHeaderComponent={
          <View style={{ paddingBottom: 8, gap: 4 }}>
            <Text style={{ fontSize: 16 }}>
              {started ? `Broadcasting as "${name}"` : "Starting..."}
            </Text>
            <Text style={{ color: "#666" }}>
              GATT Server: {gattStatus}
            </Text>
            {!isSupported() && (
              <Text style={{ color: "red" }}>
                BLE advertising not supported on this device
              </Text>
            )}
            {error && <Text style={{ color: "red" }}>{error}</Text>}
            {started && devices.length === 0 && (
              <Text style={{ color: "#999", fontStyle: "italic" }}>
                Scanning for nearby devices...
              </Text>
            )}
          </View>
        }
        renderItem={({ item }) => (
          <View
            style={{
              backgroundColor: "#fff",
              padding: 16,
              borderRadius: 10,
            }}
          >
            <Text style={{ fontSize: 16 }}>{item.name}</Text>
          </View>
        )}
      />
    </View>
  );
}

// --- App ---

export default function App() {
  const [name, setName] = useState<string | null>(null);

  if (!name) {
    return <StartScreen onStart={setName} />;
  }

  return <DiscoveryScreen name={name} onBack={() => setName(null)} />;
}
