package com.midouz.change_phone;

import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.midouz.change_phone.helper.FileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.TimeZone;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DeviceSpooferHook implements IXposedHookLoadPackage {
    private Properties spoofedDeviceProps;
    private Properties spoofedGeoProps;

    private void loadSpoofedDeviceProperties() {
        spoofedDeviceProps = new Properties();
        File file = new File(FileHelper.SPOOF_DEVICE_INFO_PATH);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedDeviceProps.load(fis);
                    XposedBridge.log("===Xposed===: Loaded spoofed properties from file: " + FileHelper.SPOOF_DEVICE_INFO_PATH);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed properties file not found: " + FileHelper.SPOOF_DEVICE_INFO_PATH);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed properties: " + e.getMessage());
        }
    }

    private void loadSpoofedGeoProperties() {
        spoofedGeoProps = new Properties();
        File file = new File(FileHelper.SPOOF_GEO_PATH);

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    spoofedGeoProps.load(fis);
                    XposedBridge.log("===Xposed===: Loaded spoofed geo properties from file: " + FileHelper.SPOOF_GEO_PATH);
                }
            } else {
                XposedBridge.log("===Xposed===: Spoofed geo properties file not found: " + FileHelper.SPOOF_GEO_PATH);
            }
        } catch (Exception e) {
            XposedBridge.log("===Xposed===: Failed to load spoofed geo properties: " + e.getMessage());
        }
    }

    private void applyBuildSpoofs() {
        if (spoofedDeviceProps == null || spoofedDeviceProps.isEmpty()) return;

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
                XposedBridge.log("===Xposed===: Set Build.VERSION.SDK_INT to: " + sdkInt);
            } catch (NumberFormatException e) {
                XposedBridge.log("===Xposed===: Invalid SDK_INT value: " + spoofedDeviceProps.getProperty("sdk"));
            }
        }
    }

    private void spoofField(Class<?> clazz, String fieldName, String propKey) {
        if (spoofedDeviceProps.containsKey(propKey)) {
            String value = spoofedDeviceProps.getProperty(propKey);
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            XposedBridge.log("===Xposed===: Set " + clazz.getSimpleName() + "." + fieldName + " to: " + value);
        }
    }

    private void spoofSystemProperty(String key, XC_MethodHook.MethodHookParam param) {
        if (spoofedDeviceProps == null || spoofedDeviceProps.isEmpty()) return;

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
                    param.setResult(spoofedDeviceProps.getProperty(propKey));
                    XposedBridge.log("===Xposed===: Spoofed wifi.interface.mac to: " + spoofedDeviceProps.getProperty(propKey));
                }
                return;
            default:
                return;
        }

        if (spoofedDeviceProps.containsKey(propKey)) {
            String value = spoofedDeviceProps.getProperty(propKey);
            param.setResult(value);
            XposedBridge.log("===Xposed===: Spoofed " + key + " to: " + value);
        }
    }

    private Location createSpoofedLocation(String provider) {
        if (spoofedGeoProps == null || spoofedGeoProps.isEmpty() ||
                !spoofedGeoProps.containsKey("latitude") || !spoofedGeoProps.containsKey("longitude")) {
            return null;
        }

        try {
            Location spoofedLocation = new Location(provider != null ? provider : "spoofed");
            spoofedLocation.setLatitude(Double.parseDouble(spoofedGeoProps.getProperty("latitude")));
            spoofedLocation.setLongitude(Double.parseDouble(spoofedGeoProps.getProperty("longitude")));
            spoofedLocation.setTime(System.currentTimeMillis());
            spoofedLocation.setAccuracy(10.0f); // 10 meters accuracy
            spoofedLocation.setSpeed(0.0f);     // Stationary
            return spoofedLocation;
        } catch (NumberFormatException e) {
            XposedBridge.log("===Xposed===: Invalid latitude/longitude values: " + e.getMessage());
            return null;
        }
    }

    private void spoofLocation(XC_MethodHook.MethodHookParam param) {
        Location spoofedLocation = createSpoofedLocation((String) param.args[0]);
        if (spoofedLocation != null) {
            param.setResult(spoofedLocation);
            XposedBridge.log("===Xposed===: Spoofed location to Lat: " +
                    spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                    spoofedGeoProps.getProperty("longitude"));
        }
    }

    private void spoofTimeZone(XC_MethodHook.MethodHookParam param) {
        if (spoofedGeoProps == null || spoofedGeoProps.isEmpty()) return;

        if (spoofedGeoProps.containsKey("time_zone")) {
            String timeZoneId = spoofedGeoProps.getProperty("time_zone");
            TimeZone spoofedTimeZone = TimeZone.getTimeZone(timeZoneId);
            param.setResult(spoofedTimeZone);
            XposedBridge.log("===Xposed===: Spoofed timezone to: " + timeZoneId);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("===Xposed===: Hooking package: " + lpparam.packageName);

        // Load properties once at startup
        loadSpoofedDeviceProperties();
        loadSpoofedGeoProperties();

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
                        if (spoofedDeviceProps != null) {
                            WifiInfo wifiInfo = (WifiInfo) param.getResult();
                            if (wifiInfo != null) {
                                if (spoofedDeviceProps.containsKey("mac_address")) {
                                    XposedHelpers.setObjectField(wifiInfo, "mMacAddress", spoofedDeviceProps.getProperty("mac_address"));
                                    XposedBridge.log("===Xposed===: Spoofed WifiInfo MAC address to: " + spoofedDeviceProps.getProperty("mac_address"));
                                }
                                if (spoofedDeviceProps.containsKey("ssid")) {
                                    XposedHelpers.setObjectField(wifiInfo, "mSSID", spoofedDeviceProps.getProperty("ssid"));
                                    XposedBridge.log("===Xposed===: Spoofed WifiInfo SSID to: " + spoofedDeviceProps.getProperty("ssid"));
                                }
                            }
                        }
                    }
                }
        );

        // Hook LocationManager.getLastKnownLocation
        XposedHelpers.findAndHookMethod(
                "android.location.LocationManager",
                lpparam.classLoader,
                "getLastKnownLocation",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        spoofLocation(param);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Location result = (Location) param.getResult();
                        if (result != null) {
                            XposedBridge.log("===Xposed===: LocationManager.getLastKnownLocation - Package: " + lpparam.packageName +
                                    ", Lat: " + result.getLatitude() + ", Lon: " + result.getLongitude());
                        } else {
                            XposedBridge.log("===Xposed===: LocationManager.getLastKnownLocation - Package: " + lpparam.packageName +
                                    ", Result: null");
                        }
                    }
                }
        );

        // Hook LocationManager.requestLocationUpdates
        XposedHelpers.findAndHookMethod(
                "android.location.LocationManager",
                lpparam.classLoader,
                "requestLocationUpdates",
                String.class, // provider
                long.class,   // minTime
                float.class,  // minDistance
                LocationListener.class, // listener
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("===Xposed===: LocationManager.requestLocationUpdates - Package: " + lpparam.packageName);

                        if (spoofedGeoProps == null || spoofedGeoProps.isEmpty()) return;

                        String provider = (String) param.args[0];
                        long minTime = (long) param.args[1];
                        float minDistance = (float) param.args[2];
                        LocationListener originalListener = (LocationListener) param.args[3];

                        if (originalListener != null) {
                            Location spoofedLocation = createSpoofedLocation(provider);
                            if (spoofedLocation != null) {
                                // Replace the original listener with a wrapper
                                LocationListener spoofedListener = new LocationListener() {
                                    private final Handler handler = new Handler(Looper.getMainLooper());
                                    private final Runnable updateRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            Location updatedLocation = createSpoofedLocation(provider);
                                            if (updatedLocation != null) {
                                                originalListener.onLocationChanged(updatedLocation);
                                                handler.postDelayed(this, minTime > 0 ? minTime : 5000); // Default to 5s if minTime is 0
                                            }
                                        }
                                    };

                                    @Override
                                    public void onLocationChanged(Location location) {
                                        originalListener.onLocationChanged(spoofedLocation);
                                        handler.removeCallbacks(updateRunnable);
                                        handler.postDelayed(updateRunnable, minTime > 0 ? minTime : 5000);
                                        XposedBridge.log("===Xposed===: Spoofed location update to Lat: " +
                                                spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                                                spoofedGeoProps.getProperty("longitude") + " for provider: " + provider);
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

                                // Replace the listener in the arguments
                                param.args[3] = spoofedListener;

                                // Immediately deliver the spoofed location
                                spoofedListener.onLocationChanged(spoofedLocation);
                            }
                        }
                    }
                }
        );

        // Hook FusedLocationProviderClient.getLastLocation
        XposedHelpers.findAndHookMethod(
                "com.google.android.gms.location.FusedLocationProviderClient",
                lpparam.classLoader,
                "getLastLocation",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Location spoofedLocation = createSpoofedLocation("fused");
                        if (spoofedLocation != null) {
                            Object task = XposedHelpers.callStaticMethod(
                                    XposedHelpers.findClass("com.google.android.gms.tasks.Tasks", lpparam.classLoader),
                                    "forResult", spoofedLocation
                            );
                            param.setResult(task);
                            XposedBridge.log("===Xposed===: Spoofed FusedLocationProviderClient.getLastLocation to Lat: " +
                                    spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                                    spoofedGeoProps.getProperty("longitude"));
                        }
                    }
                }
        );

        // Hook FusedLocationProviderClient.requestLocationUpdates
        XposedHelpers.findAndHookMethod(
                "com.google.android.gms.location.FusedLocationProviderClient",
                lpparam.classLoader,
                "requestLocationUpdates",
                "com.google.android.gms.location.LocationRequest",
                "com.google.android.gms.location.LocationCallback",
                "android.os.Looper",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (spoofedGeoProps == null || spoofedGeoProps.isEmpty()) return;

                        Object locationRequest = param.args[0];
                        Object originalCallback = param.args[1];
                        Looper looper = (Looper) param.args[2];

                        if (originalCallback != null) {
                            Location spoofedLocation = createSpoofedLocation("fused");
                            if (spoofedLocation != null) {
                                // Wrap the original LocationCallback
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
                                                    Location updatedLocation = createSpoofedLocation("fused");
                                                    if (updatedLocation != null) {
                                                        Object locationResult = XposedHelpers.callStaticMethod(
                                                                XposedHelpers.findClass("com.google.android.gms.location.LocationResult", lpparam.classLoader),
                                                                "create", new Object[]{new Location[]{updatedLocation}}
                                                        );
                                                        Object origCallback = XposedHelpers.getAdditionalInstanceField(spoofedCallback, "originalCallback");
                                                        XposedHelpers.callMethod(origCallback, "onLocationResult", locationResult);
                                                        handler.postDelayed(this, 5000); // Update every 5 seconds
                                                    }
                                                }
                                            };

                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param2) throws Throwable {
                                                Object origCallback = XposedHelpers.getAdditionalInstanceField(spoofedCallback, "originalCallback");
                                                Object locationResult = XposedHelpers.callStaticMethod(
                                                        XposedHelpers.findClass("com.google.android.gms.location.LocationResult", lpparam.classLoader),
                                                        "create", new Object[]{new Location[]{spoofedLocation}}
                                                );
                                                XposedHelpers.callMethod(origCallback, "onLocationResult", locationResult);
                                                handler.removeCallbacks(updateRunnable);
                                                handler.postDelayed(updateRunnable, 5000);
                                                XposedBridge.log("===Xposed===: Spoofed FusedLocationProviderClient.requestLocationUpdates to Lat: " +
                                                        spoofedGeoProps.getProperty("latitude") + ", Lon: " +
                                                        spoofedGeoProps.getProperty("longitude"));
                                                param2.setResult(null); // Prevent original updates
                                            }
                                        }
                                );

                                // Replace the callback in the arguments
                                param.args[1] = spoofedCallback;

                                // Return a successful Task to indicate the request was "processed"
                                Object task = XposedHelpers.callStaticMethod(
                                        XposedHelpers.findClass("com.google.android.gms.tasks.Tasks", lpparam.classLoader),
                                        "forResult", (Void) null
                                );
                                param.setResult(task);
                            }
                        }
                    }
                }
        );

        // Hook TimeZone.getDefault
        XposedHelpers.findAndHookMethod(
                "java.util.TimeZone",
                lpparam.classLoader,
                "getDefault",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        spoofTimeZone(param);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TimeZone result = (TimeZone) param.getResult();
                        XposedBridge.log("===Xposed===: TimeZone.getDefault - Package: " + lpparam.packageName +
                                ", TimeZone: " + (result != null ? result.getID() : "null"));
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getDeviceId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (spoofedDeviceProps.containsKey("imei")) {
                            param.setResult(spoofedDeviceProps.getProperty("imei"));
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                "android.telephony.TelephonyManager",
                lpparam.classLoader,
                "getSubscriberId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (spoofedDeviceProps.containsKey("imsi")) {
                            param.setResult(spoofedDeviceProps.getProperty("imsi"));
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                lpparam.classLoader,
                "getAdvertisingIdInfo",
                "android.content.Context",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String spoofedAdId = spoofedDeviceProps.getProperty("ad_id", java.util.UUID.randomUUID().toString());
                        Object adInfo = XposedHelpers.newInstance(
                                XposedHelpers.findClass("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info", lpparam.classLoader),
                                spoofedAdId, false // isLimitAdTrackingEnabled
                        );
                        param.setResult(adInfo);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                "android.util.DisplayMetrics",
                lpparam.classLoader,
                "getMetrics",
                "android.util.DisplayMetrics",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (spoofedDeviceProps.containsKey("width") && spoofedDeviceProps.containsKey("height")) {
                            Object metrics = param.args[0]; // The DisplayMetrics object passed as an argument
                            XposedHelpers.setIntField(metrics, "widthPixels", Integer.parseInt(spoofedDeviceProps.getProperty("width_pixels")));
                            XposedHelpers.setIntField(metrics, "heightPixels", Integer.parseInt(spoofedDeviceProps.getProperty("height_pixels")));
                            XposedBridge.log("===Xposed===: Spoofed DisplayMetrics - Width: " + spoofedDeviceProps.getProperty("width_pixels") +
                                    ", Height: " + spoofedDeviceProps.getProperty("height_pixels"));
                        }
                    }
                }
        );
    }
}