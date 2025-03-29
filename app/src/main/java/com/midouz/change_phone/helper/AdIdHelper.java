package com.midouz.change_phone.helper;
import android.content.Context;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdIdHelper {

    public interface AdIdCallback {
        void onAdIdReceived(String adId);
        void onError(Exception e);
    }

    public static void getAdvertisingId(Context context, AdIdCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Get Advertising ID
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                if (adInfo != null) {
                    String adId = adInfo.getId();
                    boolean isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();

                    // Call the callback with the result
                    if (callback != null) {
                        callback.onAdIdReceived(adId);
                    }
                }
            } catch (IOException | IllegalStateException | NullPointerException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            } catch (GooglePlayServicesRepairableException e) {
                throw new RuntimeException(e);
            } catch (GooglePlayServicesNotAvailableException e) {
                throw new RuntimeException(e);
            }
        });
    }
}