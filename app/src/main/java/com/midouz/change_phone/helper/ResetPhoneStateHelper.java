package com.midouz.change_phone.helper;


import android.accounts.Account;
import android.accounts.AccountManager;
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

    public static void removeAccountData(Context context) {
        Log.i(TAG, "Starting to remove account data...");
        try {
            // Get AccountManager instance
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager.getAccounts();

            // Remove accounts via AccountManager
            for (Account account : accounts) {
                Log.d(TAG, "Removing account: " + account.name + " (" + account.type + ")");
                try {
                    // Remove account (requires MANAGE_ACCOUNTS permission)
                    if (account.type.contains("com.google")) {
                        accountManager.removeAccountExplicitly(account);
                        Log.d(TAG, "Removed Google account: " + account.name);
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "Permission denied for removing account: " + account.name, e);
                }
            }

            // Execute root commands for deeper cleanup
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // Clear Google Play Services and Play Store data
            Log.d(TAG, "Clearing Google Play Services and Play Store data...");
            os.writeBytes("pm clear com.google.android.gms\n");
            os.writeBytes("pm clear com.android.vending\n");

            // Clear contacts provider (may store account-related data)
            os.writeBytes("pm clear com.android.providers.contacts\n");

            // Clear account database
            Log.d(TAG, "Clearing accounts database...");
            String accountDbPath = "/data/system/users/0/accounts.db";
            os.writeBytes("sqlite3 " + accountDbPath + " \"DELETE FROM accounts;\"\n");
            os.writeBytes("sqlite3 " + accountDbPath + " \"DELETE FROM grants;\"\n");

            // Clear sync-related data
            Log.d(TAG, "Clearing sync-related data...");
            os.writeBytes("rm -rf /data/system/sync/*\n");

            // Clear Google Play Services and Play Store cache directories
            Log.d(TAG, "Clearing Google app caches...");
            os.writeBytes("rm -rf /data/data/com.google.android.gms/cache/*\n");
            os.writeBytes("rm -rf /data/data/com.google.android.gms/files/*\n");
            os.writeBytes("rm -rf /data/data/com.android.vending/cache/*\n");
            os.writeBytes("rm -rf /data/data/com.android.vending/files/*\n");

            // Clear credential storage
            Log.d(TAG, "Clearing credential storage...");
            os.writeBytes("rm -rf /data/misc/credentials/*\n");

            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                Log.i(TAG, "Successfully removed account data.");
            } else {
                Log.w(TAG, "Account removal process exited with code: " + exitValue);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing account data: " + e.getMessage(), e);
        }
    }

    public static void clearUserSettings() {
        Log.i(TAG, "Starting to clear user settings...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("rm -f /data/system/users/0/wallpaper\n");
            os.writeBytes("rm -f /data/system/users/0/settings_system.xml\n");
            os.writeBytes("rm -f /data/system/users/0/settings_secure.xml\n");
            os.writeBytes("rm -f /data/system/users/0/settings_global.xml\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Log.i(TAG, "Successfully cleared user settings.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing user settings: " + e.getMessage(), e);
        }
    }

    public static void clearMediaFiles() {
        Log.i(TAG, "Starting to clear media files...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("rm -rf /sdcard/DCIM/*\n");
            os.writeBytes("rm -rf /sdcard/Pictures/*\n");
            os.writeBytes("rm -rf /sdcard/Movies/*\n");
            os.writeBytes("rm -rf /sdcard/Download/*\n");
            os.writeBytes("rm -rf /sdcard/Music/*\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Log.i(TAG, "Successfully cleared media files.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing media files: " + e.getMessage(), e);
        }
    }

    public static void clearMiscUserData() {
        Log.i(TAG, "Starting to clear miscellaneous user data...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("rm -rf /data/misc/profiles/*\n");
            os.writeBytes("rm -rf /data/misc/clipboard/*\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Log.i(TAG, "Successfully cleared miscellaneous user data.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing miscellaneous user data: " + e.getMessage(), e);
        }
    }

    public static void clearBrowserData() {
        Log.i(TAG, "Starting to clear browser data...");
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("pm clear com.android.browser\n");
            os.writeBytes("pm clear com.google.android.chrome\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Log.i(TAG, "Successfully cleared browser data.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing browser data: " + e.getMessage(), e);
        }
    }

    public static void performDeviceReset(Context context) {
        Log.i(TAG, "Initiating full device reset...");
        PackageManager pm = context.getPackageManager();
        closeAllApps(pm);
        uninstallUserApps(pm);    // Uninstall user apps and their data
        clearSystemAppData(pm);   // Clear data for system apps
        removeAccountData(context); // Remove account data
        clearUserSettings();      // Clear user settings
        clearMediaFiles();        // Clear media files
        clearMiscUserData();      // Clear miscellaneous user data
        clearBrowserData();       // Clear browser data
        clearSystemCache();       // Clear system-wide cache
        Log.i(TAG, "Device reset completed.");
    }
}