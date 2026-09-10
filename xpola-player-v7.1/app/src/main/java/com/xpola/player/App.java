package com.xpola.player;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.xpola.player.Sec.ConfigLoader;
import com.xpola.player.Sec.NetworkOptimizer;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Utils.VlcEngine;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (Sec.isDebuggerAttached()) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        // Initialize Security Check
        ConfigLoader.getInstance().init(this);
        NetworkOptimizer.maybeCrash(this);
        VlcEngine.getInstance().init(this);

        sendMessage();
    }

    private void sendMessage() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Notifs");
        try {
            builder.setSmallIcon(R.drawable.app_icon)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setVibrate(new long[]{0, 200, 200, 100, 0})
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    .setAutoCancel(true);
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            if (Build.VERSION.SDK_INT < 26) {
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
            }
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Notifs";
                String description = "Notifs";
                int importance = NotificationManager.IMPORTANCE_MAX;
                @SuppressLint("WrongConstant")
                NotificationChannel channel = new NotificationChannel("Notifs", name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager =
                        getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            builder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
