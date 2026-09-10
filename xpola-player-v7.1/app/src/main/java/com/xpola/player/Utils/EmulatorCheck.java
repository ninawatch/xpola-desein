package com.xpola.player.Utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class EmulatorCheck {

    private static final String TAG = "EmulatorCheck";

    public static boolean isEmulator(Context context) {
        return detectEmulator(context);
    }

    private static boolean detectEmulator(Context context) {

        PackageManager pm = context.getPackageManager();

        // =========================================================
        // 0) HARD WHITELIST — REAL ANDROID TV (CRITICAL)
        // =========================================================
        try {
            boolean isTv =
                    pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                            pm.hasSystemFeature("android.software.leanback") ||
                            pm.hasSystemFeature("android.hardware.type.television");

            if (isTv) {
                Log.i(TAG, "Real Android TV detected → NOT emulator");
                return false;
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 1) استثناء Android BOX + HiSilicon
        // =========================================================
        try {
            String hw = Build.HARDWARE.toLowerCase();
            String manu = Build.MANUFACTURER.toLowerCase();
            String model = Build.MODEL.toLowerCase();
            String brand = Build.BRAND.toLowerCase();
            String board = Build.BOARD.toLowerCase();
            String fingerprint = Build.FINGERPRINT.toLowerCase();

            String[] androidBoxKeys = {
                    "amlogic", "rockchip", "allwinner", "mediatek",
                    "s905", "s922", "s912", "s802", "s805",
                    "rk3229", "rk3318", "rk3328", "rk3399",
                    "t95", "x96", "h96", "a95", "tx3", "tx6", "m8s",
                    "ugoos", "mxq", "minix", "beelink", "tanix",
                    "bigfish", "hisilicon", "hi375"
            };

            for (String key : androidBoxKeys) {
                if (hw.contains(key) || manu.contains(key)
                        || brand.contains(key) || model.contains(key)
                        || board.contains(key) || fingerprint.contains(key)) {

                    Log.i(TAG, "Android BOX / TV hardware detected → NOT emulator");
                    return false;
                }
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 2) استثناء Huawei / Honor
        // =========================================================
        try {
            String man = Build.MANUFACTURER.toLowerCase();
            if (man.contains("huawei") || man.contains("honor")) {
                Log.i(TAG, "Huawei/Honor detected → NOT emulator");
                return false;
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 3) فحص ملفات المحاكيات
        // =========================================================
        String[] emulatorFiles = {
                "/dev/qemu_pipe",
                "/dev/qemu_trace",
                "/system/bin/qemu-props",
                "/system/lib/libbluestacks.so",
                "/system/lib64/libbluestacks.so",
                "/system/lib/libldplayer.so",
                "/system/lib64/libldplayer.so",
                "/system/lib/libnox.so",
                "/system/lib64/libnox.so"
        };

        for (String f : emulatorFiles) {
            if (new File(f).exists()) {
                Log.i(TAG, "Found emulator file: " + f);
                return true;
            }
        }

        // =========================================================
        // 4) libhoudini (ONLY NON-TV)
        // =========================================================
        try {
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                File h1 = new File("/system/lib/libhoudini.so");
                File h2 = new File("/system/lib64/libhoudini.so");

                if (h1.exists() || h2.exists()) {
                    Log.i(TAG, "libhoudini on non-TV → Emulator");
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 5) Packages of known emulators
        // =========================================================
        String[] emulatorPackages = {
                "com.ldplayer.solo",
                "com.ldplayer.store",
                "com.microvirt.market",
                "com.bignox.app",
                "com.bluestacks.home",
                "com.bluestacks.appmart",
                "com.nemu.oaidmanager",
                "com.netease.mumu.cloner",
                "com.netease.nemu_vapi_android.nemu",
                "com.mumu.store",
                "com.nemu.nlp"
        };

        for (String pkg : emulatorPackages) {
            try {
                pm.getPackageInfo(pkg, 0);
                Log.i(TAG, "Detected emulator package: " + pkg);
                return true;
            } catch (Exception ignored) {}
        }

        // =========================================================
        // 6) CPU spoofing (Intel pretending ARM)
        // =========================================================
        try {
            String cpuinfo = readFile("/proc/cpuinfo").toLowerCase();
            boolean isIntel = cpuinfo.contains("intel") || cpuinfo.contains("amd");

            boolean claimsSamsungARM =
                    Build.MANUFACTURER.toLowerCase().contains("samsung") ||
                            Build.MODEL.toLowerCase().startsWith("sm-") ||
                            Build.HARDWARE.toLowerCase().contains("samsung");

            if (claimsSamsungARM && isIntel) {
                Log.i(TAG, "Fake Samsung ARM on Intel CPU → Emulator");
                return true;
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 7) NativeBridge ARM → x86
        // =========================================================
        try {
            String armIsa = System.getProperty("ro.dalvik.vm.isa.arm", "");
            if (armIsa.contains("x86")) {
                Log.i(TAG, "NativeBridge ARM→x86 → Emulator");
                return true;
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 8) Sensors check (ONLY NON-TV)
        // =========================================================
        try {
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);

                if (sensors != null && sensors.size() < 5) {
                    Log.i(TAG, "Few sensors on non-TV → Emulator");
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // =========================================================
        // 9) cpuinfo QEMU
        // =========================================================
        try {
            String cpuInfo = readFile("/proc/cpuinfo").toLowerCase();
            if (cpuInfo.contains("goldfish")
                    || cpuInfo.contains("ranchu")
                    || cpuInfo.contains("qemu")) {

                Log.i(TAG, "QEMU CPU detected → Emulator");
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private static String readFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
