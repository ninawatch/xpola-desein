package com.xpola.player.Sec;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.xpola.player.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class Sec {

    // الخاصية 1: تحقق من MD5 لاسم الحزمة
    public static void close(@NonNull Activity ctx) {
        if (!ctx.getPackageName().equals("com.xpola.player") ||
                !(ctx.getString(R.string.app_name).equalsIgnoreCase("xPola Player") || ctx.getString(R.string.app_name).equalsIgnoreCase("مشغل x بولا بلاير"))) {
            throw new RuntimeException();
        }

        String packageName = ctx.getPackageName() + ".flamedia";

        String md5Hash = md5(packageName);

        if (!md5Hash.equals("850b7ba5e0402279044c32b5ed33e8a2")) {
            ctx.finishAffinity();
            System.exit(0);
        }
    }


    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static class SignatureCheck {

        // التوقيع الأصلي للتطبيق مشفر باستخدام SHA-256
        // for playStore
        // private static final String ORIGINAL_SIGNATURE_SHA256 = "E546216FE6E4CA5C18FDD95D5E37B2273D38BB865C1E8682EEC7E6C64D47B618";
        // for tv
           private static final String ORIGINAL_SIGNATURE_SHA256 = "0AEE6366E3E415F9A48299ADECE8D0D210111D2961A7188EE8B9846D27CEF386";

        public static boolean isSignatureValid(PackageManager pm, String packageName) {
            try {
                PackageInfo packageInfo;

                // استخدم GET_SIGNING_CERTIFICATES إذا كان الـ API level 28 أو أعلى
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                } else {
                    // استخدم GET_SIGNATURES إذا كان الـ API level أقل من 28
                    packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                }

                // استخراج التوقيعات
                Signature[] signatures = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ? packageInfo.signingInfo.getApkContentsSigners()
                        : packageInfo.signatures;

                for (Signature signature : signatures) {
                    // تحويل التوقيع إلى SHA-256 للتحقق
                    String currentSignature = getSignatureString(signature);
                    // مقارنة التوقيع الحالي مع التوقيع الأصلي
                    if (ORIGINAL_SIGNATURE_SHA256.equals(currentSignature)) {
                        return true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                // e.printStackTrace();
            }
            return false;
        }

        public static boolean isSignatureValid(Context context) {
           try {
                PackageManager pm = context.getPackageManager();
                String packageName = context.getPackageName();
                PackageInfo packageInfo;

                // استخدم GET_SIGNING_CERTIFICATES إذا كان الـ API level 28 أو أعلى
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                } else {
                    // استخدم GET_SIGNATURES إذا كان الـ API level أقل من 28
                    packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                }

                // استخراج التوقيعات
                Signature[] signatures = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ? packageInfo.signingInfo.getApkContentsSigners()
                        : packageInfo.signatures;

                for (Signature signature : signatures) {
                    // تحويل التوقيع إلى SHA-256 للتحقق
                    String currentSignature = getSignatureString(signature);
                    // مقارنة التوقيع الحالي مع التوقيع الأصلي
                    if (ORIGINAL_SIGNATURE_SHA256.equals(currentSignature)) {
                        return true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                // e.printStackTrace();
            }
            return false;
        }

        // تحويل التوقيع إلى SHA-256
        private static String getSignatureString(Signature sig) {
            byte[] signatureBytes = sig.toByteArray();
            String sha256 = "";
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(signatureBytes);
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02X", b));
                }
                sha256 = sb.toString();
            } catch (NoSuchAlgorithmException e) {
                // e.printStackTrace();
            }
            return sha256;
        }
    }

    public static boolean isVpnActive(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                if (caps != null) {
                    return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                }
            }
        }
        return false;
    }

    public static boolean isProxyEnabled(Context context) {
        try {
            String proxyAddress = System.getProperty("http.proxyHost");
            String portStr = System.getProperty("http.proxyPort");
            int proxyPort = Integer.parseInt(portStr != null ? portStr : "-1");

            if (!TextUtils.isEmpty(proxyAddress) && proxyPort != -1) {
                return true;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                String proxyGlobal = Settings.Global.getString(context.getContentResolver(), Settings.Global.HTTP_PROXY);
                if (!TextUtils.isEmpty(proxyGlobal)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    public static boolean isEmulator() {
        Context context = getAppContext();  // للحصول على Context بدون تعديل باقي الملفات

        if (context == null) return false;

        // 1.  كشف وجود تطبيقات المحاكيات المثبتة
        String[] emulatorApps = {
                "com.android.settings.virtualdevice",
                "com.android.emulator",
                "com.ldplayer.store",
                "com.ldplayer.solo",
                "com.microvirt.guide",
                "com.microvirt.market",
                "com.bluestacks.home",
                "com.bluestacks.settings",
                "com.bignox.app",
                "com.bignox.appstore"
        };

        PackageManager pm = context.getPackageManager();
        for (String app : emulatorApps) {
            try {
                pm.getPackageInfo(app, 0);
                return true;  // إذا وجد أي تطبيق → Emulator
            } catch (Exception ignored) {}
        }

        // 2. كشف ملفات النظام الخاصة بالمحاكيات
        String[] emulatorFiles = {
                "/system/lib/libldplayer.so",
                "/system/lib64/libldplayer.so",
                "/system/lib/libnox.so",
                "/system/lib64/libnox.so",
                "/dev/qemu_pipe",
                "/dev/qemu_trace",
                "/system/bin/qemu-props"
        };

        for (String file : emulatorFiles) {
            if (new File(file).exists()) return true;
        }

        // 3. كشف المعمارية (DLPlayer و LDPlayer مستخدمين x86_64 دائماً)
        String arch = System.getProperty("os.arch");
        if (arch.contains("x86") || arch.contains("x86_64")) return true;

        // 4. عدد الحساسات (المحاكي دائماً <= 5)
        try {
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            int sensorCount = sm.getSensorList(Sensor.TYPE_ALL).size();
            if (sensorCount <= 5) return true;
        } catch (Exception ignored) {}

        // 5. شبكة المحاكي (السيرفر)
        if (Build.HOST.toLowerCase().contains("ubuntu")) return true;

        return false;
    }


    public static boolean isDebuggerAttached() {
        return Debug.isDebuggerConnected();
    }

    public static boolean isRooted() {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }


    // دالة لحذف الكاش
    public static void clearCache(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDir(cacheDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // دالة لمساعدة لحذف المجلدات
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
    private static Context getAppContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object at = activityThread.getMethod("currentApplication").invoke(null);
            if (at != null) return (Context) at;

            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            return (Context) activityThread.getMethod("getApplication").invoke(thread);

        } catch (Exception e) {
            return null;
        }
    }

}
