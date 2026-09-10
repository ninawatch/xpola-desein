package com.xpola.player.Utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;

public class ProxyEngine {

    // يجب أن يكون هذا المفتاح مطابقاً تماماً للموجود في تطبيق البروكسي
    private static final String SHARED_KEY = "XPOLA_INTERNAL_KEY_2026";

    public interface OnProxyReadyListener {
        void onReady();
        void onFailure();
    }

    public static void start(Context context, OnProxyReadyListener listener) {
        new Thread(() -> {
            // 1. فحص هل البروكسي يعمل بالفعل لتجنب إعادة التشغيل
            if (isPortOpen("127.0.0.1", 63000)) {
                new Handler(Looper.getMainLooper()).post(listener::onReady);
                return;
            }

            try {
                // 2. توليد التوكن الديناميكي (بناءً على الوقت الحالي)
                long timestamp = System.currentTimeMillis() / 1000;
                String secureToken = generateSecureToken(timestamp);

                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.xpola.proxy", "com.xpola.proxy.ProxyService"));

                // 3. إرسال التوكن والوقت كبيانات إضافية (Extras)
                intent.putExtra("auth_token", secureToken);
                intent.putExtra("auth_ts", timestamp);

                Log.d("XPOLA_ENGINE", "Starting Proxy with Token: " + secureToken);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }

                // 4. محاولة الفحص لمدة 5 ثوانٍ (Polling) للتأكد من استجابة البروكسي
                int retry = 0;
                while (retry < 10) {
                    Thread.sleep(500);
                    if (isPortOpen("127.0.0.1", 63000)) {
                        new Handler(Looper.getMainLooper()).post(listener::onReady);
                        return;
                    }
                    retry++;
                }
                new Handler(Looper.getMainLooper()).post(listener::onFailure);

            } catch (Exception e) {
                Log.e("XPOLA", "Error starting proxy app", e);
                new Handler(Looper.getMainLooper()).post(listener::onFailure);
            }
        }).start();
    }

    /**
     * دالة توليد التوكن باستخدام SHA-256
     * تقوم بدمج المفتاح السري مع الوقت واسم الحزمة
     */
    private static String generateSecureToken(long ts) {
        try {
            // ندمج المفتاح مع الوقت لضمان تغير الكود كل ثانية
            String input = SHARED_KEY + "|" + ts + "|com.xpola.player";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e("XPOLA_SEC", "Token Generation Failed", e);
            return "";
        }
    }

    private static boolean isPortOpen(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}