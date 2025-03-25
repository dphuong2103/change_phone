package com.midouz.change_phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.model.DeviceInfo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

public class SpoofController extends BroadcastReceiver {
    // Static fields for spoofed values
    public static String spoofedModel;
    public static String spoofedBrand;
    public static String spoofedManufacturer;
    public static String spoofedSerial;
    public static String spoofedDevice;
    public static String spoofedProduct;
    public static String spoofedReleaseVersion;
    public static int spoofedSdkVersion;
    public static String spoofedFingerprint;
    public static String spoofedAndroidId;
    public static String spoofedMacSuffix;

    public static String getSpoofFilePath() {
        return "data/local/tmp/spoofed_values.properties";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Properties props = new Properties();

            // Get values from intent extras
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

            String macSuffix = intent.getStringExtra("mac_suffix");
            if (macSuffix != null) props.setProperty("mac_suffix", macSuffix);

            // Update static fields
            updateStaticFields(props);

            // Write to file if there are changes
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

    public static void changeDeviceInfo(Context context, DeviceInfo device) {
        Properties props = new Properties();

        // Update static fields
        spoofedModel = device.getModel();
        spoofedBrand = device.getBrand();
        spoofedManufacturer = device.getManufacturer();
        spoofedSerial = device.getSerialNo();
        spoofedDevice = device.getDevice();
        spoofedProduct = device.getProductName();
        spoofedReleaseVersion = device.getReleaseVersion();
        spoofedSdkVersion = Integer.parseInt(device.getSdkVersion());
        spoofedFingerprint = device.getFingerprint();
        spoofedAndroidId = device.getAndroidId();
        spoofedMacSuffix = device.getMacSuffix();

        // Update properties for file
        props.setProperty("model", spoofedModel);
        props.setProperty("brand", spoofedBrand);
        props.setProperty("manufacturer", spoofedManufacturer);
        props.setProperty("serial", spoofedSerial);
        props.setProperty("device", spoofedDevice);
        props.setProperty("product", spoofedProduct);
        props.setProperty("release", spoofedReleaseVersion);
        props.setProperty("sdk", String.valueOf(spoofedSdkVersion));
        props.setProperty("fingerprint", spoofedFingerprint);
        props.setProperty("android_id", spoofedAndroidId);
        props.setProperty("mac_suffix", spoofedMacSuffix);

        try {
            saveToFile(context, props);
            Log.i("SpoofController", "Successfully updated spoofed values file with device info");
        } catch (Exception e) {
            Log.e("SpoofController", "Failed to save spoofed values: " + e.getMessage());
        }
    }

    // Load spoofed values from file
    public static void loadSpoofedValues(Context context) {
        try {
            File file = new File(getSpoofFilePath());
            if (file.exists()) {
                Properties props = new Properties();
                props.load(new FileReader(file));
                updateStaticFields(props);
                Log.i("SpoofController", "Loaded spoofed values from file");
            } else {
                Log.w("SpoofController", "Spoofed values file does not exist");
            }
        } catch (Exception e) {
            Log.e("SpoofController", "Failed to load spoofed values: " + e.getMessage());
        }
    }

    private static void saveToFile(Context context, Properties props) throws Exception {
        File file = new File(getSpoofFilePath());
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
            os.writeBytes("cat " + tempFile + " > " + getSpoofFilePath() + "\n");
            os.writeBytes("chmod 644 " + getSpoofFilePath() + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            new File(tempFile).delete(); // Clean up temp file
            Log.i("SpoofController", "Successfully saved spoofed values to: " + getSpoofFilePath());
        } catch (Exception e) {
            Log.e("SpoofController", "Error writing to file: " + e.getMessage());
            throw e;
        }
    }
    private static void updateStaticFields(Properties props) {
        spoofedModel = props.getProperty("model", spoofedModel);
        spoofedBrand = props.getProperty("brand", spoofedBrand);
        spoofedManufacturer = props.getProperty("manufacturer", spoofedManufacturer);
        spoofedSerial = props.getProperty("serial", spoofedSerial);
        spoofedDevice = props.getProperty("device", spoofedDevice);
        spoofedProduct = props.getProperty("product", spoofedProduct);
        spoofedReleaseVersion = props.getProperty("release", spoofedReleaseVersion);
        spoofedSdkVersion = Integer.parseInt(props.getProperty("sdk", String.valueOf(spoofedSdkVersion)));
        spoofedFingerprint = props.getProperty("fingerprint", spoofedFingerprint);
        spoofedAndroidId = props.getProperty("android_id", spoofedAndroidId);
        spoofedMacSuffix = props.getProperty("mac_suffix", spoofedMacSuffix);

        // Log updated values for debugging
        Log.d("SpoofController", "Updated spoofedModel: " + spoofedModel);
        Log.d("SpoofController", "Updated spoofedBrand: " + spoofedBrand);
        Log.d("SpoofController", "Updated spoofedManufacturer: " + spoofedManufacturer);
    }
}