package com.midouz.change_phone.helper;

import android.content.Context;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

public class FileHelper {
    public static final String SPOOF_DEVICE_INFO_PATH = "/data/local/tmp/spoofed_device_info.properties";
    public static final String SPOOF_GEO_PATH = "/data/local/tmp/spoofed_geo.properties";

    public static void saveDeviceInfoToFile(Context context, Properties props) throws Exception {
        File file = new File(SPOOF_DEVICE_INFO_PATH);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mkdir -p " + parentDir.getAbsolutePath() + "\n");
            os.writeBytes("chmod 777 " + parentDir.getAbsolutePath() + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        }

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String tempFile = context.getFilesDir() + "/temp_spoofed_values.properties";

            // Write to a temp file in app directory first
            try (FileWriter writer = new FileWriter(tempFile)) {
                props.store(writer, "Spoofed Device Info");
            }

            // Copy to /data/local/tmp and set permissions
            os.writeBytes("cat " + tempFile + " > " + SPOOF_DEVICE_INFO_PATH + "\n");
            os.writeBytes("chmod 644 " + SPOOF_DEVICE_INFO_PATH + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            new File(tempFile).delete(); // Clean up temp file
            Log.i("SpoofController", "Successfully saved spoofed values to: " + SPOOF_DEVICE_INFO_PATH);
        } catch (Exception e) {
            Log.e("SpoofController", "Error writing to file: " + e.getMessage());
            throw e;
        }
    }

    public static void saveGeoInfoToFile(Context context, Properties props) throws Exception {
        File file = new File(SPOOF_GEO_PATH);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mkdir -p " + parentDir.getAbsolutePath() + "\n");
            os.writeBytes("chmod 777 " + parentDir.getAbsolutePath() + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        }

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String tempFile = context.getFilesDir() + "/temp_spoofed_geo.properties";

            // Write to a temp file in app directory first
            try (FileWriter writer = new FileWriter(tempFile)) {
                props.store(writer, "Spoofed Geo Info");
            }

            // Copy to /data/local/tmp and set permissions
            os.writeBytes("cat " + tempFile + " > " + SPOOF_GEO_PATH + "\n");
            os.writeBytes("chmod 644 " + SPOOF_GEO_PATH + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            new File(tempFile).delete(); // Clean up temp file
            Log.i("SpoofController", "Successfully saved geo values to: " + SPOOF_GEO_PATH);
        } catch (Exception e) {
            Log.e("SpoofController", "Error writing to file: " + e.getMessage());
            throw e;
        }
    }


}
