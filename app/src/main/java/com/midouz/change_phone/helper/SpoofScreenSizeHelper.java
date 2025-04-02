package com.midouz.change_phone.helper;

import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SpoofScreenSizeHelper {
    private static Properties spoofedScreenSizeProps;

    private static void loadSpoofedDeviceProperties() {
        spoofedScreenSizeProps = new Properties();
        File file = new File(FileHelper.SPOOF_DEVICE_INFO_PATH);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedScreenSizeProps.load(fis);
                    XposedBridge.log("===Xposed===: Successfully loaded spoofed screen size properties from file: " + FileHelper.SPOOF_DEVICE_INFO_PATH);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed screen size properties file not found at: " + FileHelper.SPOOF_DEVICE_INFO_PATH);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed screen size properties from " + FileHelper.SPOOF_DEVICE_INFO_PATH + ", error: " + e.getMessage());
        }
    }

    public static void spoofScreenSize(XC_LoadPackage.LoadPackageParam lpparam) {
        loadSpoofedDeviceProperties();
        try {
            XposedBridge.log("===Xposed===: Starting screen size spoofing for package: " + lpparam.packageName + " with properties: " + spoofedScreenSizeProps);

            if (spoofedScreenSizeProps == null || !spoofedScreenSizeProps.containsKey("width") || !spoofedScreenSizeProps.containsKey("height")) {
                XposedBridge.log("===Xposed===: Missing width or height in properties, skipping screen size spoofing for package: " + lpparam.packageName);
                return;
            }

            int width, height;
            try {
                width = Integer.parseInt(spoofedScreenSizeProps.getProperty("width"));
                height = Integer.parseInt(spoofedScreenSizeProps.getProperty("height"));
            } catch (NumberFormatException e) {
                XposedBridge.log("===Xposed===: Invalid width or height values in properties for package: " + lpparam.packageName + ", error: " + e.getMessage());
                return;
            }

            // Hook: Display.getMetrics
            XposedHelpers.findAndHookMethod(
                    "android.view.Display",
                    lpparam.classLoader,
                    "getMetrics",
                    DisplayMetrics.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                DisplayMetrics metrics = (DisplayMetrics) param.args[0];
                                if (metrics != null) {
                                    metrics.widthPixels = width;
                                    metrics.heightPixels = height;
                                    XposedBridge.log("===Xposed===: Successfully spoofed Display.getMetrics to width: " + width + ", height: " + height + " for package: " + lpparam.packageName);
                                } else {
                                    XposedBridge.log("===Xposed===: DisplayMetrics argument is null in getMetrics for package: " + lpparam.packageName);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof Display.getMetrics for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );

            // Hook: Display.getSize
            XposedHelpers.findAndHookMethod(
                    "android.view.Display",
                    lpparam.classLoader,
                    "getSize",
                    android.graphics.Point.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                android.graphics.Point point = (android.graphics.Point) param.args[0];
                                if (point != null) {
                                    point.x = width;
                                    point.y = height;
                                    XposedBridge.log("===Xposed===: Successfully spoofed Display.getSize to width: " + width + ", height: " + height + " for package: " + lpparam.packageName);
                                } else {
                                    XposedBridge.log("===Xposed===: Point argument is null in getSize for package: " + lpparam.packageName);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof Display.getSize for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );

            // Hook: Display.getWidth
            XposedHelpers.findAndHookMethod(
                    "android.view.Display",
                    lpparam.classLoader,
                    "getWidth",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                param.setResult(width);
                                XposedBridge.log("===Xposed===: Successfully spoofed Display.getWidth to: " + width + " for package: " + lpparam.packageName);
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof Display.getWidth for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );

            // Hook: Display.getHeight
            XposedHelpers.findAndHookMethod(
                    "android.view.Display",
                    lpparam.classLoader,
                    "getHeight",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                param.setResult(height);
                                XposedBridge.log("===Xposed===: Successfully spoofed Display.getHeight to: " + height + " for package: " + lpparam.packageName);
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof Display.getHeight for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );

            // Hook: Resources.getDisplayMetrics
            XposedHelpers.findAndHookMethod(
                    "android.content.res.Resources",
                    lpparam.classLoader,
                    "getDisplayMetrics",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                DisplayMetrics metrics = (DisplayMetrics) param.getResult();
                                if (metrics != null) {
                                    metrics.widthPixels = width;
                                    metrics.heightPixels = height;
                                    XposedBridge.log("===Xposed===: Successfully spoofed Resources.getDisplayMetrics to width: " + width + ", height: " + height + " for package: " + lpparam.packageName);
                                } else {
                                    XposedBridge.log("===Xposed===: DisplayMetrics result is null in Resources.getDisplayMetrics for package: " + lpparam.packageName);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof Resources.getDisplayMetrics for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );

            XposedBridge.log("===Xposed===: Successfully initialized all screen size spoofing hooks for package: " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to initialize screen size spoofing for package: " + lpparam.packageName + ", error: " + t.getMessage());
        }
    }
}