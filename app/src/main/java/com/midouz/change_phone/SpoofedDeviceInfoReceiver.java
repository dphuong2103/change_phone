package com.midouz.change_phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.helper.FileHelper;

import java.util.Properties;
import java.util.Random;

public class SpoofedDeviceInfoReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SpoofController", "Received intent: " + intent.toString());
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

            String adId = intent.getStringExtra("ad_id");
            if (adId != null) props.setProperty("ad_id", adId);

            String width = intent.getStringExtra("width");
            if (width != null) props.setProperty("width", width);

            String height = intent.getStringExtra("height");
            if (height != null) props.setProperty("height", height);

            if (!props.isEmpty()) {
                Log.d("SpoofController", "Received spoofed values: " + props);
                FileHelper.saveDeviceInfoToFile(context, props);
                Log.i("SpoofController", "Successfully updated spoofed values file");
            } else {
                Log.w("SpoofController", "No values provided in intent, file not modified");
            }
        } catch (Exception e) {
            Log.e("SpoofController", "Failed to save spoofed values: " + e.getMessage());
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
