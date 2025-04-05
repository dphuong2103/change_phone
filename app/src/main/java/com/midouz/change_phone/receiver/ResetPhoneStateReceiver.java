package com.midouz.change_phone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.helper.ResetPhoneStateHelper;

public class ResetPhoneStateReceiver extends BroadcastReceiver {
    final String TAG = "ResetPhoneStateReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ResetPhoneStateHelper.performDeviceReset(context);
            Log.i(TAG, "Successfully reset phone state");

        } catch (Exception e) {
            Log.e(TAG, "Failed to reset phone state: " + e.getMessage());
        }

    }
}
