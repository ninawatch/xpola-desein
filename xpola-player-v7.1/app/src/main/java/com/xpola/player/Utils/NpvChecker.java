package com.xpola.player.Utils;

import androidx.annotation.NonNull;
import java.net.NetworkInterface;
import java.util.Collections;

public class NpvChecker implements Runnable {
    public static String getK8() { return "mN"; }
    private Result result;
    private boolean stopChecking = false;

    public NpvChecker(@NonNull Prefs prefs, Result result) {
        this.result = result;
        stopChecking = prefs.getBoolean("stop_check_npv", false);
    }

    @Override
    public void run() {
        if (!stopChecking) {
            Check();
            Utils.handler.postDelayed(this, 1000); // ← تعديل التأخير من 1 إلى 1000ms (1 ثانية) لتقليل الضغط
        }
    }

    public void Check() {
        if (result == null) return; // ← 🔒 حماية أساسية ضد NullPointerException

        if (isRun()) {
            Utils.handler.post(() -> {
                if (result != null) result.result(true);
            });
        } else {
            Utils.handler.post(() -> {
                if (result != null) result.result(false);
            });
        }
    }

    public static boolean isRun() {
        StringBuilder n = new StringBuilder();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isUp()) n.append(ni.getName().toLowerCase());
            }
            return n.toString().contains("tun0") || n.toString().contains("ppp") || n.toString().contains("pptp");
        } catch (Exception ignored) {
        }
        return false;
    }

    public interface Result {
        void result(boolean stop);
    }

    public void cancel() {
        stopChecking = true;
        result = null;
    }
}
