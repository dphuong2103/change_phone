package com.midouz.change_phone.model;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Data
@RequiredArgsConstructor
@Builder
public class SpoofingInfo {
    private final String model;
    private final String brand;
    private final String manufacturer;
    private final String serialNo;
    private final String device;
    private final String productName;
    private final String releaseVersion;
    private final String sdkVersion;
    private final String macAddress;
    private final String fingerprint;
    private final String androidId;
    private final String ssid; // Added SSID field
    private final double latitude;
    private final double longitude;
    private static final List<SpoofingInfo> DEVICE_INFO_LIST = new ArrayList<>();

    static {
//        DEVICE_INFO_LIST.add(new SpoofingInfo(
//                "Redmi Note 7", "Xiaomi", "Xiaomi", "XMI" + generateSerialSuffix(7),
//                "lavender", "lavender", "10", "29", "RN7:FG",
//                "xiaomi/lavender/lavender:10/QKQ1.190910.002/V12.5.1.0.QFGMIXM:user/release-keys",
//                generateAndroidId()
//        ));
//
//        DEVICE_INFO_LIST.add(new SpoofingInfo(
//                "Realme 6", "Realme", "Oppo", "RM" + generateSerialSuffix(7),
//                "RMX2001", "RMX2001", "10", "29", "R6:HI",
//                "realme/RMX2001/RMX2001:10/QKQ1.200209.002/1591234567:user/release-keys",
//                generateAndroidId()
//        ));
//
//        DEVICE_INFO_LIST.add(new SpoofingInfo(
//                "P30 Lite", "Huawei", "Huawei", "HUA" + generateSerialSuffix(7),
//                "marie", "marie", "10", "29", "P30L:JK",
//                "huawei/marie/marie:10/HUAWEIMAR-LX1M/10.0.0.195(C432E5R1P1):user/release-keys",
//                generateAndroidId()
//        ));
    }

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

    public static SpoofingInfo getRandomDevice() {
        return DEVICE_INFO_LIST.get(new Random().nextInt(DEVICE_INFO_LIST.size()));
    }

}
