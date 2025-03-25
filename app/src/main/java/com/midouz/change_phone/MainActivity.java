package com.midouz.change_phone;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.midouz.change_phone.model.DeviceInfo;

public class MainActivity extends Activity {
    Button button;
    DeviceInfo deviceInfo;
    TextView device;
    TextView model;
    TextView brand;
    TextView manufacturer;
    TextView serial;
    TextView productName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Use your layout instead of system layout
        setTextView();
        button = findViewById(R.id.btn_changeDeviceInfo);
        SpoofController.loadSpoofedValues(this);
        setDeviceInfoTextField();
        button.setOnClickListener(v -> {
            deviceInfo = DeviceInfo.getRandomDevice();
            Log.d("Change Device", "Changing device info: " + deviceInfo.toString());
            SpoofController.changeDeviceInfo(this, deviceInfo);
            setDeviceInfoTextField();
        });

    }

    private void setTextView(){
        device = findViewById(R.id.txt_device);
        model = findViewById(R.id.txt_model);
        brand = findViewById(R.id.txt_brand);
        manufacturer = findViewById(R.id.txt_manufacturer);
        serial = findViewById(R.id.txt_serialNo);
    }
    private void setDeviceInfoTextField(){
        device.setText(SpoofController.spoofedDevice != null ? SpoofController.spoofedDevice : "null");
        model.setText(SpoofController.spoofedModel != null ? SpoofController.spoofedModel : "null");
        brand.setText(SpoofController.spoofedBrand != null ? SpoofController.spoofedBrand : "null");
        manufacturer.setText(SpoofController.spoofedManufacturer != null ? SpoofController.spoofedManufacturer : "null");
        serial.setText(SpoofController.spoofedSerial != null ? SpoofController.spoofedSerial : "null");

    }

}