package com.xpola.player.Utils.Parser;

import android.util.Log;

import androidx.annotation.NonNull;

import com.xpola.player.Connections.HttpRequest;
import com.xpola.player.Interfaces.IYLGetListener;
import com.xpola.player.Utils.Constants;
import com.xpola.player.Utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YacineLinks {
    public static String getK7() { return "Z#"; }
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private String userAgent = "";
    private String key = "";

    private YacineLinks(String url, String userAgent, Map<String, String> headers, String key) {
        this.url = url;
        this.headers.putAll(headers);
        this.key = key;
        this.userAgent = userAgent;
    }

    @NonNull
    public static YacineLinks getInstance(String url, String userAgent, Map<String, String> headers, String key) {
        return new YacineLinks(url, userAgent, headers, key);
    }

    public void get(IYLGetListener iYlGetListener) {
        new Thread(() -> {
            try {
                // إضافة "user-agent" إذا لم يكن موجودًا في headers
                if (!headers.containsKey("user-agent") && !userAgent.isEmpty()) {
                    headers.put("user-agent", userAgent);
                }

                // إرسال الطلب HTTP للحصول على البيانات
                HttpRequest.get(url, headers).getData((response, otherData) -> {
                    try {
                        // التأكد من أن المفتاح غير فارغ
                        key = key == null || key.isEmpty() ? Constants.YC_KEY : key;

                        // فك تشفير الاستجابة
                        String decodedResponse = Utils.YCDecoder(response, key + Utils.objectToString(otherData));
                        Map<String, Object> object = Utils.getObject(decodedResponse);

                        // التحقق من وجود البيانات
                        if (object.containsKey("data")) {
                            List<HashMap<String, Object>> list = Utils.getListObject(object.get("data"));

                            // التحقق من وجود عناصر في القائمة
                            if (!list.isEmpty()) {
                                object = list.get(0);

                                // إعادة تعيين الرؤوس واستخراج الرابط
                                headers = new HashMap<>();
                                url = Utils.getValue(object, "url", "file:///android_asset/fonts/offline.mp4");
                                headers.putAll(IntentParser.getHeadersWithLowercaseKeys(Utils.getMapString(Utils.getValue(object, "headers", "{}"))));
                            } else {
                                // في حال كانت القائمة فارغة، نضع الرابط الافتراضي
                                url = "file:///android_asset/fonts/offline.mp4";
                            }
                        } else {
                            // في حال عدم وجود بيانات، نضع الرابط الافتراضي
                            url = "file:///android_asset/fonts/offline.mp4";
                        }
                    } catch (Exception e) {
                        // في حال حدوث أي استثناء أثناء المعالجة، نضع الرابط الافتراضي
                        Log.e("YacineLinks", "Error processing response", e);
                        url = "file:///android_asset/fonts/offline.mp4";
                    }

                    // إبلاغ المستمع (Listener) بالرابط الجديد
                    Utils.handler.post(() -> iYlGetListener.onGetListener(url, headers));
                });
            } catch (Exception e) {
                // التعامل مع أي استثناء قد يحدث أثناء إرسال الطلب أو استلام البيانات
                Log.e("YacineLinks", "Error making HTTP request", e);
                url = "file:///android_asset/fonts/offline.mp4";
                Utils.handler.post(() -> iYlGetListener.onGetListener(url, headers));
            }
        }).start();
    }

}
