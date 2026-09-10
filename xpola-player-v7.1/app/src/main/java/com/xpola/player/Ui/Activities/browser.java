package com.xpola.player.Ui.Activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
//import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.xpola.player.Ads.AdsManager;
import com.xpola.player.R;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Utils.AdBlocker;
import com.xpola.player.Utils.Chrome;
//import com.xpola.player.Utils.Constants;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Parser.Data;
import com.xpola.player.Utils.Parser.IntentParser;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;
import com.xpola.player.Utils.NpvChecker;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class browser extends AppCompatActivity {
    private WebView web;
    private ImageButton back;
    private TextView title;
    private ProgressBar progress;
    private String userAgent = "";
    private Map<String, String> headers = new HashMap<>();
    private String url = "";
    private AdsManager wAdsAdapter;
    private Prefs prefs;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        initializeLogic();
    }

    private void initializeLogic() {
        prefs = new Prefs(this);
        dialog = new AlertDialog.Builder(this).create();
        wAdsAdapter = AdsManager.getInstance(this);
        if (Utils.checkUsageAgreements(this, prefs)) {
            return;
        }
        web = findViewById(R.id.web_view);
        title = findViewById(R.id.web_title);
        back = findViewById(R.id.web_back);
        progress = findViewById(R.id.progress);
        Utils.handler.postDelayed(() -> wAdsAdapter.showAdWhenOpenPlayer(), 2000);
        Utils.handler.post(new NpvChecker(prefs, stop -> {
            if (stop) {
                Dialogs.DisableVPN(this, dialog);
                url = "";
                headers = new HashMap<>();
            }
        }));
        if (!Utils.isNetworkAvailable(this)) {
            Utils.showToast(this, getString(R.string.you_are_offline));
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        checkIntent();
        if (web != null) web.onResume();
        super.onResume();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void checkIntent() {
        Intent intent = getIntent();
        IntentParser.getData(intent, this, new IntentParser.DataCallback() {
            @Override
            public void onDataReceived(Data data) {
                headers = data.getHeaders();
                url = data.getUrl();
                String name = data.getTitle();
                userAgent = data.getUserAgent();
                if (getIntent().hasExtra("title")) {
                    name = name.isEmpty() ? Utils.objectToString(getIntent().getStringExtra("title")) : name;
                }
                title.setText(name);
                url = url.isEmpty() ? Utils.objectToString(getIntent().getData()) : url;
                userAgent = userAgent.isEmpty() ? getUserAgent() : userAgent;
                if (getIntent().hasExtra("headers"))
                    headers = headers.isEmpty() ? Utils.getMapString(Utils.objectToString(getIntent().getStringExtra("headers"))) : headers;
                headers.remove("player-type");
                back.setOnClickListener(v -> onBackPressed());
                AdBlocker.init(browser.this, () -> {
                    if (userAgent == null || userAgent.isEmpty()) {
                        userAgent = prefs.getString("default_user_agent", "");
                    }
                    if (!userAgent.isEmpty())
                        web.getSettings().setUserAgentString(userAgent);
                    if (!headers.containsKey("x-requested-with")) {
                        headers.put("x-requested-with", "com.android.chrome");
                    }
                    if (headers.containsKey("xpola-player")) {
                        String clearKey = "xpola_player" + "(" + Utils.getDeviceId(browser.this) + ")" + Utils.isCanaryInstalled(prefs.getContext());
                        headers.put("ClearKey", clearKey);
                    }
                    if (headers.containsKey("webpage")) {
                        String clearKey = "Android" + "(" + Utils.getDeviceId(browser.this) + ")" + Utils.isCanaryInstalled(prefs.getContext() );
                        headers.put("Cache-Control", clearKey);
                    }
                    if (url != null && (url.contains("inter") || url.contains("xpola"))) {
                        String clearKey = "Android" + "(" + Utils.getDeviceId(browser.this) + ")" + Utils.isCanaryInstalled(prefs.getContext());
                        headers.put("Cache-Control", clearKey);
                    }
                    web.onResume();
                    web.getSettings().setUseWideViewPort(false);
                    web.getSettings().setDomStorageEnabled(true);
                    web.getSettings().setJavaScriptEnabled(true);
                    web.setWebViewClient(new WebClient());
                    web.setWebChromeClient(new Chrome(browser.this));
                    web.loadUrl(url, headers);
                    Sec.clearCache(getApplicationContext());
                });
            }

            @Override
            public void onError(Exception e) {
                Utils.showToast(browser.this, "Error: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) {
            web.onPause();
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(browser.this, CustomIntent.RIGHT_TO_LEFT);
        release();
    }

    private void release() {
        if (web != null) {
            web.loadUrl("");
            web = null;
        }
    }

    private String getUserAgent() {
        if (getIntent().hasExtra("user-agent") && !Utils.objectToString(getIntent().getStringExtra("user-agent")).isEmpty()) {
            return Utils.objectToString(getIntent().getStringExtra("user-agent"));
        }
        return prefs.getString("default_user_agent", "").equalsIgnoreCase("") ? Utils.defaultUserAgent(this) : prefs.getString("default_user_agent", "");
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            Utils.handler.postDelayed(() -> {
                if (wAdsAdapter != null)
                    wAdsAdapter.showAd();
            }, 500);
            finish();
            release();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public class WebClient extends WebViewClient {

        private final Map<String, Boolean> loadedUrls = new HashMap<>();

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String u) {
            if (headers.containsKey("redirect") ||
                    (headers.containsKey("x-requested-with") &&
                            "com.android.browser".equals(headers.get("x-requested-with")))) {
                return true;
            }

            if (u.startsWith("intent://")) {
                try {
                    Intent intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                        return true;
                    }
                } catch (URISyntaxException e) {

                }
            } else if (url.equals(u)) {
                view.loadUrl(u, headers);
                return true;
            }

            return false;
        }


        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request) {
            boolean ad;
            String url = request.getUrl().toString();
            if (!loadedUrls.containsKey(url)) {
                ad = AdBlocker.isAd(url);
                loadedUrls.put(url, ad);
            } else {
                ad = Boolean.TRUE.equals(loadedUrls.get(url));
            }
            if (url.contains("iclickcdn") || ad || url.contains("probtraf") || url.contains("clean")) {
                ad = true;
            }
            return ad ? AdBlocker.createEmptyResource() : super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progress.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) { // التأكد من أن الخطأ على الصفحة الرئيسية فقط
                view.loadUrl("file:///android_asset/fonts/error.html");
            }
        }
    }

}
