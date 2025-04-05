package com.midouz.change_phone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.helper.FileHelper;

import java.util.Properties;

public class SpoofedGeoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Properties props = new Properties();

            String longitude = intent.getStringExtra("longitude");
            if (longitude != null) props.setProperty("longitude", longitude);
            String latitude = intent.getStringExtra("latitude");
            if (latitude != null) props.setProperty("latitude", latitude);
            String timeZone = intent.getStringExtra("time_zone");
            if (timeZone != null) props.setProperty("time_zone", timeZone);

            if (!props.isEmpty()) {
                FileHelper.saveGeoInfoToFile(context, props);
                Log.i("GeoReceiver", "Successfully updated geo values file");
            } else {
                Log.w("GeoReceiver", "No values provided in intent, file not modified");
            }
        } catch (Exception e) {
            Log.e("GeoReceiver", "Failed to save geo values: " + e.getMessage());
        }
    }
}
