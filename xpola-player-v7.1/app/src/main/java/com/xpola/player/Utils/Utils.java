package com.xpola.player.Utils;

import static android.provider.Settings.Secure.ANDROID_ID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xpola.player.Modules.FileInfo;
import com.xpola.player.R;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Ui.Activities.Main;

import org.jetbrains.annotations.Contract;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Utils {
    private final Context ctx;
    public static final Handler handler = new Handler(Looper.getMainLooper());
    public static final String XOR_ENCRYPTION_STARTS_WITH = "XPOLA808";

    // ------------------ إعدادات XPOLA909 ------------------
    // المفتاح المخزّن داخل الـ APK (مشفّر XPOLA808) — استبدل إن رغبت
    public static final String DEFAULT_XPOLA909_KEY_ENCRYPTED = "XPOLA808aEB/fHFAfHFpdWJQIiAiJTA=";
    public static final String XPOLA909_PREFIX = "XPOLA909";
    public static final String XPOLA808_PREFIX = "XPOLA808";
    // -----------------------------------------------------

    public Utils(Context ctx) {
        this.ctx = ctx;
    }

    public static void RadiusAndStroke(@NonNull View v, int rad, int stroke, int strokeColor, int bgColor) {
        v.setBackground(new GradientDrawable() {
            public GradientDrawable getStrokeAndRadius(int rad, int stroke, int strokeColor, int bgColor) {
                this.setCornerRadius(rad);
                this.setColor(bgColor);
                this.setStroke(stroke, strokeColor);
                return this;
            }
        }.getStrokeAndRadius(rad, stroke, strokeColor, bgColor));
    }

    public static void Radius(@NonNull View v, int rad, int bgColor) {
        v.setBackground(new GradientDrawable() {
            public GradientDrawable getRadius(int rad, int bgColor) {
                this.setCornerRadius(rad);
                this.setColor(bgColor);
                return this;
            }
        }.getRadius(rad, bgColor));
    }

    public static void Radii(@NonNull View v, final float l, final float t, final float r, final float b, int bgColor) {
        v.setBackground(new GradientDrawable() {
            public GradientDrawable getRadius(float[] rad, int bgColor) {
                this.setCornerRadii(new float[]{l, l, t, t, r, r, b, b});
                this.setColor(bgColor);
                return this;
            }
        }.getRadius(new float[]{l, t, r, b}, bgColor));
    }

    @ColorInt
    public static int getColor(Resources res, int colorId) {
        return ResourcesCompat.getColor(res, colorId, null);
    }


    public static void SetFocus(View v, final int color1, final int color2) {
        if (v == null) return;
        v.setOnFocusChangeListener((v1, isFocused) -> {
            if (isFocused) {
                if (v1.getTag() != null && v1.getTag().equals("fab"))
                    RadiusAndStroke(v1, 360, 3, Colors.YELLOW, color1);
                else RadiusAndStroke(v1, 20, 3, Colors.YELLOW, color1);
            } else {
                if (v1.getTag() != null && v1.getTag().equals("fab"))
                    RadiusAndStroke(v1, 360, 0, Colors.YELLOW, color2);
                else RadiusAndStroke(v1, 20, 0, Colors.YELLOW, color2);
            }
        });
        onTouch(v, color1, color2);
    }


    @SuppressLint("ClickableViewAccessibility")
    public static void onTouch(View v, final int color1, final int color2) {
        if (v == null) return;
        v.setOnTouchListener((v1, m) -> {
            if (MotionEvent.ACTION_DOWN == m.getAction()) {
                if (v1.getTag() != null && v1.getTag().equals("fab"))
                    RadiusAndStroke(v1, 360, 3, Colors.YELLOW, color1);
                else RadiusAndStroke(v1, 20, 3, Colors.YELLOW, color1);
            } else {
                switch (m.getAction()) {
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_HOVER_EXIT:
                    case MotionEvent.ACTION_UP:
                        if (v1.getTag() != null && v1.getTag().equals("fab"))
                            RadiusAndStroke(v1, 360, 0, Colors.YELLOW, color2);
                        else RadiusAndStroke(v1, 20, 0, Colors.YELLOW, color2);
                }
            }
            return false;
        });
    }

    public static String objectToString(Object o) {
        try {
            if (o == null) return "";
            if (o instanceof List || o instanceof Map) return new Gson().toJson(o);
            else return o.toString();
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String getValue(Map<String, Object> index, String key, String def) {
        return index != null && index.containsKey(key) && !objectToString(index.get(key)).isEmpty() ? objectToString(index.get(key)) :
                def;
    }

    public static String getValueString(Map<String, String> index, String key, String def) {
        return index != null && index.containsKey(key) && !objectToString(index.get(key)).isEmpty() ? objectToString(index.get(key)) :
                def;
    }


    public static String getValue(Bundle index, String key, String def) {
        try {
            return index != null && index.containsKey(key) && !objectToString(index.get(key)).isEmpty() ? objectToString(index.get(key)) :
                    def;
        } catch (Throwable ignored) {
        }
        return def;
    }

    public static boolean parseBoolean(String s, boolean def) {
        try {
            s = s.equals("1") ? "true" : s.equals("0") ? "false" : s;
            return Boolean.parseBoolean(s);
        } catch (Exception ignored) {
        }
        return def;
    }

    public void showToast(String msg) {
        handler.post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }


    public static void showToast(Context ctx, String msg) {

        handler.post(() -> {

            try {
                Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);

                View view = toast.getView();
                if (view == null) {
                    toast.show();
                    return;
                }

                // خلفية التوست
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(25);
                bg.setColor(Color.parseColor("#A0000000")); // أسود شفاف
                view.setBackground(bg);

                // تنسيق النص
                TextView text = view.findViewById(android.R.id.message);
                if (text != null) {
                    text.setTextColor(Color.WHITE);
                    text.setTextSize(17);
                    text.setGravity(Gravity.CENTER);
                }

                // عرض في وسط الشاشة
                toast.setGravity(Gravity.CENTER, 0, 120);

                toast.show();

            } catch (Exception ignored) {}
        });
    }


    @NonNull
    public static String removeAllSpaces(@NonNull String str) {
        return str.replaceAll("[\\s]+", "");
    }


    public void setIcon(String poster, ImageView into) {
        try {
            Glide.with(ctx)
                    .load(poster)
                    .placeholder(R.drawable.app_icon)
                    .error(R.drawable.app_icon)
                    .into(into);
        } catch (Exception ignored) {
            into.setImageResource(R.drawable.app_icon);
        }
    }


    @NonNull
    public static String isCanaryInstalled(Context context) {
        String[] packs = {"app.greyshirts.sslcapture",
                "com.emanuelef.remote_capture", "com.minhui.networkcapture",
                "com.minhui.networkcapture.pro", "com.reqable.android","com.reqable.android.helper",
                 "tech.httptoolkit.android.v1","com.black.canary","com.pcapdroid.mitm",
                "com.guoshi.httpcanary", "com.guoshi.httpcanary.premium", "com.pingidentity.burpsuite","np.filemanager.pro",
                "com.telerik.fiddler", "com.xk72.charles", "jp.co.taosoftware.android.packetcapture",
                "com.sniffer","com.llldur","bin.mt.plus"};
        for (String pack : packs)
            if (isInstalled(context, pack))
                return "y";
        return "n";
    }
    public static boolean isUrl(String s) {
        return s.startsWith("http");
    }

    @NonNull
    @Contract(pure = true)
    public static String defaultUserAgent(Activity context) {
        return new WebView(context).getSettings().getUserAgentString();
    }


    @NonNull
    public static List<?> removeNullObjects(@NonNull List<?> list) {
        List<Object> outList = new ArrayList<>();
        for (Object o : list) {
            if (o != null) {
                outList.add(o);
            }
        }
        list.clear();
        return outList;
    }

    public static String getExtension(String input) {
        try {
            String escape = "#\"'?/\\<>\\]\\[\\{\\}";
            Uri mainUrl = Uri.parse(input);
            input = mainUrl.getPath();
            if (input != null && input.lastIndexOf("/") != -1)
                input = input.substring(input.lastIndexOf("/"));
            Pattern pattern = Pattern.compile("/.+\\.(.+)[" + escape + "]?");
            if (input != null) {
                Matcher m = pattern.matcher(input);
                if (m.find()) return m.group(1);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public static List<HashMap<String, Object>> getListObject(Object o) {
        try {
            if (o == null) return new ArrayList<>();
            if (o instanceof List) return (List<HashMap<String, Object>>) o;
            if (o instanceof String && isArray(o.toString()))
                return new Gson().fromJson(o.toString(), new TypeToken<ArrayList<HashMap<String, Object>>>() {
                }.getType());
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getListString(Object o) {
        try {
            if (o == null) return new ArrayList<>();
            if (o instanceof List) return (List<Map<String, String>>) o;
            if (o instanceof String && isArray(o.toString()))
                return new Gson().fromJson(o.toString(), new TypeToken<ArrayList<HashMap<String, String>>>() {
                }.getType());
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> getObject(Object o) {
        try {
            if (o == null) return new HashMap<>();
            if (o instanceof Map) return (HashMap<String, Object>) o;
            if (o instanceof String && isObject(o.toString()))
                return new Gson().fromJson(o.toString(), new TypeToken<HashMap<String, Object>>() {
                }.getType());
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getMapString(Object o) {
        try {
            if (o == null) return new HashMap<>();
            if (o instanceof Map) return (HashMap<String, String>) o;
            if (o instanceof String && isObject(o.toString()))
                return new Gson().fromJson(o.toString(), new TypeToken<HashMap<String, String>>() {
                }.getType());
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }


    @SuppressWarnings("unchecked")
    public static List<String> getList(Object o) {
        try {
            if (o == null) return new ArrayList<>();
            if (o instanceof List) return (List<String>) o;
            if (o instanceof String && isArray(o.toString()))
                return new Gson().fromJson(o.toString(), new TypeToken<List<String>>() {
                }.getType());
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    public static List<HashMap<String, Object>> removeAds(List<HashMap<String, Object>> list) {
        try {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (!Utils.getValue(list.get(i), "type", "").isEmpty()) {
                    list.remove(i);
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    public static boolean isM3u(@NonNull String u) {
        u = u.toLowerCase();
        return u.contains("/get.php?") || u.contains("raw.githubusercontent")
                || (u.contains(".m3u") && !u.contains(".m3u8"));
    }

    public float getDip(int inp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, inp, ctx.getResources().getDisplayMetrics());
    }


    public static boolean isArray(@NonNull String str) {
        return removeAllSpaces(str).startsWith("[") && removeAllSpaces(str).endsWith("]");
    }

    public static boolean isObject(@NonNull String str) {
        return removeAllSpaces(str).startsWith("{") && removeAllSpaces(str).endsWith("}");
    }

    public static boolean isInstalled(Context ctx, String p) {
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo(p, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void sortListMap(List<HashMap<String, Object>> listMap, String key, boolean isNumber, boolean ascending) {
        Collections.sort(listMap, (compareMap1, compareMap2) -> {
            try {
                if (isNumber && compareMap1.containsKey(key) && compareMap2.containsKey(key)) {
                    int _count1 = Integer.parseInt(Objects.requireNonNull(compareMap1.get(key)).toString());
                    int _count2 = Integer.parseInt(Objects.requireNonNull(compareMap2.get(key)).toString());
                    if (ascending) {
                        return _count1 < _count2 ? -1 : 0;
                    } else {
                        return _count1 > _count2 ? -1 : 0;
                    }
                } else if (compareMap1.containsKey(key) && compareMap2.containsKey(key)) {
                    if (ascending) {
                        return (Objects.requireNonNull(compareMap1.get(key)).toString()).compareTo(Objects.requireNonNull(compareMap2.get(key)).toString());
                    } else {
                        return (Objects.requireNonNull(compareMap2.get(key)).toString()).compareTo(Objects.requireNonNull(compareMap1.get(key)).toString());
                    }
                }
            } catch (Exception ignored) {

            }
            return 0;
        });
    }

    public static int getRandom(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    @NonNull
    public static String ColorString() {
        String[] chars = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        StringBuilder builder = new StringBuilder("#");
        for (int x = 0; x < 6; x++) {
            builder.append(chars[Utils.getRandom(0, chars.length - 1)]);
        }
        return builder.toString();
    }

    public static String Match(String pat, String match, int gp) {
        Matcher matcher = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(match);
        if (matcher.find()) {
            return objectToString(matcher.group(gp));
        }
        return "";
    }


    public static String fromBase(String s) {
        try {
            return new String(Base64.decode(s, Base64.NO_WRAP));
        } catch (Throwable ignored) {
        }
        return s;
    }

// تم تغيير هذه الدالة ابتداءا من اصدار  6.7+
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }
    public static Class<?> getClass(String name, Class<?> def) {
        try {
            return Class.forName(name);
        } catch (Exception ignored) {
        }
        return def;
    }

    public static boolean isHex(@NonNull String s) {
        return s.matches("[0-9A-Fa-f]+");
    }

    @NonNull
    public static String toUrlBase64(byte[] s) {
        return Base64.encodeToString(s, Base64.NO_WRAP).replace("+", "-")
                .replace("/", "_").replace("=", "");
    }

    @NonNull
    public static byte[] hexToBytes(@NonNull String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static void trustAllCerts() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ignored) {
        }
    }

    public static RenderersFactory buildRenderersFactory(@NonNull Context context, boolean preferExtensionRenderer) {
        @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = preferExtensionRenderer
                ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
        return new DefaultRenderersFactory(context.getApplicationContext()).setExtensionRendererMode(extensionRendererMode).forceEnableMediaCodecAsynchronousQueueing();
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId(@NonNull Context context) {
        return Settings.Secure.getString(context.getContentResolver(), ANDROID_ID);
    }

    public static boolean haveScheme(@NonNull String s) {
        return s.equalsIgnoreCase("playready")
                || s.equalsIgnoreCase("widevine")
                || s.equalsIgnoreCase("clearkey");
    }

    public static boolean checkUsageAgreements(Activity activity, @NonNull Prefs prefs) {
        if (!prefs.getBoolean("done", false)) {
            showToast(activity, "Please agree to the terms and conditions for using Xpola Player");
            Intent intent = new Intent(activity, Main.class);
            Bundle extras = activity.getIntent().getExtras();
            if (intent.getData() != null)
                intent.setData(intent.getData());
            if (extras != null) {
                extras.putString("class", activity.getClass().getName());
                intent.putExtras(extras);
            }
            activity.startActivity(intent);
            activity.finish();
            return true;
        }
        return false;
    }

    public static boolean isXor(@NonNull String s) {
        return s.toUpperCase().startsWith(XOR_ENCRYPTION_STARTS_WITH);
    }

    /******************* هنا الإضافات المتعلقة بـ XPOLA909 و حل المفتاح المشفر *******************/

    /** هل السلسلة مشفرة XPOLA909؟ */
    public static boolean isXpola909(@NonNull String s) {
        return s.toUpperCase().startsWith(XPOLA909_PREFIX);
    }

    /** هل السلسلة XPOLA808؟ */
    public static boolean isXpola808(@NonNull String s) {
        return s.toUpperCase().startsWith(XPOLA808_PREFIX);
    }

    /**
     * Base64 URL-safe بدون padding
     */
    @NonNull
    public static String urlBase64Encode(@NonNull byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP).replace("+", "-").replace("/", "_").replace("=", "");
    }

    /**
     * يفك ترميز Base64 URL-safe (يعيد padding إذا لزم)
     */
    @NonNull
    public static byte[] urlBase64Decode(@NonNull String s) {
        String tmp = s.replace("-", "+").replace("_", "/");
        int mod = tmp.length() % 4;
        if (mod != 0) {
            int pad = 4 - mod;
            StringBuilder sb = new StringBuilder(tmp);
            for (int i = 0; i < pad; i++) sb.append('=');
            tmp = sb.toString();
        }
        return Base64.decode(tmp, Base64.NO_WRAP);
    }

    /**
     * فك المفتاح المخزّن بصيغة XPOLA808 الى المفتاح الحقيقي (مثال: xPolaPlayer@2025)
     * (لا تعتمد على الدالة القديمة decrypt هنا لتفادي أي تداخل)
     */
    @NonNull
    public static String decryptXpola808(@NonNull String enc) {
        try {
            String work = enc.startsWith(XPOLA808_PREFIX) ? enc.substring(XPOLA808_PREFIX.length()) : enc;
            byte[] decoded = Base64.decode(work, Base64.NO_WRAP);
            char[] chars = new String(decoded, StandardCharsets.UTF_8).toCharArray();
            for (int i = 0; i < chars.length; i++) {
                chars[i] = (char) (chars[i] ^ 16);
            }
            // 👇 مهم: قصّ المسافات الزائدة
            return String.valueOf(chars).trim();
        } catch (Exception e) {
            return enc;
        }
    }

    /**
     * حل المفتاح: إن كان keyOrEncrypted يبدأ بـ XPOLA808 نقوم بفكه، وإلا نعيده كما هو.
     * استخدام هذه الدالة يضمن أن المفتاح الموجود داخل الكلاس يمكن أن يكون مشفراً دائماً.
     */
    @NonNull
    public static String resolveKey(@NonNull String keyOrEncrypted) {
        try {
            if (keyOrEncrypted == null || keyOrEncrypted.isEmpty()) return "";
            if (isXpola808(keyOrEncrypted)) {
                return decryptXpola808(keyOrEncrypted);
            }
        } catch (Throwable ignored) {
        }
        return keyOrEncrypted;
    }

    /**
     * فك XPOLA909 باستخدام مفتاح (يمكن تمرير المفتاح كـ XPOLA808 مشفّر أو كمفتاح صريح)
     */
    @NonNull
    public static String decryptXpola909(@NonNull String enc, @NonNull String keyOrEncryptedKey, Context context) throws SecurityException {
        try {
            if (enc == null || enc.isEmpty()) return "";
            if (!isXpola909(enc)) return enc;

            // Check the app's signature before decrypting
            if (!Sec.SignatureCheck.isSignatureValid(context)) {
                throw new SecurityException("App signature is not valid.");
            }

            // أولاً نحصل على المفتاح الحقيقي (نفك XPOLA808 إن لزم)
            String key = resolveKey(keyOrEncryptedKey);
            if (key == null || key.isEmpty()) key = "xpola_default_key";

            String body = enc.substring(XPOLA909_PREFIX.length());
            byte[] bytes = urlBase64Decode(body);

            // عكس المصفوفة
            for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
                byte t = bytes[i];
                bytes[i] = bytes[j];
                bytes[j] = t;
            }

            // XOR عكسي (نفس العملية لأن XOR انعكاسي)
            for (int i = 0; i < bytes.length; i++) {
                int k = key.charAt(i % key.length());
                bytes[i] = (byte) (bytes[i] ^ (k + (i & 0xFF)));
            }

            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return enc;
        }
    }

    /**
     * نسخة مساعدة لدالة decrypt تقبل مفتاحًا محددًا (يمكن أن يكون مشفّراً XPOLA808 أو صريحًا).
     * هذه النسخة تُستخدم إذا أردت فك رسالة XPOLA909 بمفتاح محدد خارج الثابت.
     */
    @NonNull
    public static String decrypt(@NonNull String str, @NonNull String keyOrEncryptedKey, Context context) {
        if (str == null || str.isEmpty()) return "";

        // جرب XPOLA909 أولاً
        try {
            if (isXpola909(str)) {
                return decryptXpola909(str, keyOrEncryptedKey, context);
            }
        } catch (SecurityException e) {
            throw e; // Re-throw the security exception to be handled by the caller
        } catch (Throwable ignored) {
        }

        // وإلا اتبع سلوك XPOLA808 القديم
        boolean isXor = isXor(str);
        String work = str;
        if (isXor) work = work.substring(XOR_ENCRYPTION_STARTS_WITH.length());
        work = fromBase(work);
        if (isXor) {
            char[] charArray = work.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                charArray[i] = (char) (charArray[i] ^ 16);
            }
            work = String.valueOf(charArray);
        }
        return work;
    }

    /**
     * الدالة العامة decrypt(String) — تحافظ على التوافق:
     * - لو النص يبدأ بـ XPOLA909 => فك باستخدام المفتاح المخزّن داخل الـ APK (المشفّر XPOLA808).
     * - وإلا: نفس سلوك XPOLA808 القديم.
     */
    @NonNull
    public static String decrypt(@NonNull String str, Context context) {
        if (str == null || str.isEmpty()) return "";

        // XPOLA101 Decryption
        if (str.startsWith("XPOLA101.")) {
            return Xpola101Crypto.decryptAuto(context, str);
        }

        // إذا كانت XPOLA909 -> نفكها باستخدام المفتاح المخزن داخل الـ APK
        try {
            if (isXpola909(str)) {
                return decryptXpola909(str, DEFAULT_XPOLA909_KEY_ENCRYPTED, context);
            }
        } catch (SecurityException e) {
            throw e; // Re-throw the security exception to be handled by the caller
        } catch (Throwable ignored) {
        }

        // وإلا نبقي سلوك XPOLA808 القديم كما هو تماماً
        boolean isXor = isXor(str);
        String work = str;
        if (isXor) work = work.substring(XOR_ENCRYPTION_STARTS_WITH.length());
        work = fromBase(work);
        if (isXor) {
            char[] charArray = work.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                charArray[i] = (char) (charArray[i] ^ 16);
            }
            work = String.valueOf(charArray);
        }
        return work;
    }

    /******************* نهاية إضافات XPOLA909 *******************/

    @NonNull
    public static String fromBase64_USASCII(String s) {
        return new String(Base64.decode(s, Base64.DEFAULT), StandardCharsets.US_ASCII);
    }


    @NonNull
    public static String YCDecoder(String s, String key) {
        s = fromBase64_USASCII(s);
        StringBuilder builder = new StringBuilder();
        for (int chr = 0; chr < s.length(); chr++) {
            builder.append((char) (((int) s.charAt(chr)) ^ ((int) key.charAt(chr % key.length()))));
        }
        return builder.toString();
    }


    public static String toBase64(String s) {
        return Base64.encodeToString(s.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static String fromBase64(String s) {
        return new String(Base64.decode(s, Base64.NO_WRAP | Base64.NO_PADDING));
    }

    public static String UnicodeDecoder(String input) {
        Pattern unicodePattern = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
        Matcher matcher = unicodePattern.matcher(input);
        while (matcher.find()) {
            String unicode = objectToString(matcher.group(1));
            char unicodeChar = (char) parseInt(unicode, 16);
            input = input.replace(unicode, String.valueOf(unicodeChar));
        }
        return input;
    }

    @NonNull
    @Contract("_ -> new")
    public static FileInfo getFileInfo(@NonNull Activity activity) {
        Uri fileUri = activity.getIntent().getData();
        String fileName = "";
        String fileExtension = "";
        if (fileUri != null) {
            Cursor cursor = activity.getContentResolver().query(fileUri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = Utils.objectToString(cursor.getString(nameIndex));
                if (fileName.contains(".")) {
                    fileExtension = Utils.objectToString(fileName.substring(fileName.lastIndexOf(".") + 1));
                }
                cursor.close();
            }
        }
        return new FileInfo(fileName.replace("." + fileExtension, ""), fileExtension);
    }

    private static Object parseInt(String s, int def) {
        try {
            Integer.parseInt(s, def);
        } catch (Exception ignored) {
        }
        return def;
    }

    public static String getDataFromUrl(String url) throws Exception {
        java.net.URL urlObj = new java.net.URL(url);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();
        return content.toString();
    }
}
