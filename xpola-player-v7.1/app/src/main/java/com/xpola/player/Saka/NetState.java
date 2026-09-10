package com.xpola.player.Saka;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetState {
    public static String getK3() { return "9W"; }

    public static boolean check(Context context) {

        try {

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkCapabilities caps =
                    cm.getNetworkCapabilities(cm.getActiveNetwork());

            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);

        } catch (Exception e) {
            return false;
        }
    }
}