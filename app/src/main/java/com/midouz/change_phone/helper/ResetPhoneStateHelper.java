package com.midouz.change_phone.helper;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

public class ResetPhoneStateHelper {
    private static final String TAG = "ResetPhoneStateHelper"; // Log tag for easy filtering

    public static void closeAllApps(PackageManager pm) {
        Log.i(TAG, "Starting to close all apps...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            List<String> excludedApps = Arrays.asList(
                    "org.meowcat.edxposed.manager",
                    "com.midouz.change_phone",  // Your LSPosed module
                    "com.topjohnwu.magisk",     // Magisk
                    "android",                  // Core system
                    "com.android.systemui"      // SystemUI (optional, to keep UI alive)
            );

            for (ApplicationInfo app : apps) {
                if (!excludedApps.contains(app.packageName)) {
                    Log.d(TAG, "Killing app: " + app.packageName);
                    os.writeBytes("am kill " + app.packageName + "\n");
                }
            }
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Log.i(TAG, "Finished closing apps.");
        } catch (Exception e) {
            Log.e(TAG, "Error closing apps: " + e.getMessage(), e);
        }
    }

    public static void uninstallUserApps(PackageManager pm) {
        Log.i(TAG, "Starting to uninstall user apps...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            List<String> excludedApps = Arrays.asList(
                    "org.meowcat.edxposed.manager",
                    "com.midouz.change_phone",  // Your LSPosed module
                    "com.topjohnwu.magisk"      // Magisk
            );

            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !excludedApps.contains(app.packageName)) {
                    Log.d(TAG, "Uninstalling app: " + app.packageName);
                    os.writeBytes("pm uninstall " + app.packageName + "\n");
                }
            }
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.i(TAG, "Successfully uninstalled user apps.");
            } else {
                Log.w(TAG, "Uninstall process exited with code: " + exitCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error uninstalling user apps: " + e.getMessage(), e);
        }
    }

    public static void clearSystemAppData(PackageManager pm) {
        Log.i(TAG, "Starting to clear system app data...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            List<String> excludedApps = Arrays.asList(
                    "org.meowcat.edxposed.manager",
                    "com.midouz.change_phone",  // Your LSPosed module
                    "com.topjohnwu.magisk",     // Magisk
                    "android"                   // Core Android system package
            );

            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && !excludedApps.contains(app.packageName)) {
                    Log.d(TAG, "Clearing data for system app: " + app.packageName);
                    os.writeBytes("pm clear " + app.packageName + "\n");
                }
            }
            Log.d(TAG, "Clearing app-specific caches under /data/data/");
            os.writeBytes("find /data/data/ -type d -name 'cache' -not -path '/data/data/com.midouz.change_phone/*' -not -path '/data/data/com.topjohnwu.magisk/*' -exec rm -rf {} +\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.i(TAG, "Successfully cleared system app data and caches.");
            } else {
                Log.w(TAG, "Clear system app data process exited with code: " + exitCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing system app data: " + e.getMessage(), e);
        }
    }

    public static void clearSystemCache() {
        Log.i(TAG, "Starting to clear system cache...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            Log.d(TAG, "Clearing /data/cache/*");
            os.writeBytes("rm -rf /data/cache/*\n");
            Log.d(TAG, "Clearing /data/dalvik-cache/*");
            os.writeBytes("rm -rf /data/dalvik-cache/*\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.i(TAG, "Successfully cleared system cache.");
            } else {
                Log.w(TAG, "Clear system cache process exited with code: " + exitCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing system cache: " + e.getMessage(), e);
        }
    }

    public static void performDeviceReset(Context context) {
        Log.i(TAG, "Initiating full device reset...");
        PackageManager pm = context.getPackageManager();
        closeAllApps(pm);
        uninstallUserApps(pm);    // Uninstall user apps and their data
        clearSystemAppData(pm);   // Clear data for system apps
        clearSystemCache();       // Clear system-wide cache
        Log.i(TAG, "Device reset completed.");
    }
}