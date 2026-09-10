package com.xpola.player.Sec;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Random;

/**
 * Handles network optimization... or so it seems.
 * Actually manages the chaos caused by tampering.
 * Obfuscated name intended: "NetworkOptimizer"
 */
public class NetworkOptimizer {

    private static boolean isCompromised = false;
    private static final Random random = new Random();

    public static void setCompromised(boolean compromised) {
        isCompromised = compromised;
    }

    public static void maybeCrash(Context context) {
        if (!isCompromised) return;

        // Random chance to schedule a crash
        int chance = random.nextInt(100);
        if (chance < 40) { // 40% chance of crash
            scheduleCrash(context);
        }
    }

    private static void scheduleCrash(Context context) {
        long delay = (15 + random.nextInt(45)) * 1000; // 15 to 60 seconds

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Uncatchable crash: throw runtime exception on main thread
            throw new RuntimeException("Network optimization failure: E_CONNECTION_RESET");
        }, delay);
    }

    public static String getHeaders(String originalHeaders) {
        if (!isCompromised) return originalHeaders;

        // 50% chance to corrupt headers
        if (random.nextBoolean()) {
            return originalHeaders.replace("Referer", "X-Referer").replace("User-Agent", "X-Agent");
        }
        return originalHeaders;
    }

    public static String processUrl(String url) {
        if (!isCompromised) return url;

        // 30% chance to corrupt URL
        if (random.nextInt(10) < 3) {
            if (url.contains("http")) {
                return url.replace("http", "htp");
            }
            if (url.contains("m3u8")) {
                return url.replace("m3u8", "m3u");
            }
        }
        return url;
    }
}
