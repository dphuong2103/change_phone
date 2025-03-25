package com.midouz.change_phone;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DeviceSpooferHook implements IXposedHookLoadPackage {
    private Properties spoofedProps;

    private String getSpoofFilePath() {
        return SpoofController.getSpoofFilePath();
    }

    // Load properties from file
    private void loadSpoofedProperties() {
        spoofedProps = new Properties();
        String filePath = getSpoofFilePath();
        File file = new File(filePath);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedProps.load(fis);
                    XposedBridge.log("===Xposed===: Loaded spoofed properties from file: " + filePath);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed properties file not found: " + filePath);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed properties: " + e.getMessage());
        }
    }

    // Apply spoofed values to Build fields
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

    // Helper method to spoof a static field
    private void spoofField(Class<?> clazz, String fieldName, String propKey) {
        if (spoofedProps.containsKey(propKey)) {
            String value = spoofedProps.getProperty(propKey);
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            XposedBridge.log("===Xposed===: Set " + clazz.getSimpleName() + "." + fieldName + " to: " + value);
        }
    }

    // Apply spoofed values to SystemProperties
    private void spoofSystemProperty(String key, XC_MethodHook.MethodHookParam param) {
        if (spoofedProps == null || spoofedProps.isEmpty()) return;

        String propKey;
        switch (key) {
            case "ro.product.model": propKey = "model"; break;
            case "ro.product.brand": propKey = "brand"; break;
            case "ro.product.manufacturer": propKey = "manufacturer"; break;
            case "ro.product.device": propKey = "device"; break;
            case "ro.product.name": propKey = "product"; break;
            case "ro.build.fingerprint": propKey = "fingerprint"; break;
            case "ro.build.version.release": propKey = "release"; break;
            default: return;
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

        // Apply Build field spoofs for every package
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
    }
}