package com.midouz.change_phone.helper;

import android.net.wifi.WifiInfo;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SpoofDeviceInfoHelper {
    private static Properties spoofedDeviceProps;

    public static void spoofDeviceInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        loadSpoofedDeviceProperties();
        try {
            applyBuildSpoofs();
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to apply Build spoofs for package: " + lpparam.packageName + ", error: " + t.getMessage());
        }

        // Hook: SystemProperties.get (single parameter)
        XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "get",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            String key = (String) param.args[0];
                            spoofSystemProperty(key, param);
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof SystemProperties.get (single param) for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            String key = (String) param.args[0];
                            String result = (String) param.getResult();
                            XposedBridge.log("===Xposed===: SystemProperties.get result for package: " + lpparam.packageName +
                                    ", Key: " + key + ", Result: " + (result != null ? result : "null"));
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Error logging SystemProperties.get result for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: SystemProperties.get (with default value)
        XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "get",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            String key = (String) param.args[0];
                            spoofSystemProperty(key, param);
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof SystemProperties.get (with default) for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            String key = (String) param.args[0];
                            String defaultValue = (String) param.args[1];
                            String result = (String) param.getResult();
                            XposedBridge.log("===Xposed===: SystemProperties.get (with default) result for package: " + lpparam.packageName +
                                    ", Key: " + key + ", Default: " + (defaultValue != null ? defaultValue : "null") + ", Result: " + (result != null ? result : "null"));
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Error logging SystemProperties.get (with default) result for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: AdvertisingIdClient.getAdvertisingIdInfo
        try {
            XposedHelpers.findAndHookMethod(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                    lpparam.classLoader,
                    "getAdvertisingIdInfo",
                    "android.content.Context",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                String spoofedAdId = spoofedDeviceProps != null ?
                                        spoofedDeviceProps.getProperty("ad_id", java.util.UUID.randomUUID().toString()) :
                                        java.util.UUID.randomUUID().toString();
                                Object adInfo = XposedHelpers.newInstance(
                                        XposedHelpers.findClass("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info", lpparam.classLoader),
                                        spoofedAdId, false // isLimitAdTrackingEnabled
                                );
                                param.setResult(adInfo);
                                XposedBridge.log("===Xposed===: Successfully spoofed AdvertisingId to: " + spoofedAdId + " for package: " + lpparam.packageName);
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof AdvertisingIdClient.getAdvertisingIdInfo for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );
            XposedBridge.log("===Xposed===: Successfully hooked AdvertisingIdClient.getAdvertisingIdInfo for package: " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to initialize hook for AdvertisingIdClient.getAdvertisingIdInfo for package: " + lpparam.packageName + ", error: " + t.getMessage());
        }

        // Hook: TelephonyManager.getSubscriberId (IMSI)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getSubscriberId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey("imsi")) {
                                String spoofedImsi = spoofedDeviceProps.getProperty("imsi");
                                param.setResult(spoofedImsi);
                                XposedBridge.log("===Xposed===: Successfully spoofed IMSI to: " + spoofedImsi + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getSubscriberId for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TelephonyManager.getDeviceId (IMEI)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getDeviceId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey("imei")) {
                                String spoofedImei = spoofedDeviceProps.getProperty("imei");
                                param.setResult(spoofedImei);
                                XposedBridge.log("===Xposed===: Successfully spoofed IMEI to: " + spoofedImei + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getDeviceId for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: WifiInfo.getMacAddress
        XposedHelpers.findAndHookMethod(
                "android.net.wifi.WifiInfo",
                lpparam.classLoader,
                "getMacAddress",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey("ssid")) {
                                String spoofedMacAddress = spoofedDeviceProps.getProperty("mac_address");
                                param.setResult(spoofedMacAddress);
                                XposedBridge.log("===Xposed===: Successfully spoofed WifiInfo.MacAddress to: " + spoofedMacAddress + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof WifiInfo.MacAddress for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: WifiInfo.getSSID
        XposedHelpers.findAndHookMethod(
                "android.net.wifi.WifiInfo",
                lpparam.classLoader,
                "getSSID",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey("ssid")) {
                                String spoofedSsid = spoofedDeviceProps.getProperty("ssid");
                                param.setResult(spoofedSsid);
                                XposedBridge.log("===Xposed===: Successfully spoofed WifiInfo SSID to: " + spoofedSsid + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof WifiInfo.getSSID for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );
    }

    private static void loadSpoofedDeviceProperties() {
        spoofedDeviceProps = new Properties();
        File file = new File(FileHelper.SPOOF_DEVICE_INFO_PATH);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedDeviceProps.load(fis);
                    XposedBridge.log("===Xposed===: Successfully loaded spoofed device properties from file: " + FileHelper.SPOOF_DEVICE_INFO_PATH);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed device properties file not found at: " + FileHelper.SPOOF_DEVICE_INFO_PATH);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed device properties from " + FileHelper.SPOOF_DEVICE_INFO_PATH + ", error: " + e.getMessage());
        }
    }

    private static void applyBuildSpoofs() {
        if (spoofedDeviceProps == null || spoofedDeviceProps.isEmpty()) {
            XposedBridge.log("===Xposed===: No spoofed device properties available for Build spoofs");
            return;
        }

        try {
            spoofField(android.os.Build.class, "MODEL", "model");
            spoofField(android.os.Build.class, "BRAND", "brand");
            spoofField(android.os.Build.class, "MANUFACTURER", "manufacturer");
            spoofField(android.os.Build.class, "SERIAL", "serial");
            spoofField(android.os.Build.class, "DEVICE", "device");
            spoofField(android.os.Build.class, "PRODUCT", "product");
            spoofField(android.os.Build.class, "FINGERPRINT", "fingerprint");
            spoofField(android.os.Build.VERSION.class, "RELEASE", "release");

            if (spoofedDeviceProps.containsKey("sdk")) {
                try {
                    int sdkInt = Integer.parseInt(spoofedDeviceProps.getProperty("sdk", "0"));
                    XposedHelpers.setStaticIntField(android.os.Build.VERSION.class, "SDK_INT", sdkInt);
                    XposedBridge.log("===Xposed===: Successfully set Build.VERSION.SDK_INT to: " + sdkInt);
                } catch (NumberFormatException e) {
                    XposedBridge.log("===Xposed===: Invalid SDK_INT value in spoofed properties: " + spoofedDeviceProps.getProperty("sdk") + ", error: " + e.getMessage());
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Unexpected error in applyBuildSpoofs: " + t.getMessage());
        }
    }

    private static void spoofField(Class<?> clazz, String fieldName, String propKey) {
        try {
            if (spoofedDeviceProps.containsKey(propKey)) {
                String value = spoofedDeviceProps.getProperty(propKey);
                XposedHelpers.setStaticObjectField(clazz, fieldName, value);
                XposedBridge.log("===Xposed===: Successfully set " + clazz.getSimpleName() + "." + fieldName + " to: " + value);
            }
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to spoof field " + clazz.getSimpleName() + "." + fieldName + " for key " + propKey + ", error: " + t.getMessage());
        }
    }

    private static void spoofSystemProperty(String key, XC_MethodHook.MethodHookParam param) {
        if (spoofedDeviceProps == null || spoofedDeviceProps.isEmpty()) {
            XposedBridge.log("===Xposed===: No spoofed device properties available for SystemProperties spoofing");
            return;
        }

        try {
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
                    propKey = "mac_address";
                    if (spoofedDeviceProps.containsKey(propKey)) {
                        String spoofedMac = spoofedDeviceProps.getProperty(propKey);
                        param.setResult(spoofedMac);
                        XposedBridge.log("===Xposed===: Successfully spoofed wifi.interface.mac to: " + spoofedMac);
                    }
                    return;
                default:
                    return;
            }

            if (spoofedDeviceProps.containsKey(propKey)) {
                String value = spoofedDeviceProps.getProperty(propKey);
                param.setResult(value);
                XposedBridge.log("===Xposed===: Successfully spoofed SystemProperty " + key + " to: " + value);
            }
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to spoof SystemProperty " + key + ", error: " + t.getMessage());
        }
    }
}