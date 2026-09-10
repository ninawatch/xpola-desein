package com.xpola.player.Ui.Activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.xpola.player.R;
import com.xpola.player.Utils.Constants;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.ResumeHelper;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        // تهيئة prefs
        Prefs prefs = new Prefs(this);

        // تحديد زر الإغلاق وإضافة حدث النقر عليه
        Button closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish()); // يغلق النشاط عند النقر

        // تحديد زر التحديث وإضافة حدث النقر عليه
        Button updateButton = findViewById(R.id.update_button);
        updateButton.setOnClickListener(v -> {
            // فتح صفحة التطبيق على متجر Google Play لتحديث التطبيق
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.xpola.player"));
            startActivity(intent);
        });

        // تحديد زر إعادة التشغيل وإضافة حدث النقر عليه
        Button restartButton = findViewById(R.id.restart_button);
        restartButton.setOnClickListener(v -> restartAppHard());

        // تحديد زر مسح بيانات الاستئناف وإضافة حدث النقر عليه
        Button clearResumeButton = findViewById(R.id.clear_resume_button);
        clearResumeButton.setOnClickListener(v -> {
            ResumeHelper.clearAllData(this);
            Toast.makeText(this, "Resume data cleared", Toast.LENGTH_SHORT).show();
        });

        // تحديد TextView لعرض رقم النسخة
        TextView appVersionTextView = findViewById(R.id.app_version);

        // تعيين نص جديد لعرض رقم النسخة
        String versionText = "App Version: " + Constants.version + prefs.getString("sub_type", "");
        appVersionTextView.setText(versionText);

        // تحديد TextView لعرض حالة النسخة
        TextView newVersionTextView = findViewById(R.id.new_version);

        // الحصول على آخر نسخة متاحة من prefs
        String latestVersion = prefs.getString("latest_version", "");

        // مقارنة النسخ وتحديث النص
        if (!latestVersion.isEmpty()) {
            if (Constants.version.equals(latestVersion)) {
                newVersionTextView.setText("No new version available");
                newVersionTextView.setTextColor(Color.WHITE); // اضبط لون النص للأبيض إذا كانت النسخة هي الأحدث
                updateButton.setVisibility(Button.GONE); // إخفاء زر التحديث إذا كانت النسخة هي الأحدث
            } else {
                newVersionTextView.setText("New version available: " + latestVersion);
                newVersionTextView.setTextColor(Color.RED); // اضبط لون النص للأحمر إذا كان هناك إصدار جديد
                updateButton.setVisibility(Button.VISIBLE); // إظهار زر التحديث عند توفر نسخة جديدة
            }
        } else {
            newVersionTextView.setText("Unable to retrieve latest version info");
            newVersionTextView.setTextColor(Color.GRAY); // استخدم لون رمادي للإشارة إلى عدم القدرة على استرجاع المعلومات
            updateButton.setVisibility(Button.GONE); // إخفاء زر التحديث إذا لم يكن بالإمكان استرجاع المعلومات
        }
    }

    /**
     * دالة لإعادة تشغيل التطبيق
     */
    public void restartAppHard() {

        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName());

        if (intent == null) return;

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 300,
                    pendingIntent
            );
        }

        // إنهاء كل الأنشطة
        finishAffinity();

        // قتل التطبيق نهائيًا
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}