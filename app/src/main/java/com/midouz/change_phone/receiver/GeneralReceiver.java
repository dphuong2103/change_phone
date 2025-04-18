package com.midouz.change_phone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.midouz.change_phone.helper.AccountRemover;
import com.midouz.change_phone.helper.ResetPhoneStateHelper;

import java.util.Objects;

public class GeneralReceiver extends BroadcastReceiver {
    public static final String TAG = "GeneralReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        String type = intent.getStringExtra("type");
        if (Objects.equals(type, "remove_accounts")) {
//            ResetPhoneStateHelper.removeAccountData(context);
            AccountRemover.removeAccountData(context);
        }

    }
}
