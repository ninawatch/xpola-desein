package com.xpola.player.Utils.Parser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xpola.player.Connections.HttpRequest;
import com.xpola.player.Interfaces.OnLinkFounded;
import com.xpola.player.Utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinksFinder {

    private static final String TAG = "LinksFinder";
    private static final int AUTO_TIMEOUT_SECONDS = 20;
    private static final int PAGE_LOAD_WAIT_MS = 3000;

    // Supported video/audio formats for auto-detection
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "m3u8", "ts", "mpd", "mp4", "mp3"
    ));

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".m3u8", ".ts", ".mpd", ".mp4", ".mp3",
            ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv"
    ));

    private static final Set<String> STREAM_PATTERNS = new HashSet<>(Arrays.asList(
            "/video/", "/stream/", "/live/", "/hls/", "/dash/",
            "manifest", "playlist", "master.m3u8", "index.m3u8",
            ".m3u8?", ".mpd?", "aws", "cloudfront", "cdn"
    ));

    private static final Set<String> BLACKLIST_PATTERNS = new HashSet<>(Arrays.asList(
            "yandex", "google-analytics", "doubleclick", "ads", "pixel",
            "metrics", "tracking", "stat", "analytics", "beacon", "log",
            "monitoring", "telemetry", "adserver", "googlesyndication",
            "facebook.com/tr", "twitter.com/i/ads", "moatads.com",
            "scorecardresearch.com", "criteo.com", "rubiconproject.com",
            "googletagmanager.com", "googletagservices.com",
            "fonts.googleapis.com", "ajax.googleapis.com",
            ".html", ".php", ".aspx", ".jsp", ".js", ".css"
    ));

    private final Handler handler = new Handler(Looper.getMainLooper());
    private OnLinkFounded onLinkFounded;
    private List<String> finds = new ArrayList<>();
    private String pageUrl = "";
    private static final String FALLBACK_URL = "https://github.com/video-xplayer/videos/raw/refs/heads/main/web2exo.mp4";

    private boolean autoMode = false;
    private volatile boolean autoFinished = false;
    private Runnable autoTimeoutRunnable = null;
    private WebView currentWebView = null;
    private final List<String> detectedUrls = new ArrayList<>();

    @NonNull
    public static LinksFinder getNewInstance() {
        return new LinksFinder();
    }

    public LinksFinder() {
        resetState();
    }

    private void resetState() {
        autoFinished = false;
        autoTimeoutRunnable = null;
        currentWebView = null;
        onLinkFounded = null;
        finds.clear();
        pageUrl = "";
        autoMode = false;
        detectedUrls.clear();
    }

    // =====================================================
    // ENTRY POINT (NORMAL MODE)
    // =====================================================

    public void findLink(
            String url,
            @NonNull String finds,
            String userAgent,
            Map<String, String> headers,
            OnLinkFounded callback
    ) {
        resetState();
        this.pageUrl = url;
        this.onLinkFounded = callback;
        this.finds = Arrays.asList(finds.split(";"));

        autoMode = containsAuto(this.finds);

        if (autoMode) {
            Log.d(TAG, "AUTO MODE detected – waiting for WebView call");
            return;
        }

        getPageContent(userAgent, headers);
    }

    // =====================================================
    // AUTO MODE (WEBVIEW INTERCEPTION) - IMPROVED
    // =====================================================

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public void findLinkAuto(
            Context context,
            WebView webView,
            String url,
            String userAgent,
            Map<String, String> headers,
            OnLinkFounded callback
    ) {
        resetState();

        this.onLinkFounded = callback;
        this.currentWebView = webView;

        Log.d(TAG, "Starting AUTO mode for URL: " + url);
        Log.d(TAG, "Looking for formats: " + SUPPORTED_FORMATS);

        handler.post(() -> {
            try {
                cleanupWebViewResources(webView);
                setupWebView(webView, userAgent, url, headers);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up WebView", e);
                deliverResult(FALLBACK_URL, true);
            }
        });
    }

    private void setupWebView(WebView webView, String userAgent, String url, Map<String, String> headers) {
        try {
            // Clear cookies
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
            cookieManager.flush();

            // Configure WebView settings
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setDatabaseEnabled(true);
            webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
            webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            // Disable AppCache for older versions
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                try {
                    webView.getSettings().getClass()
                            .getMethod("setAppCacheEnabled", boolean.class)
                            .invoke(webView.getSettings(), false);
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Set unique User-Agent
            String uniqueUserAgent = generateUniqueUserAgent(userAgent);
            webView.getSettings().setUserAgentString(uniqueUserAgent);
            Log.d(TAG, "Using User-Agent: " + uniqueUserAgent);

            // Setup timeout
            setupAutoTimeout();

            // Create WebViewClient with enhanced interception
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public WebResourceResponse shouldInterceptRequest(
                        WebView view,
                        WebResourceRequest request
                ) {
                    String reqUrl = request.getUrl().toString();

                    if (autoFinished) {
                        return super.shouldInterceptRequest(view, request);
                    }

                    // Check if URL is a valid video/stream
                    if (isValidVideoUrl(reqUrl)) {
                        Log.d(TAG, "✅ Video/Stream found via intercept: " + reqUrl);
                        detectedUrls.add(reqUrl);
                        // Return first valid URL found
                        if (!autoFinished) {
                            deliverResult(reqUrl, false);
                        }
                        return new WebResourceResponse(null, null, null);
                    }

                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "Page finished loading: " + url);

                    // Inject JavaScript to scan for video elements
                    injectVideoScannerJavaScript(view);

                    // Wait a bit and check again
                    handler.postDelayed(() -> {
                        if (!autoFinished && detectedUrls.isEmpty()) {
                            Log.d(TAG, "No video found after page load, checking HTML content");
                            checkPageContentViaJavaScript(view);
                        }
                    }, PAGE_LOAD_WAIT_MS);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode,
                                            String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e(TAG, "WebView error: " + errorCode + " - " + description);

                    if (!autoFinished && errorCode != -1) {
                        handler.postDelayed(() -> {
                            if (!autoFinished && detectedUrls.isEmpty()) {
                                Log.d(TAG, "WebView error, using fallback");
                                deliverResult(FALLBACK_URL, true);
                            }
                        }, 1000);
                    }
                }
            });

            // Load URL with headers
            long timestamp = System.currentTimeMillis();
            if (headers != null && !headers.isEmpty()) {
                Map<String, String> webViewHeaders = new HashMap<>(headers);
                webViewHeaders.put("Cache-Control", "no-cache, no-store, must-revalidate");
                webViewHeaders.put("Pragma", "no-cache");
                webViewHeaders.put("Expires", "0");
                webViewHeaders.put("X-Request-ID", String.valueOf(timestamp));
                webView.loadUrl(url, webViewHeaders);
            } else {
                webView.loadUrl(url);
            }

            Log.d(TAG, "WebView started loading URL");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView", e);
            deliverResult(FALLBACK_URL, true);
        }
    }

    private void injectVideoScannerJavaScript(WebView webView) {
        String jsCode =
                "javascript:(function() {" +
                        "    var videoUrls = [];" +
                        "    " +
                        "    // Check video elements" +
                        "    var videos = document.getElementsByTagName('video');" +
                        "    for (var i = 0; i < videos.length; i++) {" +
                        "        var src = videos[i].getAttribute('src');" +
                        "        if (src && src.length > 0) videoUrls.push(src);" +
                        "        " +
                        "        // Check source elements inside video" +
                        "        var sources = videos[i].getElementsByTagName('source');" +
                        "        for (var j = 0; j < sources.length; j++) {" +
                        "            var sourceSrc = sources[j].getAttribute('src');" +
                        "            if (sourceSrc && sourceSrc.length > 0) videoUrls.push(sourceSrc);" +
                        "        }" +
                        "    }" +
                        "    " +
                        "    // Check all source elements" +
                        "    var allSources = document.getElementsByTagName('source');" +
                        "    for (var i = 0; i < allSources.length; i++) {" +
                        "        var src = allSources[i].getAttribute('src');" +
                        "        if (src && src.length > 0) videoUrls.push(src);" +
                        "    }" +
                        "    " +
                        "    // Check links with video extensions" +
                        "    var links = document.getElementsByTagName('a');" +
                        "    var videoExtensions = ['m3u8', 'ts', 'mpd', 'mp4', 'mp3', 'webm', 'mkv'];" +
                        "    for (var i = 0; i < links.length; i++) {" +
                        "        var href = links[i].getAttribute('href');" +
                        "        if (href) {" +
                        "            for (var j = 0; j < videoExtensions.length; j++) {" +
                        "                if (href.toLowerCase().indexOf('.' + videoExtensions[j]) > 0 ||" +
                        "                    href.toLowerCase().indexOf(videoExtensions[j] + '?') > 0) {" +
                        "                    videoUrls.push(href);" +
                        "                    break;" +
                        "                }" +
                        "            }" +
                        "        }" +
                        "    }" +
                        "    " +
                        "    // Send back unique URLs" +
                        "    var uniqueUrls = videoUrls.filter(function(v, i, a) { return a.indexOf(v) === i; });" +
                        "    for (var i = 0; i < uniqueUrls.length; i++) {" +
                        "        console.log('Found video URL: ' + uniqueUrls[i]);" +
                        "    }" +
                        "    window.location = 'jsbridge://videourl/' + encodeURIComponent(JSON.stringify(uniqueUrls));" +
                        "})();";

        webView.loadUrl(jsCode);
    }

    private void checkPageContentViaJavaScript(WebView webView) {
        String jsCode =
                "javascript:(function() {" +
                        "    var pageText = document.body.innerText;" +
                        "    var urlPattern = /(https?:\\/\\/[^\\s\"'<>]+)/gi;" +
                        "    var matches = pageText.match(urlPattern);" +
                        "    var videoUrls = [];" +
                        "    var formats = ['m3u8', 'ts', 'mpd', 'mp4', 'mp3'];" +
                        "    if (matches) {" +
                        "        for (var i = 0; i < matches.length; i++) {" +
                        "            for (var j = 0; j < formats.length; j++) {" +
                        "                if (matches[i].toLowerCase().indexOf(formats[j]) > 0) {" +
                        "                    videoUrls.push(matches[i]);" +
                        "                    break;" +
                        "                }" +
                        "            }" +
                        "        }" +
                        "    }" +
                        "    window.location = 'jsbridge://contenturl/' + encodeURIComponent(JSON.stringify(videoUrls));" +
                        "})();";

        webView.loadUrl(jsCode);
    }

    private void setupAutoTimeout() {
        if (autoTimeoutRunnable != null) {
            handler.removeCallbacks(autoTimeoutRunnable);
            autoTimeoutRunnable = null;
        }

        autoTimeoutRunnable = () -> {
            if (autoFinished) return;

            if (detectedUrls.isEmpty()) {
                Log.d(TAG, "AUTO timeout reached – no video found, using fallback URL");
                deliverResult(FALLBACK_URL, true);
            } else {
                Log.d(TAG, "AUTO timeout reached – delivering best video URL");
                deliverResult(getBestVideoUrl(), false);
            }
        };

        handler.postDelayed(autoTimeoutRunnable, TimeUnit.SECONDS.toMillis(AUTO_TIMEOUT_SECONDS));
    }

    private String generateUniqueUserAgent(String userAgent) {
        long timestamp = System.currentTimeMillis();
        String baseUA = (userAgent != null && !userAgent.isEmpty())
                ? userAgent
                : "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

        return baseUA + "&t=" + timestamp + "&r=" + Math.random();
    }

    private String getBestVideoUrl() {
        if (detectedUrls.isEmpty()) {
            return FALLBACK_URL;
        }

        // Prioritize formats in order: m3u8, mpd, mp4, ts, mp3
        for (String format : Arrays.asList("m3u8", "mpd", "mp4", "ts", "mp3")) {
            for (String url : detectedUrls) {
                if (url.toLowerCase().contains(format)) {
                    return url;
                }
            }
        }

        return detectedUrls.get(0);
    }

    private void deliverResult(final String url, final boolean isFallback) {
        if (autoFinished) return;

        autoFinished = true;

        if (autoTimeoutRunnable != null) {
            handler.removeCallbacks(autoTimeoutRunnable);
            autoTimeoutRunnable = null;
        }

        cleanupWebViewResources(currentWebView);

        handler.post(() -> {
            if (onLinkFounded != null) {
                if (isFallback) {
                    Log.d(TAG, "Delivering fallback URL");
                } else {
                    Log.d(TAG, "Delivering video URL: " + url);
                }
                onLinkFounded.onLinkFounded(url);
            } else {
                Log.e(TAG, "Callback is null, cannot deliver result");
            }
        });
    }

    private void cleanupWebViewResources(@Nullable WebView webView) {
        if (webView == null) return;

        try {
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
            webView.clearMatches();
            webView.clearSslPreferences();
            webView.setWebViewClient(null);
            webView.loadUrl("about:blank");

            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();

            Log.d(TAG, "WebView resources cleaned up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning WebView resources", e);
        }
    }

    // =====================================================
    // ENHANCED URL VALIDATION
    // =====================================================

    private boolean isValidVideoUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        String lowerUrl = url.toLowerCase().trim();

        // Check blacklist
        for (String pattern : BLACKLIST_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return false;
            }
        }

        // Check if URL contains any of the target formats
        for (String format : SUPPORTED_FORMATS) {
            if (lowerUrl.contains("." + format) ||
                    lowerUrl.contains("/" + format + "/") ||
                    lowerUrl.contains(format + "?")) {
                Log.d(TAG, "Found matching format: " + format);
                return true;
            }
        }

        // Check stream patterns
        for (String pattern : STREAM_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                // Additional check for video extensions
                for (String ext : VIDEO_EXTENSIONS) {
                    if (lowerUrl.contains(ext)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // =====================================================
    // HTML PARSING (NORMAL MODE)
    // =====================================================

    private void getPageContent(String userAgent, Map<String, String> headers) {
        new Thread(() -> {
            try {
                if (!headers.containsKey("user-agent") && userAgent != null && !userAgent.isEmpty()) {
                    headers.put("user-agent", userAgent);
                }

                String body = HttpRequest.get(pageUrl, headers).String();
                String result = extractUrlFromHtml(body, finds);

                if (onLinkFounded != null) {
                    handler.post(() -> onLinkFounded.onLinkFounded(result));
                }

            } catch (Throwable e) {
                Log.e(TAG, "HTML parsing error", e);
                if (onLinkFounded != null) {
                    handler.post(() -> onLinkFounded.onLinkFounded(FALLBACK_URL));
                }
            }
        }).start();
    }

    @NonNull
    private String extractUrlFromHtml(String content, List<String> finds) {
        content = content.replaceAll("[\n\r\t]+", " ")
                .replaceAll("&quot;", "\"")
                .trim();

        // Enhanced regex for URLs
        String regex = "(https?://[^\\s\"'<>\\\\]+)";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(content);

        List<String> matchingUrls = new ArrayList<>();

        while (matcher.find()) {
            String url = Utils.objectToString(matcher.group(1))
                    .replaceAll("\\\\/", "/")
                    .replaceAll("\\\\u0026", "&")
                    .replaceAll("\\\\u003D", "=")
                    .replaceAll("\\\\u003d", "=")
                    .replaceAll("</string>", "")
                    .replaceAll("&amp;", "&");

            boolean allFound = true;

            for (String find : finds) {
                if ("auto".equalsIgnoreCase(find)) continue;
                if (!url.toLowerCase().contains(find.toLowerCase())) {
                    allFound = false;
                    break;
                }
            }

            // If we're in auto mode or finds include target formats
            if (allFound) {
                // Check if URL contains any of the target formats
                for (String format : SUPPORTED_FORMATS) {
                    if (url.toLowerCase().contains(format)) {
                        return url;
                    }
                }
                matchingUrls.add(url);
            }
        }

        // Return first matching URL if found
        if (!matchingUrls.isEmpty()) {
            return matchingUrls.get(0);
        }

// البحث عن atob(...)
        String base64Url = extractBase64Video(content);

        if (base64Url != null) {
            return base64Url;
        }

        return FALLBACK_URL;
    }
    private String extractBase64Video(String content) {

        Pattern pattern = Pattern.compile(
                "(?:window\\.)?atob\\(['\"]([^'\"]+)['\"]\\)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {

            try {

                String encoded = matcher.group(1);

                String decoded = new String(
                        Base64.decode(encoded, Base64.DEFAULT)
                );

                Log.d(TAG, "Decoded Base64 : " + decoded);

                if (isValidVideoUrl(decoded)) {
                    return decoded;
                }

            } catch (Exception e) {
                // ignore
            }
        }

        return null;
    }
    private boolean containsAuto(List<String> finds) {
        for (String f : finds) {
            String trimmed = f.trim().toLowerCase();
            if ("auto".equals(trimmed) || "web2exo:auto".equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================
    // CACHE MANAGEMENT
    // =====================================================

    public void clearCache(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteDirectory(cacheDir);
            }

            File webViewCacheDir = new File(context.getCacheDir(), "webview");
            if (webViewCacheDir.exists()) {
                deleteDirectory(webViewCacheDir);
            }

            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();

            Log.d(TAG, "All cache cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Cache clear error", e);
        }
    }

    private boolean deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    if (!deleteDirectory(new File(dir, child))) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }

    public void cleanup() {
        resetState();
        if (currentWebView != null) {
            cleanupWebViewResources(currentWebView);
        }
    }
}