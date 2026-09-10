package com.xpola.player.Notifs;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.xpola.player.R;
import com.xpola.player.Ui.Activities.Player;
import com.xpola.player.Ui.Activities.Splash;
import com.xpola.player.Ui.Activities.m3Player;
import com.xpola.player.Utils.Parser.IntentParser;
import com.xpola.player.Utils.Parser.M3uParser;
import com.xpola.player.Utils.Utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FCMNotifs extends FirebaseMessagingService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String NOTIF_NAME = "Notifs";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        sendMessage(message);
    }

    private void sendMessage(RemoteMessage remoteMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(FCMNotifs.this, NOTIF_NAME);

        try {
            // تحديد صوت مخصص من مجلد res/raw
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.custom_sound);

            builder.setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(Objects.requireNonNull(remoteMessage.getData()).get("title"))
                    .setContentText(Objects.requireNonNull(remoteMessage.getData()).get("message"))
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setContentIntent(goIntent(remoteMessage))
                    .setVibrate(new long[]{0, 200, 200, 100, 0})
                    .setSound(soundUri) // <-- الصوت المخصص هنا
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
            }

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // إنشاء قناة الإشعارات مع الصوت المخصص لأندرويد 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                @SuppressLint("WrongConstant")
                NotificationChannel channel = new NotificationChannel(NOTIF_NAME, NOTIF_NAME, importance);
                channel.setDescription(NOTIF_NAME);

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            try {
                new Thread(() -> {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(remoteMessage.getData().get("image")).openConnection();
                        connection.connect();
                        InputStream is = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        handler.post(() -> {
                            builder.setLargeIcon(bitmap);
                            builder.setStyle(new NotificationCompat.BigPictureStyle()
                                    .setBigContentTitle(Objects.requireNonNull(remoteMessage.getData()).get("title"))
                                    .bigPicture(bitmap)
                                    .bigLargeIcon((Bitmap) null));
                            nm.notify(Utils.getRandom(1, 999999), builder.build());
                        });
                    } catch (Exception e) {
                        handler.post(() -> nm.notify(Utils.getRandom(1, 999999), builder.build()));
                    }
                }).start();
            } catch (Exception e) {
                nm.notify(Utils.getRandom(1, 999999), builder.build());
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    private PendingIntent goIntent(@NonNull RemoteMessage message) {
        Map<String, String> extras = message.getData();
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (extras.containsKey("type")) {
            intent.putExtra("title", Utils.getValueString(extras, "title", ""));
            HashMap<String, String> headers = new HashMap<>();

            if (!Utils.getValueString(extras, M3uParser.USER_AGENT, "").isEmpty())
                headers.put(M3uParser.USER_AGENT, Utils.getValueString(extras, M3uParser.USER_AGENT, ""));
            if (!Utils.getValueString(extras, M3uParser.REFERER, "").isEmpty())
                headers.put(M3uParser.REFERER, Utils.getValueString(extras, M3uParser.REFERER, ""));

            headers.put(M3uParser.PLAYER_TYPE, Utils.getValueString(extras, "type", ""));
            intent.putExtra(M3uParser.HEADERS, Utils.objectToString(headers));

            if (!Utils.getValueString(extras, M3uParser.URL, "").isEmpty()) {
                intent.putExtra(M3uParser.URL, Utils.getValueString(extras, M3uParser.URL, ""));
                if (!Utils.getValueString(extras, M3uParser.DRM_LICENSE, "").isEmpty() &&
                        !Utils.getValueString(extras, M3uParser.DRM_SCHEME, "").isEmpty()) {
                    intent.putExtra(M3uParser.DRM_LICENSE, Utils.getValueString(extras, M3uParser.DRM_LICENSE, ""));
                    intent.putExtra(M3uParser.DRM_SCHEME, Utils.getValueString(extras, M3uParser.DRM_SCHEME, ""));
                }
                intent.setData(Uri.parse(Utils.getValueString(extras, M3uParser.URL, "")));
            }

            if (!Utils.getValueString(extras, M3uParser.SERVERS, "").isEmpty()) {
                String s = Utils.getValueString(extras, M3uParser.SERVERS, "[]");
                s = Utils.objectToString(Utils.isArray(s) ? s : Utils.decrypt(s, this));
                if (!s.isEmpty() && !s.equals("[]"))
                    intent.putExtra(M3uParser.SERVERS, IntentParser.createServers(s, this));
            }

            if (Utils.getValueString(extras, "type", "").isEmpty()) {
                intent.setClass(getBaseContext(), Splash.class);
            } else if (Utils.getValueString(extras, "type", "").equalsIgnoreCase("m3u")) {
                intent.setClass(getBaseContext(), m3Player.class);
            } else if (!Utils.getValueString(extras, "type", "").equalsIgnoreCase("other")) {
                intent.setClass(getBaseContext(), Player.class);
            }
        } else {
            intent.setClass(getBaseContext(), Splash.class);
        }

        return PendingIntent.getActivity(
                getBaseContext(),
                100,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
    }
}
