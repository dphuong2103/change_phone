package com.midouz.change_phone.helper;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class ScreenSizeHelper {
    public static int getScreenWidth(Activity activity) {
        WindowManager wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.widthPixels; // Screen width in pixels
    }

    public static int getScreenHeight(Activity activity) {
        WindowManager wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.heightPixels; // Screen height in pixels
    }
}
