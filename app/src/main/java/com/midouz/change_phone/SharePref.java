package com.midouz.change_phone;

import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.XposedHelpers;

public class SharePref {
    private static final String PREFS_NAME = "XposedSpoofPrefs";
    private static SharedPreferences prefs;

    public static SharedPreferences getPrefs() {
        Context systemContext = (Context) XposedHelpers.callMethod(
                XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.app.ActivityThread", null),
                        "currentActivityThread"
                ),
                "getSystemContext"
        );
        if(prefs == null) {
            systemContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        return prefs;
    }
}
