package com.midouz.change_phone;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int WIFI_PERMISSION_REQUEST = 101;

    Button button;
    TextView device, model, brand, manufacturer, serial, productName, releaseVersion, sdkVersion, mac, fingerprint, androidId, ssid, latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTextView();
        button = findViewById(R.id.btn_changeDeviceInfo);

        button.setOnClickListener(v -> {
            fetchSpoofedDeviceInfo();
        });

        fetchSpoofedDeviceInfo(); // Fetch spoofed values on start
    }

    private void setTextView() {
        device = findViewById(R.id.txt_device);
        model = findViewById(R.id.txt_model);
        brand = findViewById(R.id.txt_brand);
        manufacturer = findViewById(R.id.txt_manufacturer);
        serial = findViewById(R.id.txt_serialNo);
        productName = findViewById(R.id.txt_productName);
        releaseVersion = findViewById(R.id.txt_releaseVersion);
        sdkVersion = findViewById(R.id.txt_sdkVersion);
        mac = findViewById(R.id.txt_macAddress);
        fingerprint = findViewById(R.id.txt_fingerprint);
        androidId = findViewById(R.id.txt_androidId);
        ssid = findViewById(R.id.txt_ssid);
        latitude = findViewById(R.id.txt_latitude);
        longitude = findViewById(R.id.txt_longitude);
    }

    private void fetchSpoofedDeviceInfo() {
        // Fetch basic device info
        device.setText("Device: " + Build.DEVICE);
        model.setText("Model:" + Build.MODEL);
        brand.setText("Brand: " + Build.BRAND);
        manufacturer.setText("Manufacturer: " + Build.MANUFACTURER);
        serial.setText("Serial: " + Build.SERIAL);
        productName.setText("Product: " + Build.PRODUCT);
        releaseVersion.setText("Release Version: " + Build.VERSION.RELEASE);
        sdkVersion.setText("SDK Version: " + String.valueOf(Build.VERSION.SDK_INT));
        fingerprint.setText("Fingerprint: " + Build.FINGERPRINT);

        // Fetch Android ID
        androidId.setText("Android id: " + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        // Fetch Wi-Fi and Location data
        fetchWiFiInfo();
        fetchLocation();
    }

    private void fetchWiFiInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, WIFI_PERMISSION_REQUEST);
            return;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            ssid.setText("ssid: " + wifiInfo.getSSID());
            mac.setText("mac: " + wifiInfo.getMacAddress());
        } else {
            ssid.setText("ssid: N/A");
            mac.setText("mac: N/A");
        }
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        Location lastKnownLocation = null;
//
//        if (locationManager != null) {
//            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            if (lastKnownLocation == null) {
//                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//            }
//        }
//
//        if (lastKnownLocation != null) {
//            latitude.setText(String.valueOf(lastKnownLocation.getLatitude()));
//            longitude.setText(String.valueOf(lastKnownLocation.getLongitude()));
//        } else {
//            latitude.setText("0.0");
//            longitude.setText("0.0");
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST || requestCode == WIFI_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == LOCATION_PERMISSION_REQUEST) {
                    fetchLocation();
                } else {
                    fetchWiFiInfo();
                }
            } else {
                Log.w("Permissions", "Permission denied by user.");
            }
        }
    }
}
