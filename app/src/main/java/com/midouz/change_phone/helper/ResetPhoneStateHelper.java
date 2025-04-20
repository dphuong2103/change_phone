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

            // Log initial accounts
            Log.d(TAG, "Found " + accounts.length + " accounts before removal:");
            for (Account account : accounts) {
                Log.d(TAG, "Account: " + account.name + " (" + account.type + ")");
            }

            // Start root process
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // Disconnect from internet
            Log.d(TAG, "Disabling network...");
            os.writeBytes("svc wifi disable\n");
            os.writeBytes("svc data disable\n");

            // Stop and disable Google services
            Log.d(TAG, "Stopping and disabling Google services...");
            os.writeBytes("am force-stop com.google.android.gms\n");
            os.writeBytes("am force-stop com.android.vending\n");
//            os.writeBytes("am force-stop com.google.android.gsf\n");
            os.writeBytes("pm disable com.google.android.gms\n");
//            os.writeBytes("pm disable com.google.android.gsf\n");

            // Remove accounts via AccountManager
            for (Account account : accounts) {
                if (account.type.contains("com.google")) {
                    Log.d(TAG, "Removing account: " + account.name);
                    try {
                        accountManager.removeAccountExplicitly(account);
                        Log.d(TAG, "Removed account: " + account.name);
                    } catch (SecurityException e) {
                        Log.w(TAG, "Permission denied for account: " + account.name, e);
                    }
                }
            }

            // Clear app data
            Log.d(TAG, "Clearing app data...");
            os.writeBytes("pm clear com.google.android.gms\n");
            os.writeBytes("pm clear com.android.vending\n");
            os.writeBytes("pm clear com.google.android.gsf\n");
            os.writeBytes("pm clear com.android.providers.contacts\n");

            // Clear account databases
            Log.d(TAG, "Clearing account databases...");
            os.writeBytes("sqlite3 /data/system/users/0/accounts.db \"DELETE FROM accounts; DELETE FROM grants;\"\n");
            os.writeBytes("sqlite3 /data/system_ce/0/accounts_ce.db \"DELETE FROM accounts; DELETE FROM grants;\"\n");
            os.writeBytes("sqlite3 /data/system_de/0/accounts_de.db \"DELETE FROM accounts; DELETE FROM grants;\"\n");
            os.writeBytes("sqlite3 /data/system/sync/accounts.db \"DELETE FROM accounts;\"\n");

            // Clear app caches and files
            Log.d(TAG, "Clearing app caches and files...");
            os.writeBytes("rm -rf /data/data/com.google.android.gms/*\n");
            os.writeBytes("rm -rf /data/data/com.android.vending/*\n");
            os.writeBytes("rm -rf /data/data/com.google.android.gsf/*\n");
            os.writeBytes("rm -rf /data/user_de/0/com.google.android.gms/*\n");
            os.writeBytes("rm -rf /data/user_de/0/com.android.vending/*\n");
            os.writeBytes("rm -rf /data/user_de/0/com.google.android.gsf/*\n");

            // Clear sync, backup, and credential data
            Log.d(TAG, "Clearing sync, backup, and credential data...");
            os.writeBytes("rm -rf /data/system/sync/*\n");
            os.writeBytes("rm -rf /data/backup/*\n");
            os.writeBytes("rm -rf /data/misc/backup/*\n");
            os.writeBytes("rm -rf /data/misc/credentials/*\n");
            os.writeBytes("rm -rf /data/misc/keychain/*\n");
            os.writeBytes("rm -rf /data/system/users/0/*.account\n");

            // Disable sync and backup
            Log.d(TAG, "Disabling sync and backup...");
            os.writeBytes("content call --uri content://com.google.android.gsf.accountsettings/sync --method disableSync\n");
            os.writeBytes("bmgr enable false\n");



            // Re-enable Google services
            Log.d(TAG, "Re-g Google services...");
            os.writeBytes("pm enable com.google.android.gms\n");//            os.writeBytes("g\n");

            // Re-enable network
            Log.d(TAG, "Re-enabling network...");
            os.writeBytes("svc wifi enable\n");
            os.writeBytes("svc data enable\n");

            // Close root process
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            Log.d(TAG, "Root process exit value: " + exitValue);

            // Verify accounts
            accounts = accountManager.getAccounts();
            Log.d(TAG, "Found " + accounts.length + " accounts after removal:");
            for (Account account : accounts) {
                Log.d(TAG, "Remaining account: " + account.name + " (" + account.type + ")");
            }

            // Optional: Reboot device
            // os.writeBytes("reboot\n");

        } catch (Exception e) {
            Log.e(TAG, "Error removing account data: " + e.getMessage(), e);
        }
    }

    public static void performDeviceReset(Context context) {
        Log.i(TAG, "Initiating full device reset...");
        PackageManager pm = context.getPackageManager();
        closeAllApps(pm);
        uninstallUserApps(pm);    // Uninstall user apps and their data
        clearSystemAppData(pm);   // Clear data for system apps
        removeAccountData(context);
        clearSystemCache();       // Clear system-wide cache
        Log.i(TAG, "Device reset completed.");
    }
}