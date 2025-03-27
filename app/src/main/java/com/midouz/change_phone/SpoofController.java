package com.midouz.change_phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.model.SpoofingInfo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.Random;

public class SpoofController extends BroadcastReceiver {
    public static final String SPOOF_FILE_PATH = "/data/local/tmp/spoofed_values.properties";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Properties props = new Properties();

            String model = intent.getStringExtra("model");
            if (model != null) props.setProperty("model", model);
            String brand = intent.getStringExtra("brand");
            if (brand != null) props.setProperty("brand", brand);
            String manufacturer = intent.getStringExtra("manufacturer");
            if (manufacturer != null) props.setProperty("manufacturer", manufacturer);
            String serial = intent.getStringExtra("serial");
            if (serial != null) props.setProperty("serial", serial);
            String device = intent.getStringExtra("device");
            if (device != null) props.setProperty("device", device);
            String product = intent.getStringExtra("product");
            if (product != null) props.setProperty("product", product);
            String release = intent.getStringExtra("release");
            if (release != null) props.setProperty("release", release);
            String sdk = intent.getStringExtra("sdk");
            if (sdk != null) props.setProperty("sdk", sdk);
            String fingerprint = intent.getStringExtra("fingerprint");
            if (fingerprint != null) props.setProperty("fingerprint", fingerprint);
            String androidId = intent.getStringExtra("android_id");
            if (androidId != null) props.setProperty("android_id", androidId);
            String macAddress = intent.getStringExtra("mac_address");
            if (macAddress != null) props.setProperty("mac_address", macAddress);
            else if (!props.containsKey("mac_address"))
                props.setProperty("mac_address", generateRandomMacAddress());
            String ssid = intent.getStringExtra("ssid"); // Add SSID extra
            if (ssid != null) props.setProperty("ssid", ssid);

            if (!props.isEmpty()) {
                saveToFile(context, props);
                Log.i("SpoofController", "Successfully updated spoofed values file");
            } else {
                Log.w("SpoofController", "No values provided in intent, file not modified");
            }
        } catch (Exception e) {
            Log.e("SpoofController", "Failed to save spoofed values: " + e.getMessage());
        }
    }

    public static void changeSpoofedValues(Context context, SpoofingInfo device) {
        Properties props = new Properties();

        props.setProperty("model", device.getModel());
        props.setProperty("brand", device.getBrand());
        props.setProperty("manufacturer", device.getManufacturer());
        props.setProperty("serial", device.getSerialNo());
        props.setProperty("device", device.getDevice());
        props.setProperty("product", device.getProductName());
        props.setProperty("release", device.getReleaseVersion());
        props.setProperty("sdk", String.valueOf(Integer.parseInt(device.getSdkVersion())));
        props.setProperty("fingerprint", device.getFingerprint());
        props.setProperty("android_id", device.getAndroidId());
        props.setProperty("mac_address", device.getMacAddress() != null ? device.getMacAddress() : generateRandomMacAddress());
        props.setProperty("ssid", device.getSsid() != null ? device.getSsid() : "SpoofedWiFi");

        try {
            saveToFile(context, props);
            Log.i("SpoofController", "Successfully updated spoofed values file with device info");
        } catch (Exception e) {
            Log.e("SpoofController", "Failed to save spoofed values: " + e.getMessage());
        }
    }

    public static SpoofingInfo loadSpoofedValues() {
        try {
            File file = new File(SPOOF_FILE_PATH);
            if (file.exists()) {
                Properties props = new Properties();
                props.load(new FileReader(file));
                Log.i("SpoofController", "Loaded spoofed values from file");

                return SpoofingInfo.builder()
                        .model(props.getProperty("model", "Unknown"))
                        .brand(props.getProperty("brand", "Unknown"))
                        .manufacturer(props.getProperty("manufacturer", "Unknown"))
                        .serialNo(props.getProperty("serial", "Unknown"))
                        .device(props.getProperty("device", "Unknown"))
                        .productName(props.getProperty("product", "Unknown"))
                        .releaseVersion(props.getProperty("release", "Unknown"))
                        .sdkVersion(props.getProperty("sdk", "Unknown"))
                        .fingerprint(props.getProperty("fingerprint", "Unknown"))
                        .androidId(props.getProperty("android_id", "Unknown"))
                        .macAddress(props.getProperty("mac_address", generateRandomMacAddress()))
                        .ssid(props.getProperty("ssid", "SpoofedWiFi"))
                        .build();
            } else {
                Log.w("SpoofController", "Spoofed values file does not exist");
            }
        } catch (Exception e) {
            Log.e("SpoofController", "Failed to load spoofed values: " + e.getMessage());
        }

        return SpoofingInfo.builder()
                .model("Unknown")
                .brand("Unknown")
                .manufacturer("Unknown")
                .serialNo("Unknown")
                .device("Unknown")
                .productName("Unknown")
                .releaseVersion("Unknown")
                .sdkVersion("Unknown")
                .fingerprint("Unknown")
                .androidId("Unknown")
                .macAddress(generateRandomMacAddress())
                .ssid("SpoofedWiFi")
                .build();
    }

    private static void saveToFile(Context context, Properties props) throws Exception {
        File file = new File(SPOOF_FILE_PATH);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mkdir -p " + parentDir.getAbsolutePath() + "\n");
            os.writeBytes("chmod 777 " + parentDir.getAbsolutePath() + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        }

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String tempFile = context.getFilesDir() + "/temp_spoofed_values.properties";

            // Write to a temp file in app directory first
            try (FileWriter writer = new FileWriter(tempFile)) {
                props.store(writer, "Spoofed Device Info");
            }

            // Copy to /data/local/tmp and set permissions
            os.writeBytes("cat " + tempFile + " > " + SPOOF_FILE_PATH + "\n");
            os.writeBytes("chmod 644 " + SPOOF_FILE_PATH + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            new File(tempFile).delete(); // Clean up temp file
            Log.i("SpoofController", "Successfully saved spoofed values to: " + SPOOF_FILE_PATH);
        } catch (Exception e) {
            Log.e("SpoofController", "Error writing to file: " + e.getMessage());
            throw e;
        }
    }

    private static String generateRandomMacAddress() {
        Random rand = new Random();
        byte[] mac = new byte[6];
        rand.nextBytes(mac);
        mac[0] &= (byte) 0xFE; // Ensure unicast
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    }
}
