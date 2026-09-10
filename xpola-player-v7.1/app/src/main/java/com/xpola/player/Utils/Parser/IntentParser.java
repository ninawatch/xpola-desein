package com.xpola.player.Utils.Parser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentParser {
    public static final String DRM_INFO_SEPARATOR = "|drm-info=";

    // دالة لتحليل بيانات Intent والحصول على كائن Data يحتوي على المعلومات المستخلصة
    public static void getData(Intent intent, Context context, DataCallback callback) {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            Data data = new Data();
            try {
                if (intent != null) {

                    // ---------------------------
                    //  حل مشكل الروابط المحلية 100%
                    // ---------------------------
                    Uri dataUri = intent.getData();
                    if (dataUri != null) {
                        String sch = dataUri.getScheme();

                        //  ملف محلي → لا decrypt ولا parsing إطلاقاً
                        if ("file".equalsIgnoreCase(sch) || "content".equalsIgnoreCase(sch)) {

                            data.setUrl(dataUri.toString());
                            data.setTitle(getTitleFromUrl(dataUri.toString()));
                            data.setScheme(sch);

                            handler.post(() -> callback.onDataReceived(data));
                            return;
                        }
                    }
                    // ---------------------------


                    String scheme = Utils.objectToString(intent.getScheme());
                    data.setScheme(scheme);
                    Bundle bundle = intent.getExtras();

                    if (!isWebScheme(intent) && !isFilesScheme(scheme) && bundle != null) {

                        data.setTitle(Utils.getValue(bundle, "title",
                                Utils.getValue(bundle, "name", "")));

                        if (bundle.containsKey(M3uParser.SERVERS)) {

                            String servers = Utils.getValue(bundle, M3uParser.SERVERS, "[]");
                            data.setServers(Utils.getListString(servers));

                        } else {

                            String dataUrl = Utils.getValue(bundle, M3uParser.URL,
                                    Utils.objectToString(intent.getData()));

                            if (!Utils.isUrl(dataUrl) && !Utils.isArray(dataUrl))
                                dataUrl = Utils.decrypt(dataUrl, context);

                            if (Utils.isArray(dataUrl)) {
                                data.setServers(Utils.getListString(createServers(dataUrl, context)));
                                data.setTitle("");
                            } else {
                                data.setUrl(dataUrl);
                            }

                            String headers = Utils.getValue(bundle, M3uParser.HEADERS, "{}");
                            data.setPlayerType(Utils.getValue(bundle, M3uParser.PLAYER_TYPE, ""));
                            data.setDrmScheme(Utils.getValue(bundle, M3uParser.DRM_SCHEME, ""));
                            data.setDrmLicense(Utils.getValue(bundle, M3uParser.DRM_LICENSE, ""));
                            data.setHeaders(toJson(headers));
                            data.setUserAgent(Utils.getValueString(data.getHeaders(), M3uParser.USER_AGENT, ""));
                        }

                    } else if (intent.getData() != null) {

                        String dataUrl = Uri.decode(getUrl(
                                Utils.objectToString(intent.getData()),
                                Utils.objectToString(intent.getScheme()),
                                context
                        ));

                        if (dataUrl.contains("player-type=json")) {

                            String jsonData = Utils.getDataFromUrl(dataUrl.split("\\|")[0]);
                            if (jsonData != null) {

                                jsonData = jsonData.trim();
                                if (jsonData.startsWith("\"") && jsonData.endsWith("\"")) {
                                    jsonData = jsonData.substring(1, jsonData.length() - 1);
                                }
                                jsonData = jsonData.trim();

                                if (jsonData.startsWith("XPOLA909")) {
                                    try {
                                        jsonData = Utils.decrypt(jsonData, context);
                                    } catch (SecurityException e) {
                                        throw new SecurityException("Decryption failed: " + e.getMessage());
                                    }
                                }

                                data.setServers(Utils.getListString(createServersFromJson(jsonData)));
                                data.setTitle("");
                            }

                        } else {

                            String url = dataUrl;

                            if (Utils.isArray(dataUrl)) {

                                data.setServers(Utils.getListString(createServers(dataUrl, context)));
                                data.setTitle("");

                            } else {

                                int index = dataUrl.indexOf("|");
                                int index2 = dataUrl.indexOf(DRM_INFO_SEPARATOR);

                                String drmScheme = "", drmLicense = "";
                                String headers = "";

                                if (index != -1) {
                                    headers = dataUrl.substring(index + 1);
                                    if (index2 != -1 && index + 1 < index2) {
                                        headers = dataUrl.substring(index + 1, index2);
                                    }
                                    url = dataUrl.substring(0, index);
                                }

                                if (index2 != -1) {
                                    String[] drmInfo = Utils.objectToString(
                                            Uri.decode(dataUrl).substring(index2 + DRM_INFO_SEPARATOR.length())
                                    ).split("\\|");

                                    if (drmInfo.length == 2) {
                                        drmScheme = drmInfo[0];
                                        drmLicense = drmInfo[1];
                                    }
                                }

                                data.setUrl(url);

                                if (!drmScheme.isEmpty() && !drmLicense.isEmpty()) {
                                    data.setDrmLicense(drmLicense);
                                    data.setDrmScheme(drmScheme);
                                }

                                data.setTitle(getTitleFromUrl(url));
                                data.setHeaders(toJson(headers));
                                data.setUserAgent(Utils.getValueString(data.getHeaders(), M3uParser.USER_AGENT, ""));
                            }
                        }
                    }
                }

                handler.post(() -> callback.onDataReceived(data));

            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });

    }

    // دالة للتحقق مما إذا كانت خطة الـ URL هي ويب
    public static boolean isWebScheme(@NonNull Intent intent) {
        String scheme = Utils.objectToString(intent.getScheme()).toLowerCase();
        return scheme.equalsIgnoreCase("servers")
                || scheme.equalsIgnoreCase("xmtv")
                || scheme.equalsIgnoreCase("xmtv-m3u")
                || scheme.equalsIgnoreCase("xmtv-browser")
                || scheme.equalsIgnoreCase("xmtv-iframe")
                || scheme.equalsIgnoreCase("intent");
    }

    public static boolean isFilesScheme(@NonNull String scheme) {
        return scheme.equalsIgnoreCase("file")
                || scheme.equalsIgnoreCase("content");
    }

    //URL  دالة لاستخراج العنوان من الـ
    @NonNull
    private static String getTitleFromUrl(String url) {
        try {
            String path = Uri.parse(url).getPath();
            if (path != null) {
                int i = path.lastIndexOf("/");
                int i2 = path.lastIndexOf(Utils.getExtension(path));
                if (i != -1 && i2 != -1)
                    return path.substring(i + 1, i2 + Utils.getExtension(path).length());
                else if (i != -1) {
                    return path.substring(i + 1);
                }
            }
        } catch (Exception ignored) {
            // تجاهل الاستثناءات
        }
        return "";
    }

    // دالة لتحليل الـ URL وإزالة بعض الأجزاء الغير ضرورية
    @NonNull
    public static String getUrl(@NonNull String url, String scheme, Context context) {
        String findScheme = Utils.Match("(" + scheme + "[:\\/\\/]*)", url, 1);
        String findIntent = Utils.Match("(#intent.+)", url, 1);
        if (findScheme != null)
            url = url.replace(findScheme, "");
        if (findIntent != null)
            url = url.replace(findIntent, "");
        if (url.startsWith("http") || Utils.isArray(url) ) {
            return url;
        } else {
            return Utils.decrypt(url, context);
        }
    }

    // دالة لتحويل نص JSON إلى HashMap
    public static HashMap<String, String> toJson(String s) {
        HashMap<String, String> headers = new HashMap<>();
        try {
            if (Utils.isObject(s)) {
                headers = Utils.getMapString(s);
            } else {
                String[] ss = s.split("\\|+");
                for (String d : ss) {
                    int i = d.indexOf("=");
                    if (i != -1) {
                        headers.put(Utils.objectToString(d.substring(0, i)).toLowerCase(),
                                Utils.objectToString(d.substring(i + 1)));
                    }
                }
            }
        } catch (Throwable ignored) {
            // تجاهل الاستثناءات
        }
        return getHeadersWithLowercaseKeys(headers);
    }

    // دالة لإنشاء قائمة من السيرفرات من سلسلة نصية
    public static String createServers(String line, Context context) {
        List<String> array = Utils.getList(Utils.isArray(line) ? line : Utils.decrypt(line, context));
        List<HashMap<String, String>> servers = new ArrayList<>();
        int x = 1;

        for (String s : array) {
            String url = s, headers = "", drmLicense = "", drmScheme = "";
            String serverName = "S" + x;  // Default name

            // Extract URL and headers
            int i = Uri.decode(s).indexOf("|");
            int i2 = Uri.decode(s).toLowerCase().indexOf(DRM_INFO_SEPARATOR);
            if (i != -1) {
                url = Uri.decode(s).substring(0, i);
                headers = Uri.decode(s).substring(i + 1);
                if (i2 != -1 && i + 1 < i2)
                    headers = Uri.decode(s).substring(i + 1, i2);
                // Find name in headers if exists
                String namePattern = "name=([^|]+)";
                Pattern pattern = Pattern.compile(namePattern);
                Matcher matcher = pattern.matcher(headers);
                if (matcher.find()) {
                    serverName = matcher.group(1);  // Extract name from header
                }
                headers = Utils.objectToString(toJson(headers));
            }
            if (i2 != -1) {
                String[] drmInfo = Utils.objectToString(Uri.decode(s).substring(i2 + DRM_INFO_SEPARATOR.length())).split("\\|");
                if (drmInfo.length >= 2) {
                    drmScheme = Utils.objectToString(drmInfo[0]);
                    drmLicense = Utils.objectToString(drmInfo[1]);
                }
            }
            HashMap<String, String> server = new HashMap<>();
            server.put(M3uParser.NAME, serverName);
            server.put(M3uParser.URL, url);
            server.put(M3uParser.HEADERS, headers);
            if (!drmScheme.isEmpty() && !drmLicense.isEmpty()) {
                server.put(M3uParser.DRM_LICENSE, drmLicense);
                server.put(M3uParser.DRM_SCHEME, drmScheme);
            }
            servers.add(server);
            x++;
        }
        return Utils.objectToString(servers);
    }

    public static String createServersFromJson(String json) {
        List<HashMap<String, String>> servers = new ArrayList<>();
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject videoObject = jsonArray.getJSONObject(i);
                String videoUrl = videoObject.getString("video_url");
                StringBuilder headersBuilder = new StringBuilder();
                java.util.Iterator<String> keys = videoObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!key.equals("video_url")) {
                        if (headersBuilder.length() > 0) {
                            headersBuilder.append("|");
                        }
                        headersBuilder.append(key).append("=").append(videoObject.getString(key));
                    }
                }
                HashMap<String, String> server = new HashMap<>();
                server.put(M3uParser.NAME, videoObject.optString("name", "S" + (i + 1)));
                server.put(M3uParser.URL, videoUrl);
                server.put(M3uParser.HEADERS, Utils.objectToString(toJson(headersBuilder.toString())));
                servers.add(server);
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return Utils.objectToString(servers);
    }

    // دالة لتحويل headers إلى مصفوفة نصوص
    @NonNull
    public static String[] getHeaders(Map<String, String> headers) {
        try {
            if (headers != null) {
                String[] hs = new String[headers.size() * 2];
                int i = 0;
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    hs[i] = h.getKey();
                    i++;
                    hs[i] = h.getValue();
                    i++;
                }
                return hs;
            }
        } catch (Exception ignored) {
            // تجاهل الاستثناءات
        }
        return new String[]{};
    }

    @NonNull
    public static HashMap<String, String> getHeadersWithLowercaseKeys(Map<String, String> headers) {
        HashMap<String, String> newHeaders = new HashMap<>();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header != null && header.getKey() != null)
                newHeaders.put(header.getKey().toLowerCase(), header.getValue());
        }
        return newHeaders;
    }

    public interface DataCallback {
        void onDataReceived(Data data);
        void onError(Exception e);
    }
}
