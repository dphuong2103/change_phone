package com.midouz.change_phone.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class DeviceInfo {
    // Getter methods
    // Fields
    private final String model;
    private final String brand;
    private final String manufacturer;
    private final String serialNo;
    private final String device;
    private final String productName;
    private final String releaseVersion;
    private final String sdkVersion;
    private final String macSuffix;
    private final String fingerprint;
    private final String androidId;

    // Constructor
    public DeviceInfo(String model, String brand, String manufacturer, String serialNo,
                      String device, String productName, String releaseVersion,
                      String sdkVersion, String macSuffix, String fingerprint,
                      String androidId) {
        this.model = model;
        this.brand = brand;
        this.manufacturer = manufacturer;
        this.serialNo = serialNo;
        this.device = device;
        this.productName = productName;
        this.releaseVersion = releaseVersion;
        this.sdkVersion = sdkVersion;
        this.macSuffix = macSuffix;
        this.fingerprint = fingerprint;
        this.androidId = androidId;
    }

    // Device list
    private static final List<DeviceInfo> DEVICE_INFO_LIST = new ArrayList<>();

    static {
        // Xiaomi Redmi Note 7 (Android 10)
        DEVICE_INFO_LIST.add(new DeviceInfo(
                "Redmi Note 7",
                "Xiaomi",
                "Xiaomi",
                "XMI" + generateSerialSuffix(7),
                "lavender",
                "lavender",
                "10",
                "29",
                "RN7:FG",
                "xiaomi/lavender/lavender:10/QKQ1.190910.002/V12.5.1.0.QFGMIXM:user/release-keys",
                generateAndroidId()
        ));

        // Realme 6 (Android 10)
        DEVICE_INFO_LIST.add(new DeviceInfo(
                "Realme 6",
                "Realme",
                "Oppo",
                "RM" + generateSerialSuffix(7),
                "RMX2001",
                "RMX2001",
                "10",
                "29",
                "R6:HI",
                "realme/RMX2001/RMX2001:10/QKQ1.200209.002/1591234567:user/release-keys",
                generateAndroidId()
        ));

        // Huawei P30 Lite (Android 10)
        DEVICE_INFO_LIST.add(new DeviceInfo(
                "P30 Lite",
                "Huawei",
                "Huawei",
                "HUA" + generateSerialSuffix(7),
                "marie",
                "marie",
                "10",
                "29",
                "P30L:JK",
                "huawei/marie/marie:10/HUAWEIMAR-LX1M/10.0.0.195(C432E5R1P1):user/release-keys",
                generateAndroidId()
        ));
    }

    // Utility methods
    private static String generateSerialSuffix(int length) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String generateAndroidId() {
        String chars = "0123456789abcdef";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Method to get random device
    public static DeviceInfo getRandomDevice() {
        Random random = new Random();
        int index = random.nextInt(DEVICE_INFO_LIST.size());
        return DEVICE_INFO_LIST.get(index);
    }
}