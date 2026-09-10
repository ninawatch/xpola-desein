package com.xpola.player.Ui.Activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.xpola.player.Ads.AdsManager;
import com.xpola.player.R;
import com.xpola.player.Utils.AdBlocker;
import com.xpola.player.Utils.Chrome;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Parser.Data;
import com.xpola.player.Utils.Parser.IntentParser;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;
import com.xpola.player.Utils.NpvChecker;

public class iframe extends AppCompatActivity {
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
        // 🔒 فحص التعديل على التطبيق (اسم الحزمة)
        com.xpola.player.Sec.Pack.checkAndCloseIfTampered(this);
        setContentView(R.layout.iframe);
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

                if (getIntent().hasExtra("headers")) {
                    headers = headers.isEmpty() ? Utils.getMapString(Utils.objectToString(getIntent().getStringExtra("headers"))) : headers;
                }

                headers.remove("player-type");
                back.setOnClickListener(v -> onBackPressed());

                AdBlocker.init(iframe.this, () -> {
                    if (userAgent == null || userAgent.isEmpty()) {
                        userAgent = prefs.getString("default_user_agent", "");
                    }
                    if (!userAgent.isEmpty()) {
                        web.getSettings().setUserAgentString(userAgent);
                    }
                    if (!headers.containsKey("x-requested-with")) {
                        headers.put("x-requested-with", "com.android.chrome");
                    }
                    web.onResume();
                    web.getSettings().setUseWideViewPort(false);
                    web.getSettings().setDomStorageEnabled(true);
                    web.getSettings().setJavaScriptEnabled(true);
                    web.setWebViewClient(new WebClient());
                    web.setWebChromeClient(new Chrome(iframe.this));

                    StringBuilder formHtml = new StringBuilder();
                    formHtml.append("<html><body style=\"margin: 0; padding: 0; overflow: hidden;\">");

                    // chek if iframe wth sandbox or no
                    if (headers.containsKey("sandbox") && headers.get("sandbox").equalsIgnoreCase("false")) {
                        // iframe without sandbox
                        formHtml.append("<iframe src=\"").append(url).append("\" ")
                                .append("style=\"border: none; width: 100%; height: 100vh;\" frameborder=\"0\" allowfullscreen></iframe>");
                    } else {
                        // iframe with sandbox
                        formHtml.append("<iframe src=\"").append(url).append("\" ")
                                .append("style=\"border: none; width: 100%; height: 100vh;\" frameborder=\"0\" ")
                                .append("allowfullscreen sandbox=\"allow-scripts allow-same-origin\"></iframe>");
                    }

                    formHtml.append("</body></html>");
                    web.loadDataWithBaseURL(null, formHtml.toString(), "text/html", "UTF-8", null);

                    web.setWebViewClient(new WebViewClient() {
                        @Nullable
                        @Override
                        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                            if (request.getUrl().toString().equals(url)) {
                                OkHttpClient client = new OkHttpClient();
                                Request.Builder builder = new Request.Builder().url(request.getUrl().toString());

                                for (Map.Entry<String, String> entry : headers.entrySet()) {
                                    builder.addHeader(entry.getKey(), entry.getValue());
                                }

                                try {
                                    Response response = client.newCall(builder.build()).execute();
                                    return new WebResourceResponse(
                                            "text/html",
                                            "UTF-8",
                                            response.body().byteStream()
                                    );
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            return super.shouldInterceptRequest(view, request);
                        }
                    });
                    progress.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(Exception e) {
                Utils.showToast(iframe.this, "Error: " + e.getMessage());
            }
        });
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) {
            web.onPause();

        }
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(iframe.this, CustomIntent.RIGHT_TO_LEFT);
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
            if (wAdsAdapter != null) {
                wAdsAdapter.showAd();
            }

            new android.os.Handler().postDelayed(() -> {
                if (!web.canGoBack()) {
                    finish();
                }
            }, 1000);
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
            if (url.equals(u)) {
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
    }
}