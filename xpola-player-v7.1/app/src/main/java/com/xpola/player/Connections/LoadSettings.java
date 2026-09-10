package com.xpola.player.Connections;

import android.app.Activity;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.xpola.player.Interfaces.INet;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Sec.Sec.SignatureCheck;
import com.xpola.player.Utils.Constants;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoadSettings {
    public static boolean appValue = true;

    public static void getSettings(Activity ctx) {
        FirebaseApp.initializeApp(ctx);
        Prefs prefs = new Prefs(ctx);
        FirebaseDatabase.getInstance("https://55xpola-d30ff-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("player")
                .child("settings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try {
                                HashMap<String, Object> index = Utils.getObject(snapshot.getValue());
                                if (index != null) {
                                    prefs.setString("appId", checkKey(index, "appid"));
                                    prefs.setString("adBanner", checkKey(index, "adBanner"));
                                    prefs.setString("adInter", checkKey(index, "adInter"));
                                    prefs.setString("adNative", checkKey(index, "adNative"));
                                    prefs.setString("appInter", checkKey(index, "appInter"));
                                    prefs.setString("appBanner", checkKey(index, "appBanner"));
                                    prefs.setString("fb_banner", checkKey(index, "fb_banner"));
                                    prefs.setString("fb_inter", checkKey(index, "fb_inter"));
                                    prefs.setString("fb_native", checkKey(index, "fb_native"));

                                    prefs.setString("version", checkKey(index, "version"));
                                    prefs.setString("message", checkKey(index, "message"));
                                    prefs.setString("url", checkKey(index, "url"));
                                    prefs.setString("fb", checkKey(index, "fb"));
                                    prefs.setString("email", checkKey(index, "email"));
                                    prefs.setString("website", checkKey(index, "website"));
                                    prefs.setString("telegram", checkKey(index, "telegram"));
                                    prefs.setString("privacy", checkKey(index, "privacy"));
                                    prefs.setString("default_user_agent", checkKey(index, "default_user_agent"));
                                    prefs.setString("policy_1", checkKey(index, "policy_1"));
                                    prefs.setString("policy_2", checkKey(index, "policy_2"));
                                    prefs.setString("replace", checkKey(index, "replace"));
                                    prefs.setString("replace_2", checkKey(index, "replace_2"));
                                    prefs.setString("with", checkKey(index, "with"));
                                    prefs.setString("with_2", checkKey(index, "with_2"));
                                    prefs.setString("httpcanary", checkKey(index, "httpcanary"));
                                    prefs.setString("contacturl", checkKey(index, "contacturl"));
                                    prefs.setString("latest_version", checkKey(index, "latest_version"));
                                    prefs.setString("z_package_apps", checkKey(index, "z_package_apps"));
                                    prefs.setInt("ads_counter", index.containsKey("ads_counter") ? Integer.parseInt(Objects.requireNonNull(index.get("ads_counter")).toString()) : 3);
                                    prefs.setInt("max", Integer.parseInt(checkKey(index, "max_native_load")));
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    // دالة فك التشفير
    private static String decrypt(String encrypted) {
        if (encrypted == null || !encrypted.startsWith("XPOLA808")) {
            return "";
        }

        String base64Part = encrypted.substring(8);
        byte[] decodedBytes = Base64.decode(base64Part, Base64.DEFAULT);
        String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
        StringBuilder decrypted = new StringBuilder();

        for (char c : decoded.toCharArray()) {
            decrypted.append((char) (c ^ 16));
        }

        return decrypted.toString();
    }

    // تعديل دالة getSettingsJSONFile لاستخدام فك التشفير
    public static void getSettingsJSONFile(Activity ctx) {
        boolean isSignatureValid = SignatureCheck.isSignatureValid(ctx.getPackageManager(), ctx.getPackageName());
        if (!isSignatureValid) {
            appValue = false;
        }
        Prefs prefs = new Prefs(ctx);
        NetRequests requests = new NetRequests();
        Map<String, String> headers = new HashMap<>();
        String rootStatus = Sec.isRooted() ? "R1" : "R0";

        // 🟩 هنا نضيف إصدار الأندرويد
        String androidVersion = android.os.Build.VERSION.RELEASE; // مثال: "13"

        headers.put("clearKey",
                "xpola_player_808("
                        + Utils.getDeviceId(ctx) + ")"
                        + Utils.isCanaryInstalled(ctx) + "("
                        + Constants.version + ")" + "("
                        + appValue + ")" + "("
                        + rootStatus + ")" + "("
                        + androidVersion + ")"  // ← تمت الإضافة هنا
        );

        String primaryUrl = "https://arabradiofm.com/images/logo.jpeg";
        String backupUrl = "https://xpola.yoo7.com/h22-page/index.html";

        fetchData(requests, primaryUrl, backupUrl, headers, prefs);
    }

    private static void fetchData(NetRequests requests, String url, String backupUrl, Map<String, String> headers, Prefs prefs) {
        requests.get(url, headers, new INet() {
            @Override
            public void onSuccess(String data, String requestUrl) {
                parseAndSaveData(data, prefs);
            }

            @Override
            public void onFailed(String error, String requestUrl) {
                if (requestUrl.equals(url)) {
                    // حاول مرة أخرى باستخدام الرابط الاحتياطي
                    requests.get(backupUrl, headers, new INet() {
                        @Override
                        public void onSuccess(String data, String requestUrl) {
                            parseAndSaveData(data, prefs);
                        }

                        @Override
                        public void onFailed(String error, String requestUrl) {
                            // فشل تحميل البيانات من الرابطين
                        }
                    });
                }
            }
        });
    }

    private static void parseAndSaveData(String data, Prefs prefs) {
        String decryptedData = decrypt(data);
        HashMap<String, Object> index = new HashMap<>();
        if (Utils.isObject(decryptedData)) {
            index = Utils.getObject(decryptedData);
        } else if (Utils.isArray(decryptedData)) {
            index = !Utils.getListObject(decryptedData).isEmpty() ? Utils.getListObject(decryptedData).get(0) : new HashMap<>();
        }
        if (index != null && !index.isEmpty()) {
            if (prefs.isAdsRemoved()) {
                index.put("adBanner", "");
                index.put("adInter", "");
                index.put("adNative", "");
                index.put("appBanner", "");
                index.put("appInter", "");
            }
            prefs.setString("appId", checkKey(index, "appid"));
            prefs.setString("adBanner", checkKey(index, "adBanner"));
            prefs.setString("adInter", checkKey(index, "adInter"));
            prefs.setString("adNative", checkKey(index, "adNative"));
            prefs.setString("adReward", checkKey(index, "adReward"));
            prefs.setString("appInter", checkKey(index, "appInter"));
            prefs.setString("appBanner", checkKey(index, "appBanner"));
            prefs.setString("fb_banner", checkKey(index, "fb_banner"));
            prefs.setString("fb_inter", checkKey(index, "fb_inter"));
            prefs.setString("fb_native", checkKey(index, "fb_native"));
            prefs.setString("version", checkKey(index, "version"));
            prefs.setString("message", checkKey(index, "message"));
            prefs.setString("url", checkKey(index, "url"));
            prefs.setBoolean("forceUpdate", Utils.parseBoolean(checkKey(index, "forceUpdate"), true));
            prefs.setString("fb", checkKey(index, "fb"));
            prefs.setString("email", checkKey(index, "email"));
            prefs.setString("website", checkKey(index, "website"));
            prefs.setString("telegram", checkKey(index, "telegram"));
            prefs.setString("privacy", checkKey(index, "privacy"));
            prefs.setString("default_user_agent", checkKey(index, "default_user_agent"));
            prefs.setString("policy_1", checkKey(index, "policy_1"));
            prefs.setString("policy_2", checkKey(index, "policy_2"));
            prefs.setString("replace", checkKey(index, "replace"));
            prefs.setString("replace_2", checkKey(index, "replace_2"));
            prefs.setString("replace_3", checkKey(index, "replace_3"));
            prefs.setString("with", checkKey(index, "with"));
            prefs.setString("with_2", checkKey(index, "with_2"));
            prefs.setString("with_3", checkKey(index, "with_3"));
            prefs.setString("httpcanary", checkKey(index, "httpcanary"));
            prefs.setString("appverifs", checkKey(index, "appverifs"));
            prefs.setString("gouardvvnn_url", checkKey(index, "gouardvvnn_url"));
            prefs.setString("proxyxpola_url", checkKey(index, "proxyxpola_url"));
            prefs.setString("version_stoped", checkKey(index, "version_stoped"));
            prefs.setString("rooot", checkKey(index, "rooot"));
            prefs.setString("loadsetting", checkKey(index, "loadsetting"));
            prefs.setString("emulator", checkKey(index, "emulator_url"));
            prefs.setString("notconnection", checkKey(index, "notconnection_url"));
            prefs.setString("appSnfr", checkKey(index, "appSnfr"));
            prefs.setString("contacturl", checkKey(index, "contacturl"));
            prefs.setString("latest_version", checkKey(index, "latest_version"));
            prefs.setString("z_package_apps", checkKey(index, "z_package_apps"));
            prefs.setString("sub_type", checkKey(index, "sub_type"));
            prefs.setString("admobload", checkKey(index, "admobload"));

            prefs.setString("statut", checkKey(index, "statut"));
            prefs.setInt("ads_counter", Integer.parseInt(Utils.getValue(index, "ads_counter", "3")));
            prefs.setInt("ads_counter_web", Integer.parseInt(Utils.getValue(index, "ads_counter_web", "3")));
            prefs.setInt("max", Integer.parseInt(Utils.getValue(index, "max_native_load", "6")));
            prefs.setBoolean("stop_check_npv", Utils.parseBoolean(Utils.getValue(index, "vpn", "false"), false));
            prefs.setBoolean("url_check_npv", Utils.parseBoolean(Utils.getValue(index, "vpn_url", "file:///android_asset/fonts/offline.mp4"), false));
            prefs.setString("yacine_key", Utils.getValue(index, "yacine_key", Constants.YC_KEY));
            prefs.setString("yacine_url", Utils.getValue(index, "yacine_url", Constants.YC_URL_CONTAINS));
            prefs.setString("yacine_url_2", Utils.getValue(index, "yacine_url_2", Constants.YC_URL_CONTAINS));
        }
    }


    @NonNull
    private static String checkKey(HashMap<String, Object> index, String key) {
        if (index != null && index.containsKey(key) && index.get(key) != null)
            return Objects.requireNonNull(index.get(key)).toString();
        return "";
    }
}
