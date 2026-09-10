package com.xpola.player.Ui.Activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;
import com.xpola.player.Connections.LoadSettings;
import com.xpola.player.R;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Prefs;

public class Splash extends AppCompatActivity {
    private ImageView img;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔒 حماية التطبيق
        com.xpola.player.Sec.Pack.checkAndCloseIfTampered(this);

        setContentView(R.layout.splash_screen);

        if (Sec.isDebuggerAttached()) {
            finishAffinity();
            System.exit(0);
        }

        prefs = new Prefs(this);
        img = findViewById(R.id.splash_icon);

        LoadSettings.getSettingsJSONFile(this);
        Animation();

        // ============================
        // 🚫 إذا لا يوجد Google Play Services → لا نستدعي Firebase
        // ============================
        if (isGmsAvailable()) {
            try {
                FirebaseApp.initializeApp(this);

                // اشتراك Firebase فقط إذا كان الجهاز يدعم GMS
                FirebaseMessaging.getInstance().subscribeToTopic("xpola_player");
                checkDatabaseAndSubscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ============================
        // ✔ OneSignal يعمل بدون Google Play Services
        // ============================
        OneSignal.initWithContext(
                this,
                "b57039c8-7021-47f3-927c-e2aa028ea168"
        );
    }

    // ===============================================================
    // 🔥 دالة التحقق من وجود Google Play Services
    // ===============================================================
    private boolean isGmsAvailable() {
        try {
            GoogleApiAvailability api = GoogleApiAvailability.getInstance();
            int result = api.isGooglePlayServicesAvailable(this);
            return result == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    private void Animation() {
        int dur = 1000;
        ObjectAnimator animationX = new ObjectAnimator(), animationY = new ObjectAnimator();
        animationX.setTarget(img);
        animationX.setDuration(dur);
        animationX.setPropertyName("scaleX");
        animationX.setFloatValues(1, 0);
        animationX.setInterpolator(new LinearInterpolator());
        animationY.setTarget(img);
        animationY.setDuration(dur);
        animationY.setPropertyName("scaleY");
        animationY.setFloatValues(1, 0);
        animationY.setInterpolator(new LinearInterpolator());
        animationY.start();
        animationX.start();
        animationX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                handler.post(() -> {
                    Intent i = new Intent(Splash.this, Main.class);
                    startActivity(i);
                    CustomIntent.customType(Splash.this, CustomIntent.LEFT_TO_RIGHT);
                    finish();
                });
            }
        });
    }

    private void checkDatabaseAndSubscribe() {
        String storedData = prefs.getString("list", "[]");
        boolean hasStoredData = !storedData.equals("[]");

        String statut = prefs.getString("statut", "");
        if ("disabled".equalsIgnoreCase(statut)) {
            String url = prefs.getString("url", "").trim();
            if (!url.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
            finishAffinity();
            System.exit(0);
        }

        // إلغاء اشتراك القنوات القديمة
        FirebaseMessaging.getInstance().unsubscribeFromTopic("xpola_external");
        FirebaseMessaging.getInstance().unsubscribeFromTopic("xpola_prop");

        if (hasStoredData) {
            FirebaseMessaging.getInstance().subscribeToTopic("xpola_prop");
        } else {
            FirebaseMessaging.getInstance().subscribeToTopic("xpola_external");
        }
    }
}
