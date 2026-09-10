package com.xpola.player.Sec;

import android.content.Context;
import android.content.pm.PackageManager;

public class AdsTamperCheck {

    public static boolean isAdmobRemoved(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean isTV = pm.hasSystemFeature("android.software.leanback");

        if (isTV) {
            return false; // على التلفزيون تجاهل الفحص دائماً
        }

        try {
            Class.forName("com.google.android.gms.ads.AdView");
            return false; // مكتبة AdMob موجودة
        } catch (ClassNotFoundException e) {
            return true;  // مكتبة AdMob محذوفة → تلاعب
        }
    }
}
