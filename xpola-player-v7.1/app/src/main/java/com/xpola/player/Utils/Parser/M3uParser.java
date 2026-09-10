package com.xpola.player.Utils.Parser;

import android.app.Activity;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xpola.player.Connections.NetRequests;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.xpola.player.Utils.Constants;
@SuppressWarnings("deprecation")
public class M3uParser extends AsyncTask<String, Void, List<HashMap<String, Object>>> {
    public static String getK6() { return "kL"; }

    private final IHlsParser iHlsParser;
    private final Prefs prefs;
    private final Activity activity;

    private String url = "", error = "", headersString = "", userAgent = "", androidId = "";

    // Constants
    private static final String EXTINF = "EXTINF";
    private static final String EXTVLCOPT = "EXTVLCOPT";
    private static final String KODIPROP = "KODIPROP";
    private static final String REFERRER = "referrer";

    public static final String NAME = "name";
    public static final String LOGO = "logo";
    public static final String GROUP = "group";
    public static final String SUB_LIST = "sub_list";
    public static final String POSITION = "position";
    public static final String ORIGIN = "origin";
    public static final String COOKIE = "cookie";
    public static final String AUTH = "auth";
    public static final String SUBTITLE = "subtitle";
    public static final String REFERER = "referer";
    public static final String USER_AGENT = "user-agent";
    public static final String PLAYER_TYPE = "player-type";
    public static final String H_REQUESTED_WITH = "http-requested";
    public static final String X_REQUESTED_WITH = "x-requested-with";
    public static final String DRM_LICENSE = "drm-license";
    public static final String DRM_SCHEME = "drm-scheme";
    public static final String FIND = "find";
    public static final String SERVERS = "servers";
    public static final String HEADERS = "headers";
    public static final String URL = "url";

    // Regex patterns
    private static final String UA_REGX = "user-agent=(.+)";
    private static final String REF_REGX = "referrer=(.+)";
    private static final String ORIGIN_REGX = "origin=(.+)";
    private static final String COOKIE_REGX = "cookie=(.+)";
    private static final String AUTH_REGX = "auth=(.+)";
    private static final String PLAYER_TYPE_REGX = "player-type=(.+)";
    private static final String SERVERS_REGX = "servers=(.+)";
    private static final String GROUP_REGX = "group-title[=]*\"(.*?)\"";
    private static final String TVG_LOGO_REGX = "tvg-logo=\"(.*?)\"";
    private static final String NAME_REGX = ".+,(.*?[^\n\r]+)";
    private static final String X_REQUESTED_WITH_REGX = "http-requested=(.+)";
    private static final String FIND_REGX = "find=(.+)";
    private static final String DRM_LICENSE_URL_OR_KEY_REGX = "(?:http-drm-license|inputstream\\.adaptive\\.license_key)=(.+)";
    private static final String DRM_SCHEME_REGX = "(?:http-drm-scheme|inputstream\\.adaptive\\.license_type)=(.+)";
    private static final String SUBTITLE_REGX = "subtitle=(.+)";
    // Memory control
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ENTRIES = 10000;
    private int entryCount = 0;

    public M3uParser(Activity activity, Prefs prefs, IHlsParser iHlsParser) {
        this.activity = activity;
        this.prefs = prefs;
        this.iHlsParser = iHlsParser;
    }

    @Override
    protected List<HashMap<String, Object>> doInBackground(@NonNull String[] data) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        ArrayList<HashMap<String, Object>> returnedList = new ArrayList<>();
        ArrayList<HashMap<String, Object>> all = new ArrayList<>();

        if (data.length > 0) url = data[0].trim();
        if (data.length > 1) userAgent = data[1];
        if (data.length > 2) headersString = data[2];
        if (data.length > 3) androidId = data[3];
        error = "";

        BufferedReader reader = null;

        try {
            // تعديل الروابط حسب prefs
            if (Sec.SignatureCheck.isSignatureValid(activity)) {
                url = url.replace(prefs.getString("replace", ""), prefs.getString("with", ""))
                        .replace(prefs.getString("replace_2", ""), prefs.getString("with_2", ""))
                        .replace(prefs.getString("replace_3", ""), prefs.getString("with_3", ""));
            }
            String androidVersion = android.os.Build.VERSION.RELEASE;
            // تحضير الهيدرز
            Map<String, String> headers = new HashMap<>();
            if (!headersString.isEmpty()) {
                headers = Utils.getMapString(headersString);
                headers.remove(USER_AGENT);
            }
            if (!userAgent.isEmpty()) headers.put(USER_AGENT, userAgent);

            String clearKey = "xpola_player" + "(" + androidId + ")" + "("+Utils.isCanaryInstalled(prefs.getContext())+")" + "("+ androidVersion + ")" + "("+Constants.version+")";
            headers.put("ClearKey", clearKey);

            // الحصول على InputStream
            InputStream stream;

// إذا ليس رابط URL و ليس URI صحيح → حوّله إلى file://
            if (!Utils.isUrl(url) &&
                    !url.startsWith("file://") &&
                    !url.startsWith("content://")) {

                url = "file:///" + url;
            }

            if (Utils.isUrl(url)) {
                stream = NetRequests.getInputStream(url, headers);
            } else {
                stream = activity.getContentResolver().openInputStream(Uri.parse(url));
            }

            // قراءة المحتوى كله لفك التشفير إذا لزم
            String allText = readStreamToString(stream);
            if (allText.startsWith(Utils.XOR_ENCRYPTION_STARTS_WITH)) {
                allText = Utils.decrypt(allText, activity);
            }

            // استخدام BufferedReader لمعالجة الأسطر
            reader = new BufferedReader(new StringReader(allText));

            HashMap<String, Object> HlsParser = null;
            HashMap<String, String> serverHeaders = null;
            String line;
             while ((line = reader.readLine()) != null && !isCancelled() && entryCount < MAX_ENTRIES) {
                line = line.trim();
                if (line.isEmpty()) continue;
                 String lower = line.toLowerCase();

                // EXTINF
                if (line.toUpperCase().contains(EXTINF)) {
                    HlsParser = new HashMap<>();
                    serverHeaders = new HashMap<>();
                    String poster = match(line, TVG_LOGO_REGX);
                    String name = match(line, NAME_REGX);
                    HlsParser.put(NAME, name != null ? name : "");
                    HlsParser.put(LOGO, (poster != null && !poster.isEmpty()) ? poster : "R.drawable.app_icon");
                    HlsParser.put(GROUP, match(line, GROUP_REGX));

                    // Headers
                } else if ((line.toLowerCase().contains(REFERRER) || line.toLowerCase().contains(REFERER))
                        && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(REFERER, match(line, REF_REGX));

                } else if (line.toLowerCase().contains(USER_AGENT) && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(USER_AGENT, match(line, UA_REGX));

                } else if (line.toLowerCase().contains(PLAYER_TYPE) && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(PLAYER_TYPE, match(line, PLAYER_TYPE_REGX));

                } else if (line.toLowerCase().contains(H_REQUESTED_WITH) && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(X_REQUESTED_WITH, match(line, X_REQUESTED_WITH_REGX));

                } else if (line.toLowerCase().contains(FIND) && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(FIND, match(line, FIND_REGX));

                } else if (line.toLowerCase().contains(ORIGIN) && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(ORIGIN, match(line, ORIGIN_REGX));

                } else if (line.toLowerCase().contains(COOKIE) && line.toUpperCase().contains(EXTVLCOPT)) {
                    if (serverHeaders != null) serverHeaders.put(COOKIE, match(line, COOKIE_REGX));

                } else if (line.toUpperCase().contains("EXTVLCOPT:HTTP-AUTH=")) {
                    if (serverHeaders != null) {
                        String authValue = match(line, AUTH_REGX);
                        if (authValue.startsWith("auth=")) authValue = authValue.substring(5);
                        serverHeaders.put(AUTH, authValue);
                    }

                } else if (line.toUpperCase().contains("EXTVLCOPT:HTTP-SUBTITLE=")) {
                    if (serverHeaders != null) {
                        String subValue = match(line, SUBTITLE_REGX);
                        if (subValue.startsWith("subtitle=")) subValue = subValue.substring(9);
                        serverHeaders.put(SUBTITLE, subValue);
                    }

                } else if ((line.toLowerCase().contains(DRM_LICENSE) || line.toLowerCase().contains("inputstream.adaptive.license_key"))
                        && (line.toUpperCase().contains(EXTVLCOPT) || line.toUpperCase().contains(KODIPROP))) {
                    if (HlsParser != null) HlsParser.put(DRM_LICENSE, match(line, DRM_LICENSE_URL_OR_KEY_REGX));

                } else if ((line.toLowerCase().contains(DRM_SCHEME) || line.toLowerCase().contains("inputstream.adaptive.license_type"))
                        && (line.toUpperCase().contains(EXTVLCOPT) || line.toUpperCase().contains(KODIPROP))) {
                    if (HlsParser != null) HlsParser.put(DRM_SCHEME, match(line, DRM_SCHEME_REGX));

                    // روابط الفيديو
                } else if ((lower.startsWith("http://") ||
                        lower.startsWith("https://") ||
                        lower.startsWith("rtsp://")) &&
                        HlsParser != null && HlsParser.containsKey(NAME)) {
                    if (serverHeaders != null) HlsParser.put(HEADERS, Utils.objectToString(serverHeaders));

                    // معالجة DRM inline
                    if (line.contains("drmScheme") && line.contains("drmLicense")) {
                        int index = line.indexOf("|drmScheme=");
                        int index2 = line.indexOf("&drmLicense=");
                        if (index != -1 && index2 != -1 && index2 > index + 11) {
                            HlsParser.put(DRM_SCHEME, line.substring(index + 11, index2));
                            HlsParser.put(DRM_LICENSE, line.substring(index2 + 12));
                            line = line.substring(0, index);
                        }
                    }

                    HlsParser.put(URL, line);
                    list.add(HlsParser);
                    all.add(HlsParser);
                    entryCount++;
                    if (entryCount % BATCH_SIZE == 0) System.gc();

                    // معالجة SERVERS
                } else if (line.startsWith(SERVERS) && HlsParser != null && HlsParser.containsKey(NAME)) {
                    HlsParser.put(SERVERS, IntentParser.createServers(match(line, SERVERS_REGX), activity));
                    list.add(HlsParser);
                    all.add(HlsParser);
                    entryCount++;
                }
            }

            // تنظيم القنوات في مجموعات
            organizeDataIntoGroups(list, all, returnedList);

        } catch (Exception e) {
            error = e.getMessage();
            Log.e("M3uParser", "Error parsing M3U", e);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                Log.e("M3uParser", "Error closing reader", e);
            }
        }

        return returnedList;
    }

    private void organizeDataIntoGroups(List<HashMap<String, Object>> list,
                                        List<HashMap<String, Object>> all,
                                        List<HashMap<String, Object>> returnedList) {
        Utils.sortListMap(list, GROUP, false, true);
        String currentGroup = "";
        List<HashMap<String, Object>> subList = new ArrayList<>();

        // مجموعة All
        HashMap<String, Object> allHashMap = new HashMap<>();
        allHashMap.put(NAME, "All(" + all.size() + ")");
        allHashMap.put(POSITION, "-1");
        allHashMap.put(SUB_LIST, Utils.objectToString(all));
        returnedList.add(allHashMap);

        for (HashMap<String, Object> item : list) {
            if (item == null) continue;
            String itemGroup = Utils.getValue(item, GROUP, "").trim();
            if (itemGroup.isEmpty()) itemGroup = "Undefined";

            if (!currentGroup.equalsIgnoreCase(itemGroup)) {
                if (!subList.isEmpty()) addGroupToResult(returnedList, currentGroup, subList);
                subList = new ArrayList<>();
                currentGroup = itemGroup;
            }
            subList.add(item);
        }
        if (!subList.isEmpty()) addGroupToResult(returnedList, currentGroup, subList);
    }

    private void addGroupToResult(List<HashMap<String, Object>> returnedList,
                                  String groupName,
                                  List<HashMap<String, Object>> groupItems) {
        Utils.sortListMap(groupItems, NAME, false, true);
        HashMap<String, Object> groupMap = new HashMap<>();
        groupMap.put(NAME, groupName + "(" + groupItems.size() + ")");
        groupMap.put(POSITION, String.valueOf(returnedList.size()));
        groupMap.put(SUB_LIST, Utils.objectToString(groupItems));
        returnedList.add(groupMap);
    }

    @Override
    protected void onPostExecute(final List<HashMap<String, Object>> data) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (data != null && !data.isEmpty()) iHlsParser.onDataParsedSuccessfully(data);
            else if (data != null) iHlsParser.onFailed("List Empty");
            else iHlsParser.onFailed(error);
        });
        super.onPostExecute(data);
    }

    @NonNull
    private String match(String from, String pat) {
        Matcher matcher = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(from);
        if (matcher.find()) return Utils.objectToString(matcher.group(1)).trim();
        return "";
    }

    private String readStreamToString(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        return sb.toString();
    }

    public void parse(String... params) {
        execute(params);
    }

    public interface IHlsParser {
        void onDataParsedSuccessfully(List<HashMap<String, Object>> data);
        void onFailed(String error);
    }
}
