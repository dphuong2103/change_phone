package com.midouz.change_phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class SpoofReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceSpoofer";
    private SharedPreferences shareRef = SharePref.getPrefs();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=== Broadcast received: " + intent.toString());
        Log.d(TAG, "=== Extras: " + (intent.getExtras() != null ? intent.getExtras().toString() : "null"));
        Log.d(TAG, "=== Model: " + intent.getStringExtra("MODEL"));
        SpoofController spoofController = new SpoofController();

        String model = intent.getStringExtra("MODEL");
        String brand = intent.getStringExtra("BRAND");
        String androidId = intent.getStringExtra("ANDROID_ID");
        SharedPreferences.Editor editor = shareRef.edit();
        editor.putString("model", model);
        editor.putString("brand", brand);
        editor.apply();
//        if (model != null) {
//            spoofController.setSpoofedModel(model);
//            Log.d(TAG, "Set MODEL to: " + model);
//        }
//        if (brand != null) {
//            spoofController.setSpoofedBrand(brand);
//            Log.d(TAG, "Set BRAND to: " + brand);
//        }
//        if (androidId != null) {
//            spoofController.setSpoofedAndroidId(androidId);
//            Log.d(TAG, "Set ANDROID_ID to: " + androidId);
//        }
        Log.d(TAG, "Broadcast processed successfully");
    }
}