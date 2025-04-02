package com.midouz.change_phone;

import com.midouz.change_phone.helper.SpoofDeviceInfoHelper;
import com.midouz.change_phone.helper.SpoofGeoHelper;
import com.midouz.change_phone.helper.SpoofScreenSizeHelper;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
public class DeviceSpooferHook implements IXposedHookLoadPackage {


    @Override
    public void handleLoadPackage(LoadPackageParam lpParam) throws Throwable {
        XposedBridge.log("===Xposed===: Hooking package: " + lpParam.packageName);
        SpoofGeoHelper.spoofGeo(lpParam);

        SpoofDeviceInfoHelper.spoofDeviceInfo(lpParam);

        SpoofScreenSizeHelper.spoofScreenSize(lpParam);

    }

}