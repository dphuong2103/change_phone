package com.midouz.change_phone;

import android.net.wifi.WifiInfo;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DeviceSpooferHook implements IXposedHookLoadPackage {
    private static final String SPOOF_FILE_PATH = SpoofController.SPOOF_FILE_PATH;
    private Properties spoofedProps;

    private void loadSpoofedProperties() {
        spoofedProps = new Properties();
        File file = new File(SPOOF_FILE_PATH);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedProps.load(fis);
                    XposedBridge.log("===Xposed===: Loaded spoofed properties from file: " + SPOOF_FILE_PATH);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed properties file not found: " + SPOOF_FILE_PATH);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed properties: " + e.getMessage());
        }
    }

    private void applyBuildSpoofs() {
        if (spoofedProps == null || spoofedProps.isEmpty()) return;

        spoofField(android.os.Build.class, "MODEL", "model");
        spoofField(android.os.Build.class, "BRAND", "brand");
        spoofField(android.os.Build.class, "MANUFACTURER", "manufacturer");
        spoofField(android.os.Build.class, "SERIAL", "serial");
        spoofField(android.os.Build.class, "DEVICE", "device");
        spoofField(android.os.Build.class, "PRODUCT", "product");
        spoofField(android.os.Build.class, "FINGERPRINT", "fingerprint");
        spoofField(android.os.Build.VERSION.class, "RELEASE", "release");

        if (spoofedProps.containsKey("sdk")) {
            try {
                int sdkInt = Integer.parseInt(spoofedProps.getProperty("sdk", "0"));
                XposedHelpers.setStaticIntField(android.os.Build.VERSION.class, "SDK_INT", sdkInt);
                XposedBridge.log("===Xposed===: Set Build.VERSION.SDK_INT to: " + sdkInt);
            } catch (NumberFormatException e) {
                XposedBridge.log("===Xposed===: Invalid SDK_INT value: " + spoofedProps.getProperty("sdk"));
            }
        }
    }

    private void spoofField(Class<?> clazz, String fieldName, String propKey) {
        if (spoofedProps.containsKey(propKey)) {
            String value = spoofedProps.getProperty(propKey);
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            XposedBridge.log("===Xposed===: Set " + clazz.getSimpleName() + "." + fieldName + " to: " + value);
        }
    }

    private void spoofSystemProperty(String key, XC_MethodHook.MethodHookParam param) {
        if (spoofedProps == null || spoofedProps.isEmpty()) return;

        String propKey;
        switch (key) {
            case "ro.product.model":
                propKey = "model";
                break;
            case "ro.product.brand":
                propKey = "brand";
                break;
            case "ro.product.manufacturer":
                propKey = "manufacturer";
                break;
            case "ro.product.device":
                propKey = "device";
                break;
            case "ro.product.name":
                propKey = "product";
                break;
            case "ro.build.fingerprint":
                propKey = "fingerprint";
                break;
            case "ro.build.version.release":
                propKey = "release";
                break;
            case "wifi.interface.mac":
                propKey = "mac_address"; // Assign to avoid null
                if (spoofedProps.containsKey(propKey)) {
                    param.setResult(spoofedProps.getProperty(propKey));
                    XposedBridge.log("===Xposed===: Spoofed wifi.interface.mac to: " + spoofedProps.getProperty(propKey));
                }
                return;
            default:
                return;
        }

        if (spoofedProps.containsKey(propKey)) {
            String value = spoofedProps.getProperty(propKey);
            param.setResult(value);
            XposedBridge.log("===Xposed===: Spoofed " + key + " to: " + value);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("===Xposed===: Hooking package: " + lpparam.packageName);

        // Load properties once at startup
        loadSpoofedProperties();

        // Apply Build field spoofs
        applyBuildSpoofs();

        // Hook SystemProperties.get (single argument)
        XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "get",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        spoofSystemProperty(key, param);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        String result = (String) param.getResult();
                        XposedBridge.log("===Xposed===: SystemProperties.get - Package: " + lpparam.packageName +
                                ", Key: " + key + ", Result: " + result);
                    }
                }
        );

        // Hook SystemProperties.get (with default value)
        XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "get",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        spoofSystemProperty(key, param);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        String defaultValue = (String) param.args[1];
                        String result = (String) param.getResult();
                        XposedBridge.log("===Xposed===: SystemProperties.get (with default) - Package: " + lpparam.packageName +
                                ", Key: " + key + ", Default: " + defaultValue + ", Result: " + result);
                    }
                }
        );

        // Hook WifiManager.getConnectionInfo to spoof MAC address
        XposedHelpers.findAndHookMethod(
                "android.net.wifi.WifiManager",
                lpparam.classLoader,
                "getConnectionInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (spoofedProps != null) {
                            WifiInfo wifiInfo = (WifiInfo) param.getResult();
                            if (wifiInfo != null) {
                                if (spoofedProps.containsKey("mac_address")) {
                                    XposedHelpers.setObjectField(wifiInfo, "mMacAddress", spoofedProps.getProperty("mac_address"));
                                    XposedBridge.log("===Xposed===: Spoofed WifiInfo MAC address to: " + spoofedProps.getProperty("mac_address"));
                                }
                                if (spoofedProps.containsKey("ssid")) {
                                    XposedHelpers.setObjectField(wifiInfo, "mSSID", spoofedProps.getProperty("ssid"));
                                    XposedBridge.log("===Xposed===: Spoofed WifiInfo SSID to: " + spoofedProps.getProperty("ssid"));
                                }
                            }
                        }
                    }
                }
        );
    }
}