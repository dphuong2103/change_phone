package com.midouz.change_phone.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class AccountRemover {
    private static final String TAG = "AccountRemover";

    public static boolean removeAccountData(@NonNull Context context) {
        Log.i(TAG, "Starting to remove account data...");
        Process process = null;
        DataOutputStream os = null;
        try {
            // Check for root access
            if (!isRootAvailable()) {
                Log.e(TAG, "Root access not available");
                return false;
            }

            // Get AccountManager instance
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager.getAccounts();
            Log.d(TAG, "Found " + accounts.length + " accounts before removal:");
            for (Account account : accounts) {
                Log.d(TAG, "Account: " + account.name + " (" + account.type + ")");
            }

            // Start root process
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            // Disconnect from internet
            Log.d(TAG, "Disabling network...");
            os.writeBytes("svc wifi disable\n");
            os.writeBytes("svc data disable\n");

            // Stop Google services
            Log.d(TAG, "Stopping Google services...");
            os.writeBytes("am force-stop com.google.android.gms\n");
            os.writeBytes("am force-stop com.android.vending\n");
            os.writeBytes("am force-stop com.google.android.gsf\n");

            // Remove accounts via AccountManager (fallback, may fail due to permissions)
            for (Account account : accounts) {
                if (account.type.contains("com.google")) {
                    Log.d(TAG, "Attempting to remove account: " + account.name);
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

            // Clear account databases (with existence checks)
            Log.d(TAG, "Clearing account databases...");
            String[] dbPaths = {
                    "/data/system/users/0/accounts.db",
                    "/data/system_ce/0/accounts_ce.db",
                    "/data/system_de/0/accounts_de.db",
                    "/data/system/sync/accounts.db"
            };
            for (String dbPath : dbPaths) {
                if (new File(dbPath).exists()) {
                    os.writeBytes("sqlite3 " + dbPath + " \"DELETE FROM accounts; DELETE FROM grants;\"\n");
                } else {
                    Log.w(TAG, "Database not found: " + dbPath);
                }
            }

            // Clear app caches (more selective)
            Log.d(TAG, "Clearing app caches...");
            String[] cachePaths = {
                    "/data/data/com.google.android.gms/cache/*",
                    "/data/data/com.android.vending/cache/*",
                    "/data/data/com.google.android.gsf/cache/*",
                    "/data/user_de/0/com.google.android.gms/cache/*",
                    "/data/user_de/0/com.android.vending/cache/*",
                    "/data/user_de/0/com.google.android.gsf/cache/*"
            };
            for (String path : cachePaths) {
                os.writeBytes("rm -rf " + path + "\n");
            }

            // Clear sync and backup data
            Log.d(TAG, "Clearing sync and backup data...");
            os.writeBytes("rm -rf /data/system/sync/*\n");
            os.writeBytes("rm -rf /data/backup/*\n");
            os.writeBytes("rm -rf /data/misc/backup/*\n");

            // Disable sync (optional, skip if not supported)
            Log.d(TAG, "Disabling sync...");
            try {
                os.writeBytes("content call --uri content://com.google.android.gsf.accountsettings/sync --method disableSync\n");
            } catch (Exception e) {
                Log.w(TAG, "Failed to disable sync: " + e.getMessage());
            }

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

            // Trigger Google Play Services restart
            Log.d(TAG, "Triggering Google Play Services restart...");
            triggerGooglePlayServices(context);

            // Recommend reboot
            Log.i(TAG, "Account removal complete. Reboot recommended.");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "IO error during account removal: " + e.getMessage(), e);
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "Process interrupted: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close DataOutputStream: " + e.getMessage());
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    // Check for root access
    private static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            Log.w(TAG, "Root check failed: " + e.getMessage());
            return false;
        }
    }

    // Trigger Google Play Services restart without requiring an Activity
    private static void triggerGooglePlayServices(@NonNull Context context) {
        try {
            // Check if Google Play Services is available
            GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
            int resultCode = googleApi.isGooglePlayServicesAvailable(context);
            if (resultCode == ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services is already running");
                return;
            }

            // Attempt to trigger Google Play Services by starting a service intent
            Intent intent = new Intent();
            intent.setPackage("com.google.android.gms");
            intent.setAction("com.google.android.gms.common.CHECK_GOOGLE_PLAY_SERVICES");
            context.startService(intent);
            Log.d(TAG, "Sent intent to trigger Google Play Services");

        } catch (Exception e) {
            Log.w(TAG, "Failed to trigger Google Play Services: " + e.getMessage());
            // Fallback: System will restart Google Play Services when needed
            Log.d(TAG, "Google Play Services will restart automatically when accessed by an app");
        }
    }
}