package com.midouz.change_phone.helper;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SpoofGeoHelper {
    private static Properties spoofedGeoProps;

    public static void spoofGeo(XC_LoadPackage.LoadPackageParam lpparam) {
        loadSpoofedGeoProperties();

        // Hook: isFromMockProvider (simple, low-risk, but adding try-catch for consistency)
        XposedHelpers.findAndHookMethod(
                "android.location.Location",
                lpparam.classLoader,
                "isFromMockProvider",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            param.setResult(false);
                            XposedBridge.log("===Xposed===: Successfully forced isFromMockProvider to false for package: " + lpparam.packageName);
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to hook isFromMockProvider for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: LocationManager.requestLocationUpdates (complex, needs try-catch)
        XposedHelpers.findAndHookMethod(
                "android.location.LocationManager",
                lpparam.classLoader,
                "requestLocationUpdates",
                String.class, long.class, float.class, LocationListener.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            XposedBridge.log("===Xposed===: Hooking LocationManager.requestLocationUpdates for package: " + lpparam.packageName);
                            if (spoofedGeoProps == null || spoofedGeoProps.isEmpty()) {
                                XposedBridge.log("===Xposed===: No spoofed geo properties available for package: " + lpparam.packageName);
                                return;
                            }

                            String provider = (String) param.args[0];
                            long minTime = (long) param.args[1];
                            float minDistance = (float) param.args[2];
                            LocationListener originalListener = (LocationListener) param.args[3];

                            if (originalListener != null) {
                                Location spoofedLocation = createSpoofedLocation(provider);
                                if (spoofedLocation != null) {
                                    LocationListener spoofedListener = new LocationListener() {
                                        private final Handler handler = new Handler(Looper.getMainLooper());
                                        private final Runnable updateRunnable = new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Location updatedLocation = createSpoofedLocation(provider);
                                                    if (updatedLocation != null) {
                                                        originalListener.onLocationChanged(updatedLocation);
                                                        handler.postDelayed(this, minTime > 0 ? minTime : 5000);
                                                    }
                                                } catch (Throwable t) {
                                                    XposedBridge.log("===Xposed===: Error in updateRunnable for package: " + lpparam.packageName + ", error: " + t.getMessage());
                                                }
                                            }
                                        };

                                        @Override
                                        public void onLocationChanged(Location location) {
                                            try {
                                                originalListener.onLocationChanged(spoofedLocation);
                                                handler.removeCallbacks(updateRunnable);
                                                handler.postDelayed(updateRunnable, minTime > 0 ? minTime : 5000);
                                                XposedBridge.log("===Xposed===: Spoofed location update to Lat: " +
                                                        spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                                                        spoofedGeoProps.getProperty("longitude") + " for provider: " + provider +
                                                        ", package: " + lpparam.packageName);
                                            } catch (Throwable t) {
                                                XposedBridge.log("===Xposed===: Error in onLocationChanged for package: " + lpparam.packageName + ", error: " + t.getMessage());
                                            }
                                        }

                                        @Override
                                        public void onStatusChanged(String provider, int status, Bundle extras) {
                                            originalListener.onStatusChanged(provider, status, extras);
                                        }

                                        @Override
                                        public void onProviderEnabled(String provider) {
                                            originalListener.onProviderEnabled(provider);
                                        }

                                        @Override
                                        public void onProviderDisabled(String provider) {
                                            originalListener.onProviderDisabled(provider);
                                        }
                                    };
                                    param.args[3] = spoofedListener;
                                    spoofedListener.onLocationChanged(spoofedLocation);
                                } else {
                                    XposedBridge.log("===Xposed===: Failed to create spoofed location for package: " + lpparam.packageName);
                                }
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to hook LocationManager.requestLocationUpdates for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: TimeZone.getDefault
        XposedHelpers.findAndHookMethod(
                "java.util.TimeZone",
                lpparam.classLoader,
                "getDefault",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            spoofTimeZone(param);
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof timezone for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            TimeZone result = (TimeZone) param.getResult();
                            XposedBridge.log("===Xposed===: TimeZone.getDefault result for package: " + lpparam.packageName +
                                    ", TimeZone: " + (result != null ? result.getID() : "null"));
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Error logging TimeZone.getDefault result for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: LocationManager.getLastKnownLocation
        XposedHelpers.findAndHookMethod(
                "android.location.LocationManager",
                lpparam.classLoader,
                "getLastKnownLocation",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            spoofLocation(param);
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Failed to spoof LocationManager.getLastKnownLocation for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Location result = (Location) param.getResult();
                            if (result != null) {
                                XposedBridge.log("===Xposed===: LocationManager.getLastKnownLocation result for package: " + lpparam.packageName +
                                        ", Lat: " + result.getLatitude() + ", Lon: " + result.getLongitude());
                            } else {
                                XposedBridge.log("===Xposed===: LocationManager.getLastKnownLocation returned null for package: " + lpparam.packageName);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("===Xposed===: Error logging LocationManager.getLastKnownLocation result for package: " + lpparam.packageName + ", error: " + t.getMessage());
                        }
                    }
                }
        );

        // Hook: FusedLocationProviderClient.getLastLocation
        try {
            XposedHelpers.findAndHookMethod(
                    "com.google.android.gms.location.FusedLocationProviderClient",
                    lpparam.classLoader,
                    "getLastLocation",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Location spoofedLocation = createSpoofedLocation("fused");
                                if (spoofedLocation != null) {
                                    Object task = XposedHelpers.callStaticMethod(
                                            XposedHelpers.findClass("com.google.android.gms.tasks.Tasks", lpparam.classLoader),
                                            "forResult", spoofedLocation
                                    );
                                    param.setResult(task);
                                    XposedBridge.log("===Xposed===: Successfully spoofed FusedLocationProviderClient.getLastLocation to Lat: " +
                                            spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                                            spoofedGeoProps.getProperty("longitude") + " for package: " + lpparam.packageName);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to spoof FusedLocationProviderClient.getLastLocation for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );
            XposedBridge.log("===Xposed===: Successfully hooked FusedLocationProviderClient.getLastLocation for package: " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to initialize hook for FusedLocationProviderClient.getLastLocation for package: " + lpparam.packageName + ", error: " + t.getMessage());
        }

        // Hook: FusedLocationProviderClient.requestLocationUpdates
        try {
            XposedHelpers.findAndHookMethod(
                    "com.google.android.gms.location.FusedLocationProviderClient",
                    lpparam.classLoader,
                    "requestLocationUpdates",
                    "com.google.android.gms.location.LocationRequest",
                    "com.google.android.gms.location.LocationCallback",
                    "android.os.Looper",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                if (spoofedGeoProps == null || spoofedGeoProps.isEmpty()) {
                                    XposedBridge.log("===Xposed===: No spoofed geo properties available for FusedLocationProviderClient.requestLocationUpdates, package: " + lpparam.packageName);
                                    return;
                                }

                                Object locationRequest = param.args[0];
                                Object originalCallback = param.args[1];
                                Looper looper = (Looper) param.args[2];

                                if (originalCallback != null) {
                                    Location spoofedLocation = createSpoofedLocation("fused");
                                    if (spoofedLocation != null) {
                                        Object spoofedCallback = XposedHelpers.newInstance(
                                                XposedHelpers.findClass("com.google.android.gms.location.LocationCallback", lpparam.classLoader)
                                        );
                                        XposedHelpers.setAdditionalInstanceField(spoofedCallback, "originalCallback", originalCallback);

                                        XposedHelpers.findAndHookMethod(
                                                spoofedCallback.getClass(),
                                                "onLocationResult",
                                                "com.google.android.gms.location.LocationResult",
                                                new XC_MethodHook() {
                                                    private final Handler handler = new Handler(looper != null ? looper : Looper.getMainLooper());
                                                    private final Runnable updateRunnable = new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                Location updatedLocation = createSpoofedLocation("fused");
                                                                if (updatedLocation != null) {
                                                                    Object locationResult = XposedHelpers.callStaticMethod(
                                                                            XposedHelpers.findClass("com.google.android.gms.location.LocationResult", lpparam.classLoader),
                                                                            "create", new Object[]{new Location[]{updatedLocation}}
                                                                    );
                                                                    Object origCallback = XposedHelpers.getAdditionalInstanceField(spoofedCallback, "originalCallback");
                                                                    XposedHelpers.callMethod(origCallback, "onLocationResult", locationResult);
                                                                    handler.postDelayed(this, 5000);
                                                                }
                                                            } catch (Throwable t) {
                                                                XposedBridge.log("===Xposed===: Error in FusedLocation updateRunnable for package: " + lpparam.packageName + ", error: " + t.getMessage());
                                                            }
                                                        }
                                                    };

                                                    @Override
                                                    protected void beforeHookedMethod(MethodHookParam param2) {
                                                        try {
                                                            Object origCallback = XposedHelpers.getAdditionalInstanceField(spoofedCallback, "originalCallback");
                                                            Object locationResult = XposedHelpers.callStaticMethod(
                                                                    XposedHelpers.findClass("com.google.android.gms.location.LocationResult", lpparam.classLoader),
                                                                    "create", new Object[]{new Location[]{spoofedLocation}}
                                                            );
                                                            XposedHelpers.callMethod(origCallback, "onLocationResult", locationResult);
                                                            handler.removeCallbacks(updateRunnable);
                                                            handler.postDelayed(updateRunnable, 5000);
                                                            XposedBridge.log("===Xposed===: Successfully spoofed FusedLocationProviderClient.requestLocationUpdates to Lat: " +
                                                                    spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                                                                    spoofedGeoProps.getProperty("longitude") + " for package: " + lpparam.packageName);
                                                            param2.setResult(null);
                                                        } catch (Throwable t) {
                                                            XposedBridge.log("===Xposed===: Failed to spoof onLocationResult for package: " + lpparam.packageName + ", error: " + t.getMessage());
                                                        }
                                                    }
                                                }
                                        );

                                        param.args[1] = spoofedCallback;
                                        Object task = XposedHelpers.callStaticMethod(
                                                XposedHelpers.findClass("com.google.android.gms.tasks.Tasks", lpparam.classLoader),
                                                "forResult", (Void) null
                                        );
                                        param.setResult(task);
                                    }
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("===Xposed===: Failed to hook FusedLocationProviderClient.requestLocationUpdates for package: " + lpparam.packageName + ", error: " + t.getMessage());
                            }
                        }
                    }
            );
            XposedBridge.log("===Xposed===: Successfully hooked FusedLocationProviderClient.requestLocationUpdates for package: " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to initialize hook for FusedLocationProviderClient.requestLocationUpdates for package: " + lpparam.packageName + ", error: " + t.getMessage());
        }
    }

    private static void loadSpoofedGeoProperties() {
        spoofedGeoProps = new Properties();
        File file = new File(FileHelper.SPOOF_GEO_PATH);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedGeoProps.load(fis);
                    XposedBridge.log("===Xposed===: Successfully loaded spoofed geo properties from file: " + FileHelper.SPOOF_GEO_PATH);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed geo properties file not found at: " + FileHelper.SPOOF_GEO_PATH);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed geo properties from " + FileHelper.SPOOF_GEO_PATH + ", error: " + e.getMessage());
        }
    }

    private static Location createSpoofedLocation(String provider) {
        if (spoofedGeoProps == null || spoofedGeoProps.isEmpty() ||
                !spoofedGeoProps.containsKey("latitude") || !spoofedGeoProps.containsKey("longitude")) {
            XposedBridge.log("===Xposed===: Cannot create spoofed location: missing or invalid geo properties");
            return null;
        }

        try {
            Location spoofedLocation = new Location(provider != null ? provider : "spoofed");
            spoofedLocation.setLatitude(Double.parseDouble(spoofedGeoProps.getProperty("latitude")));
            spoofedLocation.setLongitude(Double.parseDouble(spoofedGeoProps.getProperty("longitude")));
            spoofedLocation.setTime(System.currentTimeMillis());
            spoofedLocation.setAccuracy(10.0f);
            spoofedLocation.setSpeed(0.0f);
            XposedBridge.log("===Xposed===: Created spoofed location with Lat: " +
                    spoofedGeoProps.getProperty("latitude") + ", Lon: " + spoofedGeoProps.getProperty("longitude"));
            return spoofedLocation;
        } catch (NumberFormatException e) {
            XposedBridge.log("===Xposed===: Failed to create spoofed location due to invalid latitude/longitude: " + e.getMessage());
            return null;
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Unexpected error creating spoofed location: " + t.getMessage());
            return null;
        }
    }

    private static void spoofLocation(XC_MethodHook.MethodHookParam param) {
        try {
            Location spoofedLocation = createSpoofedLocation((String) param.args[0]);
            if (spoofedLocation != null) {
                param.setResult(spoofedLocation);
                XposedBridge.log("===Xposed===: Successfully spoofed LocationManager.getLastKnownLocation to Lat: " +
                        spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                        spoofedGeoProps.getProperty("longitude"));
            }
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to spoof location in spoofLocation for package: " + param.thisObject + ", error: " + t.getMessage());
        }
    }

    private static void spoofTimeZone(XC_MethodHook.MethodHookParam param) {
        try {
            if (spoofedGeoProps == null || spoofedGeoProps.isEmpty()) {
                XposedBridge.log("===Xposed===: No spoofed geo properties available for timezone spoofing");
                return;
            }

            if (spoofedGeoProps.containsKey("time_zone")) {
                String timeZoneId = spoofedGeoProps.getProperty("time_zone");
                TimeZone tz = TimeZone.getTimeZone(timeZoneId);
                param.setResult(tz);
                XposedBridge.log("===Xposed===: Successfully spoofed timezone to: " + timeZoneId);
            } else {
                XposedBridge.log("===Xposed===: No time_zone key found in spoofed geo properties");
            }
        } catch (Throwable t) {
            XposedBridge.log("===Xposed===: Failed to spoof timezone in spoofTimeZone: " + t.getMessage());
        }
    }
}