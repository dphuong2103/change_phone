package com.midouz.change_phone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.constant.SpoofPropConstant;
import com.midouz.change_phone.helper.FileHelper;
import com.midouz.change_phone.helper.ResetPhoneStateHelper;

import java.util.Properties;
import java.util.Random;

public class SpoofedDeviceInfoReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SpoofController", "Received intent: " + intent.toString());
        try {
            ResetPhoneStateHelper.performDeviceReset(context);
            Properties props = new Properties();

            // Original fields
            String model = intent.getStringExtra("model");
            if (model != null) props.setProperty(SpoofPropConstant.MODEL, model);
            String brand = intent.getStringExtra("brand");
            if (brand != null) props.setProperty(SpoofPropConstant.BRAND, brand);
            String manufacturer = intent.getStringExtra("manufacturer");
            if (manufacturer != null) props.setProperty(SpoofPropConstant.MANUFACTURER, manufacturer);
            String serial = intent.getStringExtra("serial");
            if (serial != null) props.setProperty(SpoofPropConstant.SERIAL, serial);
            String device = intent.getStringExtra("device");
            if (device != null) props.setProperty(SpoofPropConstant.DEVICE, device);
            String product = intent.getStringExtra("product");
            if (product != null) props.setProperty(SpoofPropConstant.PRODUCT, product);
            String release = intent.getStringExtra("release");
            if (release != null) props.setProperty(SpoofPropConstant.RELEASE, release);
            String sdk = intent.getStringExtra("sdk");
            if (sdk != null) props.setProperty(SpoofPropConstant.SDK, sdk);
            String fingerprint = intent.getStringExtra("fingerprint");
            if (fingerprint != null) props.setProperty(SpoofPropConstant.FINGERPRINT, fingerprint);
            String androidId = intent.getStringExtra("android_id");
            if (androidId != null) props.setProperty(SpoofPropConstant.ANDROID_ID, androidId);
            String macAddress = intent.getStringExtra("mac_address");
            if (macAddress != null) {
                props.setProperty(SpoofPropConstant.MAC_ADDRESS, macAddress);
            } else if (!props.containsKey("MAC_ADDRESS")) {
                props.setProperty(SpoofPropConstant.MAC_ADDRESS, generateRandomMacAddress());
            }
            String ssid = intent.getStringExtra("ssid");
            if (ssid != null) props.setProperty(SpoofPropConstant.WIFI_NAME, ssid);
            String imei = intent.getStringExtra("imei");
            if (imei != null) props.setProperty(SpoofPropConstant.IMEI, imei);
            String subscriberId = intent.getStringExtra("subscriber_id");
            if (subscriberId != null) props.setProperty(SpoofPropConstant.SUBSCRIBER_ID, subscriberId);

            // Legacy field
            String adId = intent.getStringExtra("ad_id");
            if (adId != null) props.setProperty(SpoofPropConstant.AD_ID, adId);

            // Screen size fields (for SpoofScreenSizeHelper)
            String width = intent.getStringExtra("width");
            if (width != null) props.setProperty(SpoofPropConstant.WIDTH, width);
            String height = intent.getStringExtra("height");
            if (height != null) props.setProperty(SpoofPropConstant.HEIGHT, height);

            // New fields
            String androidSerial = intent.getStringExtra("android_serial");
            if (androidSerial != null) props.setProperty(SpoofPropConstant.ANDROID_SERIAL, androidSerial);
            String phoneNumber = intent.getStringExtra("phone_number");
            if (phoneNumber != null) props.setProperty(SpoofPropConstant.PHONE_NUMBER, phoneNumber);
            String glVendor = intent.getStringExtra("gl_vendor");
            if (glVendor != null) props.setProperty(SpoofPropConstant.GL_VENDOR, glVendor);
            String glRender = intent.getStringExtra("gl_render");
            if (glRender != null) props.setProperty(SpoofPropConstant.GL_RENDER, glRender);
            String hardware = intent.getStringExtra("hardware");
            if (hardware != null) props.setProperty(SpoofPropConstant.HARDWARE, hardware);
            String id = intent.getStringExtra("id");
            if (id != null) props.setProperty(SpoofPropConstant.ID, id);
            String host = intent.getStringExtra("host");
            if (host != null) props.setProperty(SpoofPropConstant.HOST, host);
            String radio = intent.getStringExtra("radio");
            if (radio != null) props.setProperty(SpoofPropConstant.RADIO, radio);
            String bootloader = intent.getStringExtra("bootloader");
            if (bootloader != null) props.setProperty(SpoofPropConstant.BOOTLOADER, bootloader);
            String display = intent.getStringExtra("display");
            if (display != null) props.setProperty(SpoofPropConstant.DISPLAY, display);
            String board = intent.getStringExtra("board");
            if (board != null) props.setProperty(SpoofPropConstant.BOARD, board);
            String codename = intent.getStringExtra("codename");
            if (codename != null) props.setProperty(SpoofPropConstant.CODENAME, codename);
            String serialSimNumber = intent.getStringExtra("serial_sim_number");
            if (serialSimNumber != null) props.setProperty(SpoofPropConstant.SERIAL_SIM_NUMBER, serialSimNumber);
            String bssid = intent.getStringExtra("bssid");
            if (bssid != null) props.setProperty(SpoofPropConstant.BSSID, bssid);
            String operator = intent.getStringExtra("operator");
            if (operator != null) props.setProperty(SpoofPropConstant.OPERATOR, operator);
            String operatorName = intent.getStringExtra("operator_name");
            if (operatorName != null) props.setProperty(SpoofPropConstant.OPERATOR_NAME, operatorName);
            String countryIso = intent.getStringExtra("country_iso");
            if (countryIso != null) props.setProperty(SpoofPropConstant.COUNTRY_ISO, countryIso);
            String userAgent = intent.getStringExtra("user_agent");
            if (userAgent != null) props.setProperty(SpoofPropConstant.USER_AGENT, userAgent);
            String osVersion = intent.getStringExtra("os_version");
            if (osVersion != null) props.setProperty(SpoofPropConstant.OS_VERSION, osVersion);
            String macHardware = intent.getStringExtra("mac_hardware");
            if (macHardware != null) props.setProperty(SpoofPropConstant.MAC_HARDWARE, macHardware);
            String wifiIp = intent.getStringExtra("wifi_ip");
            if (wifiIp != null) props.setProperty(SpoofPropConstant.WIFI_IP, wifiIp);
            String versionChrome = intent.getStringExtra("version_chrome");
            if (versionChrome != null) props.setProperty(SpoofPropConstant.VERSION_CHROME, versionChrome);

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