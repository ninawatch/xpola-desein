package com.xpola.player.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

public class ResumeHelper {

    private static final String PREF_NAME = "resume_data";
    private static final long SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void savePosition(Context context, String url, long position) {
        if (url == null || url.isEmpty() || position <= 0) return;

        // Format: "position|timestamp"
        String value = position + "|" + System.currentTimeMillis();
        getPrefs(context).edit().putString(url, value).apply();
    }

    public static long getPosition(Context context, String url) {
        if (url == null || url.isEmpty()) return 0;

        String value = getPrefs(context).getString(url, null);
        if (value == null) return 0;

        try {
            String[] parts = value.split("\\|");
            long position = Long.parseLong(parts[0]);
            long timestamp = Long.parseLong(parts[1]);

            if (System.currentTimeMillis() - timestamp > SEVEN_DAYS_MS) {
                clearPosition(context, url);
                return 0;
            }
            return position;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void clearPosition(Context context, String url) {
        if (url != null) {
            getPrefs(context).edit().remove(url).apply();
        }
    }

    public static void clearAllData(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public static void cleanupExpiredData(Context context) {
        SharedPreferences prefs = getPrefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();
        boolean changed = false;

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                Object val = entry.getValue();
                if (val instanceof String) {
                    String[] parts = ((String) val).split("\\|");
                    long timestamp = Long.parseLong(parts[1]);
                    if (System.currentTimeMillis() - timestamp > SEVEN_DAYS_MS) {
                        editor.remove(entry.getKey());
                        changed = true;
                    }
                }
            } catch (Exception e) {
                // Remove corrupted entry
                editor.remove(entry.getKey());
                changed = true;
            }
        }

        if (changed) {
            editor.apply();
        }
    }
}
