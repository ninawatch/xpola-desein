package com.xpola.player.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.xpola.player.R;

public class DeviceUtils {

    public static boolean isGooglePlayInstalled(Context context) {

        try {
            context.getPackageManager().getPackageInfo("com.android.vending", 0);
            return true;

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void checkDevice(Context context) {

        if (!isGooglePlayInstalled(context)) {
            return;
        }

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_incompatible_device);
        dialog.setCancelable(false);

        Button btn = dialog.findViewById(R.id.btnPlayStore);

        btn.setOnClickListener(v -> {

            String packageName = "com.xpola.player";

            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + packageName)));
            } catch (Exception e) {

                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            }

            ((Activity) context).finish();

        });

        dialog.show();
    }
}