package com.midouz.change_phone.helper;

import com.midouz.change_phone.constant.SpoofPropConstant;

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
                                        spoofedDeviceProps.getProperty(SpoofPropConstant.AD_ID, java.util.UUID.randomUUID().toString()) :
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
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.SUBSCRIBER_ID)) {
                                String spoofedImsi = spoofedDeviceProps.getProperty(SpoofPropConstant.SUBSCRIBER_ID);
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
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.IMEI)) {
                                String spoofedImei = spoofedDeviceProps.getProperty(SpoofPropConstant.IMEI);
                                param.setResult(spoofedImei);
                                XposedBridge.log("===Xposed===: Successfully spoofed IMEI to: " + spoofedImei + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getDeviceId for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TelephonyManager.getLine1Number (PHONE_NUMBER)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getLine1Number",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.PHONE_NUMBER)) {
                                String spoofedPhoneNumber = spoofedDeviceProps.getProperty(SpoofPropConstant.PHONE_NUMBER);
                                param.setResult(spoofedPhoneNumber);
                                XposedBridge.log("===Xposed===: Successfully spoofed PHONE_NUMBER to: " + spoofedPhoneNumber + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getLine1Number for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TelephonyManager.getSimSerialNumber (SERIAL_SIM_NUMBER)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getSimSerialNumber",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.SERIAL_SIM_NUMBER)) {
                                String spoofedSimSerial = spoofedDeviceProps.getProperty(SpoofPropConstant.SERIAL_SIM_NUMBER);
                                param.setResult(spoofedSimSerial);
                                XposedBridge.log("===Xposed===: Successfully spoofed SERIAL_SIM_NUMBER to: " + spoofedSimSerial + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getSimSerialNumber for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TelephonyManager.getNetworkOperator (OPERATOR)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getNetworkOperator",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.OPERATOR)) {
                                String spoofedOperator = spoofedDeviceProps.getProperty(SpoofPropConstant.OPERATOR);
                                param.setResult(spoofedOperator);
                                XposedBridge.log("===Xposed===: Successfully spoofed OPERATOR to: " + spoofedOperator + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getNetworkOperator for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TelephonyManager.getNetworkOperatorName (OPERATOR_NAME)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getNetworkOperatorName",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.OPERATOR_NAME)) {
                                String spoofedOperatorName = spoofedDeviceProps.getProperty(SpoofPropConstant.OPERATOR_NAME);
                                param.setResult(spoofedOperatorName);
                                XposedBridge.log("===Xposed===: Successfully spoofed OPERATOR_NAME to: " + spoofedOperatorName + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getNetworkOperatorName for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TelephonyManager.getNetworkCountryIso (COUNTRY_ISO)
        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getNetworkCountryIso",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.COUNTRY_ISO)) {
                                String spoofedCountryIso = spoofedDeviceProps.getProperty(SpoofPropConstant.COUNTRY_ISO);
                                param.setResult(spoofedCountryIso);
                                XposedBridge.log("===Xposed===: Successfully spoofed COUNTRY_ISO to: " + spoofedCountryIso + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof TelephonyManager.getNetworkCountryIso for package: " + lpparam.packageName + ", error: " + t.getMessage());
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
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.MAC_ADDRESS)) {
                                String spoofedMacAddress = spoofedDeviceProps.getProperty(SpoofPropConstant.MAC_ADDRESS);
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
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.WIFI_NAME)) {
                                String spoofedSsid = spoofedDeviceProps.getProperty(SpoofPropConstant.WIFI_NAME);
                                param.setResult(spoofedSsid);
                                XposedBridge.log("===Xposed===: Successfully spoofed WifiInfo SSID to: " + spoofedSsid + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof WifiInfo.getSSID for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: WifiInfo.getBSSID
        XposedHelpers.findAndHookMethod(
                "android.net.wifi.WifiInfo",
                lpparam.classLoader,
                "getBSSID",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.BSSID)) {
                                String spoofedBssid = spoofedDeviceProps.getProperty(SpoofPropConstant.BSSID);
                                param.setResult(spoofedBssid);
                                XposedBridge.log("===Xposed===: Successfully spoofed WifiInfo.BSSID to: " + spoofedBssid + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof WifiInfo.getBSSID for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: WifiInfo.getIpAddress
        XposedHelpers.findAndHookMethod(
                "android.net.wifi.WifiInfo",
                lpparam.classLoader,
                "getIpAddress",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.WIFI_IP)) {
                                String spoofedIp = spoofedDeviceProps.getProperty(SpoofPropConstant.WIFI_IP);
                                try {
                                    // Convert IP string (e.g., "192.168.1.1") to int
                                    String[] octets = spoofedIp.split("\\.");
                                    int ipInt = (Integer.parseInt(octets[0]) << 24) +
                                            (Integer.parseInt(octets[1]) << 16) +
                                            (Integer.parseInt(octets[2]) << 8) +
                                            Integer.parseInt(octets[3]);
                                    param.setResult(ipInt);
                                    XposedBridge.log("===Xposed===: Successfully spoofed WifiInfo.getIpAddress to: " + spoofedIp + " for package: " + lpparam.packageName);
                                } catch (Exception e) {
                                    XposedBridge.log("===Xposed===: Invalid WIFI_IP format: " + spoofedIp + ", error: " + e.getMessage());
                                }
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof WifiInfo.getIpAddress for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: WebSettings.getDefaultUserAgent (USER_AGENT)
        try {
            XposedHelpers.findAndHookMethod(
                    "android.webkit.WebSettings",
                    lpparam.classLoader,
                    "getDefaultUserAgent",
                    "android.content.Context",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.USER_AGENT)) {
                                    String spoofedUserAgent = spoofedDeviceProps.getProperty(SpoofPropConstant.USER_AGENT);
                                    param.setResult(spoofedUserAgent);
                                    XposedBridge.log("===Xposed===: Successfully spoofed WebSettings.getDefaultUserAgent to: " + spoofedUserAgent + " for package: " + lpparam.packageName);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof WebSettings.getDefaultUserAgent for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );
            XposedBridge.log("===Xposed===: Successfully hooked WebSettings.getDefaultUserAgent for package: " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to initialize hook for WebSettings.getDefaultUserAgent for package: " + lpparam.packageName + ", error: " + t.getMessage());
        }

        // Hook: Settings.Secure.getString (ANDROID_ID)
        XposedHelpers.findAndHookMethod(
                "android.provider.Settings$Secure",
                lpparam.classLoader,
                "getString",
                "android.content.ContentResolver",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (spoofedDeviceProps != null && spoofedDeviceProps.containsKey(SpoofPropConstant.ANDROID_ID)) {
                                String spoofedAndroidId = spoofedDeviceProps.getProperty(SpoofPropConstant.ANDROID_ID);
                                param.setResult(spoofedAndroidId);
                                XposedBridge.log("===Xposed===: Successfully spoofed Settings.Secure.getString(ANDROID_ID) to: " + spoofedAndroidId + " for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof Settings.Secure.getString(ANDROID_ID) for package: " + lpparam.packageName + ", error: " + t.getMessage());
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
            // Existing Build fields
            spoofField(android.os.Build.class, "MODEL", SpoofPropConstant.MODEL);
            spoofField(android.os.Build.class, "BRAND", SpoofPropConstant.BRAND);
            spoofField(android.os.Build.class, "MANUFACTURER", SpoofPropConstant.MANUFACTURER);
            spoofField(android.os.Build.class, "SERIAL", SpoofPropConstant.SERIAL);
            spoofField(android.os.Build.class, "DEVICE", SpoofPropConstant.DEVICE);
            spoofField(android.os.Build.class, "PRODUCT", SpoofPropConstant.PRODUCT);
            spoofField(android.os.Build.class, "FINGERPRINT", SpoofPropConstant.FINGERPRINT);
            spoofField(android.os.Build.VERSION.class, "RELEASE", SpoofPropConstant.RELEASE);

            // New Build fields
            spoofField(android.os.Build.class, "SERIAL", SpoofPropConstant.ANDROID_SERIAL);
            spoofField(android.os.Build.class, "HARDWARE", SpoofPropConstant.HARDWARE);
            spoofField(android.os.Build.class, "ID", SpoofPropConstant.ID);
            spoofField(android.os.Build.class, "HOST", SpoofPropConstant.HOST);
            spoofField(android.os.Build.class, "RADIO", SpoofPropConstant.RADIO);
            spoofField(android.os.Build.class, "BOOTLOADER", SpoofPropConstant.BOOTLOADER);
            spoofField(android.os.Build.class, "DISPLAY", SpoofPropConstant.DISPLAY);
            spoofField(android.os.Build.class, "BOARD", SpoofPropConstant.BOARD);
            spoofField(android.os.Build.VERSION.class, "CODENAME", SpoofPropConstant.CODENAME);

            if (spoofedDeviceProps.containsKey(SpoofPropConstant.SDK)) {
                try {
                    int sdkInt = Integer.parseInt(spoofedDeviceProps.getProperty(SpoofPropConstant.SDK, "0"));
                    XposedHelpers.setStaticIntField(android.os.Build.VERSION.class, "SDK_INT", sdkInt);
                    XposedBridge.log("===Xposed===: Successfully set Build.VERSION.SDK_INT to: " + sdkInt);
                } catch (NumberFormatException e) {
                    XposedBridge.log("===Xposed===: Invalid SDK_INT value in spoofed properties: " + spoofedDeviceProps.getProperty(SpoofPropConstant.SDK) + ", error: " + e.getMessage());
                }
            }

            // Spoof VERSION_CHROME by appending to FINGERPRINT
            if (spoofedDeviceProps.containsKey(SpoofPropConstant.VERSION_CHROME)) {
                String versionChrome = spoofedDeviceProps.getProperty(SpoofPropConstant.VERSION_CHROME, "");
                String fingerprint = spoofedDeviceProps.getProperty(SpoofPropConstant.FINGERPRINT,"");
                if (!fingerprint.contains(versionChrome)) {
                    String modifiedFingerprint = fingerprint + ":" + versionChrome;
                    XposedHelpers.setStaticObjectField(android.os.Build.class, "FINGERPRINT", modifiedFingerprint);
                    XposedBridge.log("===Xposed===: Appended VERSION_CHROME to FINGERPRINT: " + modifiedFingerprint);
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
                    propKey = SpoofPropConstant.MODEL;
                    break;
                case "ro.product.brand":
                    propKey = SpoofPropConstant.BRAND;
                    break;
                case "ro.product.manufacturer":
                    propKey = SpoofPropConstant.MANUFACTURER;
                    break;
                case "ro.product.device":
                    propKey = SpoofPropConstant.DEVICE;
                    break;
                case "ro.product.name":
                    propKey = SpoofPropConstant.PRODUCT;
                    break;
                case "ro.build.fingerprint":
                    propKey = SpoofPropConstant.FINGERPRINT;
                    break;
                case "ro.build.version.release":
                    propKey = SpoofPropConstant.RELEASE;
                    break;
                case "wifi.interface.mac":
                    propKey = SpoofPropConstant.MAC_ADDRESS;
                    break;
                case "ro.opengl.vendor":
                    propKey = SpoofPropConstant.GL_VENDOR;
                    break;
                case "ro.opengl.renderer":
                    propKey = SpoofPropConstant.GL_RENDER;
                    break;
                case "ro.build.version":
                    propKey = SpoofPropConstant.OS_VERSION;
                    break;
                case "ro.mac.hardware":
                    propKey = SpoofPropConstant.MAC_HARDWARE;
                    break;
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