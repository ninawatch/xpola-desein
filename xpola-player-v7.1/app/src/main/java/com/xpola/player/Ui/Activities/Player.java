package com.xpola.player.Ui.Activities;

import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_VIDEO;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.xpola.player.Sec.Fahis;
import com.xpola.player.Utils.DeviceUtils;
import com.xpola.player.Utils.EmulatorCheck;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.xpola.player.Ads.AdsManager;
import com.xpola.player.Connections.HttpRequest;
import com.xpola.player.Connections.LoadSettings;
import com.xpola.player.R;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Sec.Sec.SignatureCheck;
import com.xpola.player.Sec.AdsTamperCheck;
import com.xpola.player.Ui.Fragments.TrackSelectionDialog;
import com.xpola.player.Utils.AdBlocker;
import com.xpola.player.Utils.Chrome;
import com.xpola.player.Utils.Constants;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Floater;
import com.xpola.player.Utils.Parser.Data;
import com.xpola.player.Utils.Parser.DrmParser;
import com.xpola.player.Utils.Parser.IntentParser;
import com.xpola.player.Utils.Parser.LinksFinder;
import com.xpola.player.Utils.Parser.M3uParser;
import com.xpola.player.Utils.Parser.YacineLinks;
import com.xpola.player.Utils.PlayerSaver;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.ResumeHelper;
import com.xpola.player.Utils.Utils;
import com.xpola.player.Utils.NpvChecker;
import com.xpola.player.Utils.mimeTypes;
import com.xpola.player.Utils.ProxyEngine;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.xpola.player.Saka.CoreGuard;
public class Player extends AppCompatActivity {
    private Utils utils;
    private LinearLayout topBar;
    private ProgressBar progress;
    private ImageButton ratio, settings, btnFullScreen, pip, cast, dlna, subtitleButton, exoCopy;
    private ImageView back;
    private TextView title;
    private StyledPlayerView playerView;
    private WebView webPlayer;
    private HorizontalScrollView scrollView;
    private LinearLayout serversParent;
    private View[] views = new View[0];
    private ExoPlayer player;
    private boolean playedWebExtractor = false;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionParameters trackSelectionParameters;
    private Tracks lastSeenTracksInfo;
    private Map<String, String> headers = new HashMap<>();
    private final List<Map<String, String>> servers = new ArrayList<>();
    private String userAgent = "";
    private String videoTitle = "";
    private String url = "";

    private String type = "";
    private String license = "";
    private String scheme = "";
    private String subtitleUrl = "";
    private int lastPosition = 0,
            startItemIndex = 0;
    private long startPosition = 0;
    private int serverSelected = 0;
    private boolean isWeb = false;
    private boolean useMimeType = false;
    private boolean isShowingTrackSelectionDialog = false;
    private boolean goToFloat = false;
    private AdsManager wAdsAdapter;
    private boolean SubscripeNpv = false;
    private Prefs prefs;
    private ConsentInformation consentInformation;
    private AlertDialog dialog;
    private int random = 0;
    private long currentPosition = 0;
    private NpvChecker npvChecker;

    // متغيرات جديدة لإدارة تشغيل رابط videoload
    private Handler videoLoadHandler = new Handler();
    private boolean isVideoLoadPlaying = false;
    private String originalUrl = "";
    private Runnable videoLoadTimeoutRunnable;
    private boolean playerReleased = true;
    private static final int MAX_RETRY = 3;
    private int retryCount = 0;
    private boolean linkResolved = false;
    private boolean isTransitioningToVlc = false;
    private Handler topBarHandler = new Handler(Looper.getMainLooper());
    private Runnable hideTopBarRunnable = () -> topBar.setVisibility(View.GONE);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 💡 هذا السطر هو المفتاح لحل مشكلة انطفاء الشاشة
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 🔒 فحص التعديل على التطبيق (اسم الحزمة)
        com.xpola.player.Sec.Pack.checkAndCloseIfTampered(this);

        setContentView(R.layout.player_view);

        if (Sec.isDebuggerAttached() || AdBlocker.isHostsFileModified()) {
            finishAffinity();
            System.exit(0);
        }

        // ✅ تهيئة prefs أولاً
        prefs = new Prefs(this);

        // تحديث قيمة SubscripeNpv
        SubscripeNpv = prefs.getBoolean("stop_check_npv", false);

        initializeData();
        ConsentRequest();
        // خاص بنسخة APK
       // DeviceUtils.checkDevice(this);
    }


    private void initializeData() {
        utils = new Utils(this);
        wAdsAdapter = AdsManager.getInstance(this);
        prefs = new Prefs(this);
        if (Utils.checkUsageAgreements(this, prefs)) {
            return;
        }
        dialog = new AlertDialog.Builder(this).create();
        playerView = findViewById(R.id.player_view);
        webPlayer = findViewById(R.id.web_player);
        progress = findViewById(R.id.progress_bar);

        LinearLayout controls = playerView.findViewById(R.id.exo_basic_controls);

        topBar = findViewById(R.id.player_top_bar);
        serversParent = topBar.findViewById(R.id.servers_parent);
        scrollView = topBar.findViewById(R.id.servers_scroll_view);
        title = topBar.findViewById(R.id.title);
        back = topBar.findViewById(R.id.player_back);

        settings = controls.findViewById(R.id.player_settings);
        btnFullScreen = controls.findViewById(R.id.player_fullscreen);
        ratio = controls.findViewById(R.id.player_ratio);
        pip = controls.findViewById(R.id.player_pip);
        cast = controls.findViewById(R.id.exo_cast);
        dlna = controls.findViewById(R.id.exo_dlna);
        subtitleButton = controls.findViewById(R.id.exo_subtitle);
        exoCopy = controls.findViewById(R.id.exo_copy);
        trackSelectionParameters = new TrackSelectionParameters.Builder(this).build();
        initialize();
        LoadSettings.getSettingsJSONFile(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        Dialogs.update(this, prefs.getString("version", ""), prefs.getString("message", ""), prefs.getString("url", ""), prefs.getBoolean("forceUpdate", true));
        if (!Utils.isNetworkAvailable(this)) {
            Utils.showToast(this, getString(R.string.you_are_offline));
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        goToFloat = false;
        if (Util.SDK_INT > 23) {
            checkNewIntent();
            if (isWeb)
                webPlayer.onResume();
            if (playerView != null) {
                playerView.onResume();
            }
        }
        if (url.isEmpty()) {
            releasePlayer();
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        HideNavBar();
        goToFloat = false;
        isTransitioningToVlc = false;
        if (Util.SDK_INT <= 23 || player == null) {
            checkNewIntent();
            if (isWeb)
                webPlayer.onResume();
            if (playerView != null) {
                playerView.onResume();
            }
        }
        if (url.isEmpty()) {
            releasePlayer();
        }
        super.onResume();
    }

    private void checkNewIntent() {
        Uri incomingUri = getIntent().getData();

        if (incomingUri != null &&
                ("file".equals(incomingUri.getScheme()) || "content".equals(incomingUri.getScheme()))) {

            // تشغيل مباشر بدون IntentParser
            servers.clear();
            url = incomingUri.toString();
            title.setVisibility(View.VISIBLE);
            title.setText("Local Video");

            // تشغيل الملف مباشرة
            isWeb = false;
            headers.clear();
            type = "";
            license = "";
            scheme = "";
            setPlayer();
            return; // يمنع IntentParser من تخريب الرابط
        }
        IntentParser.getData(getIntent(), this, new IntentParser.DataCallback() {
            @Override
            public void onDataReceived(Data data) {
                try {
                    servers.clear();
                    random = Utils.getRandom(0, 5);
                    servers.addAll(data.getServers());
                    if (getIntent().hasExtra("fromFloater")) {
                        servers.clear();
                        servers.addAll(PlayerSaver.getInstance().getServers());
                        if (!servers.isEmpty()) {
                            serverSelected = PlayerSaver.getInstance().getServerSelected();
                            CreateServers();
                            selectServer(serverSelected);
                            scrollView.setVisibility(View.VISIBLE);
                            title.setVisibility(View.GONE);
                        } else {
                            scrollView.setVisibility(View.GONE);
                            title.setVisibility(View.VISIBLE);
                            userAgent = PlayerSaver.getInstance().getUserAgent();
                            headers = PlayerSaver.getInstance().getHeaders();
                            url = PlayerSaver.getInstance().getUri().toString();
                        }
                    } else if (!servers.isEmpty()) {
                        CreateServers();
                        if (IntentParser.isWebScheme(getIntent())) {
                            Utils.handler.postDelayed(() -> wAdsAdapter.showAdWhenOpenFromWeb(), 2000);
                            random = 3;
                        } else if (random == 3) {
                            Utils.handler.postDelayed(() -> wAdsAdapter.showAdWhenOpenPlayer(), 2000);
                        }
                        serverSelected = Math.max(serverSelected, PlayerSaver.getInstance().getServerSelected());
                        if (serverSelected >= servers.size()) serverSelected = 0;
                        selectServer(serverSelected);
                        scrollView.setVisibility(View.VISIBLE);
                        title.setVisibility(View.GONE);
                    } else {
                        if (IntentParser.isWebScheme(getIntent())) {
                            Utils.handler.postDelayed(() -> wAdsAdapter.showAdWhenOpenFromWeb(), 2000);
                            random = 3;
                        } else if (random == 3) {
                            Utils.handler.postDelayed(() -> wAdsAdapter.showAdWhenOpenPlayer(), 2000);
                        }
                        scrollView.setVisibility(View.GONE);
                        title.setVisibility(View.VISIBLE);
                        headers = data.getHeaders();
                        url = getIntent().hasExtra("use_parser") ? String.valueOf(getIntent().getData()) : data.getUrl();
                        license = Utils.getValueString(headers, "license", data.getDrmLicense());
                        if (!SignatureCheck.isSignatureValid(getPackageManager(), getPackageName())) {
                            license = "";
                        }
                        scheme = Utils.getValueString(headers, "scheme", data.getDrmScheme());
                        type = Utils.getValueString(headers, M3uParser.PLAYER_TYPE, "");
                        videoTitle = Utils.getValueString(headers, "title", data.getTitle());
                        if (type.isEmpty()) {
                            type = data.getPlayerType();
                        }

                        if (type.equalsIgnoreCase("vlcplayer") || (url != null && url.toLowerCase().startsWith("rtsp://"))) {
                            if (!isTransitioningToVlc) {
                                isTransitioningToVlc = true;
                                Intent intent = new Intent(Player.this, VlcPlayer.class);
                                intent.putExtra("url", url);
                                intent.putExtra("title", videoTitle);
                                intent.putExtra("headers", Utils.objectToString(headers));
                                if (!servers.isEmpty()) {
                                    intent.putExtra("servers", Utils.objectToString(servers));
                                    intent.putExtra("serverSelected", serverSelected);
                                }
                                hardResetPlayer();
                                startActivity(intent);
                                finish();
                            }
                            return;
                        }

                        userAgent = data.getUserAgent();
                        headers.remove(M3uParser.PLAYER_TYPE);
                        isWeb = type.equalsIgnoreCase("webplayer")
                                || type.equalsIgnoreCase("webplayer2");
                        if (isWeb) {
                            playedWebExtractor = true;
                        }
                        // استخراج رابط الترجمة من headers
                        if (headers.containsKey("subtitle")) {
                            subtitleUrl = Utils.getValueString(headers, "subtitle", "");
                        }

                        if (Sec.isProxyEnabled(Player.this)) {
                            url = prefs.getString("appSnfr", "file:///android_asset/fonts/offline.mp4");
                        }

                        if (IntentParser.isFilesScheme(data.getScheme()) && !Utils.isUrl(url)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    String[] mediaStorage = {READ_MEDIA_VIDEO, READ_MEDIA_AUDIO};
                                    if (ContextCompat.checkSelfPermission(Player.this, mediaStorage[0]) != PackageManager.PERMISSION_GRANTED
                                            || ContextCompat.checkSelfPermission(Player.this, mediaStorage[1]) != PackageManager.PERMISSION_GRANTED) {
                                        requestPermissions(mediaStorage, 1);
                                    } else {
                                        setPlayer();
                                    }
                                } else {
                                    if (ContextCompat.checkSelfPermission(Player.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                                    } else {
                                        setPlayer();
                                    }
                                }
                            } else {
                                setPlayer();
                            }
                        } else
                            setPlayer();
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onError(Exception e) {
                utils.showToast("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setPlayer();
            }
        } else if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setPlayer();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        releasePlayer();
        clearStartPosition();
        serverSelected = 0;
        PlayerSaver.getInstance().setServerSelected(0);
        checkNewIntent();
    }

    @Override
    public void onBackPressed() {

        if (isWeb && webPlayer.canGoBack()) {
            webPlayer.goBack();
            return;
        }

        // 🔥 احفظ الموضع أولاً قبل أي release
        saveResumePosition();

        if (random != 3) {

            releasePlayer();

            Utils.handler.postDelayed(() -> {
                if (wAdsAdapter != null)
                    wAdsAdapter.showAd();
            }, 500);

            finish();

        } else {
            releasePlayer();
            finish();
        }
    }

    @Override
    protected void onPause() {
        saveResumePosition();
        lastPosition = player != null ? (int) player.getCurrentPosition() : 0;
        if (player != null && !goToFloat) {
            releasePlayer();
        }
        if (Util.SDK_INT <= 23 && !goToFloat) {
            if (playerView != null) {
                playerView.onPause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        saveResumePosition();
        if (player != null && !goToFloat) {
            releasePlayer();
        }
        super.onStop();
    }

    private void saveResumePosition() {
        if (player != null && !player.isCurrentMediaItemLive()) {
            ResumeHelper.savePosition(this, url, player.getCurrentPosition());
        }
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(Player.this, CustomIntent.RIGHT_TO_LEFT);
    }

    protected void releasePlayer() {
        if (player != null) {
            saveResumePosition();
            player.clearMediaItems(); // removes all items and listeners, triggering STATE_IDLE cleanly instead of STATE_ENDED
            updateTrackSelectorParameters();
            updateStartPosition();
            try {
                player.setPlayWhenReady(false);
                player.clearVideoSurface();
                player.stop();
            } catch (Exception ignored) {}
            player.release();
            if (webPlayer != null)
                webPlayer.onPause();
            if (playerView != null) {
                playerView.setPlayer(null);
            }
            player = null;
        }
    }

    // ✅ دالة خاصة لإعادة ضبط حالة المشغل بالكامل لمنع الصوت المكرر
    private void hardResetPlayer() {
        try {
            if (player != null) {
                player.setPlayWhenReady(false);
                player.clearVideoSurface();
                player.clearMediaItems();
                player.stop(true);
                player.release();
            }
        } catch (Exception ignored) {}

        if (playerView != null) {
            playerView.setPlayer(null);
        }

        player = null;
        trackSelector = null;
        playerReleased = true;

        AudioManager audioManager =
                (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }

        System.gc();
    }

    private void initialize() {
        ImageButton[] images = {settings, ratio, btnFullScreen, pip, cast, dlna, subtitleButton, exoCopy};
        for (ImageButton img : images) {
            AllClicks(img);
        }
        AllClicks(back);
        playerView.setControllerVisibilityListener((StyledPlayerView.ControllerVisibilityListener) visibility -> {
            if (visibility == View.VISIBLE) {
                topBar.setVisibility(View.VISIBLE);
                ShowNavBar();
            } else {
                topBar.setVisibility(View.GONE);
               // playerView.requestFocus();
                HideNavBar();
            }
        });
    }

    private void HideNavBar() {
        getWindow().getDecorView().setSystemUiVisibility(3846);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void ShowNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void initializePlayer() {
        if (!playerReleased) {
            return; // ❌ امنع إنشاء Player جديد
        }
        playerReleased = false;
        try {
            goToFloat = false;
            playerView.setVisibility(View.VISIBLE);
            if (PlayerSaver.getInstance().Window() != null && PlayerSaver.getInstance().View() != null) {
                PlayerSaver.getInstance().Window().removeView(PlayerSaver.getInstance().View());
                PlayerSaver.getInstance().setWindow(null);
                PlayerSaver.getInstance().setView(null);
            }
            if (getIntent().hasExtra("fromFloater") && getIntent().getBooleanExtra("fromFloater", false) && PlayerSaver.getInstance().Player() != null) {
                trackSelector = PlayerSaver.getInstance().TrackSelector();
                trackSelectionParameters = PlayerSaver.getInstance().trackSelectorParameters();
                player = PlayerSaver.getInstance().Player();
                PlayerSaver.getInstance().clear();
            } else {
                trackSelector = new DefaultTrackSelector(this);
                if (userAgent.isEmpty())
                    userAgent = Utils.getValueString(headers, M3uParser.USER_AGENT, userAgent);
                if (userAgent.isEmpty())
                    userAgent = Utils.defaultUserAgent(this);
                if (!headers.containsKey("cookie")
                        || headers.get("cookie") == null
                        || !headers.get("cookie").contains("web2exo")) {

                    exoCopy.setVisibility(View.GONE);
                }
                // 👇 هنا بالضبط مكانه الصحيح
                exoCopy.setOnClickListener(v -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("video_url", url);
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(Player.this, "The link has been copied", Toast.LENGTH_SHORT).show();
                    }
                });
                if (headers.containsKey("dlna")) {
                    dlna.setVisibility(View.GONE);
                }
                // لاضافة http لروابط التي لا تتوفر عليه مع استثناء بعض الروابط التي لا تحتاجه
                if (url != null) {

                    String lower = url.toLowerCase();

                    boolean hasProtocol =
                            lower.startsWith("http://")
                                    || lower.startsWith("https://")
                                    || lower.startsWith("file://")
                                    || lower.startsWith("content://")
                                    || lower.startsWith("android.resource://")
                                    || lower.startsWith("rtmp://")
                                    || lower.startsWith("rtsp://")
                                    || lower.startsWith("udp://")
                                    || lower.startsWith("ftp://");

                    if (!hasProtocol) {
                        url = "http://" + url;
                    }
                }
                //Log.e("MY_URL", url);
                // في حالة الروت
                String rooot = prefs.getString("rooot", "");
                boolean rootStatus = Sec.isRooted();
                if (headers.containsKey("rot") && rootStatus) {
                    url = prefs.getString("rooot", "file:///android_asset/fonts/offline.mp4");
                }
                if (headers.containsKey("httpcanary") && Utils.isCanaryInstalled(this).equalsIgnoreCase("y")) {
                    url = prefs.getString("httpcanary", "file:///android_asset/fonts/offline.mp4");
                }
                if (headers.containsKey("devicecanary") && prefs.getString("appSnfr", "").equalsIgnoreCase("y")) {
                    url = prefs.getString("httpcanary", "file:///android_asset/fonts/offline.mp4");
                }
                // منع تشغيل الرابط على المحاكي
                boolean emulatorStat = EmulatorCheck.isEmulator(this);
                if (headers.containsKey("emulator") && emulatorStat) {
                    url = prefs.getString("emulator", "file:///android_asset/fonts/offline.mp4");
                }

                // منع تشغيل روابط من المحاكيات من دالة الموجودة في Sec
                if (headers.containsKey("emelator") && Sec.isEmulator()) {
                    url = prefs.getString("emulator", "file:///android_asset/fonts/offline.mp4");
                }
                // هنا في حال اصدار لم يعد شغال نضيف في Version رابط يظهر للمستخدم
                String versionStoped = prefs.getString("version_stoped", "file:///android_asset/fonts/offline.mp4");
                if (versionStoped != null && !versionStoped.isEmpty()) {
                    url = versionStoped;
                }
                // تغيير الرابط عندما لا يكون هناك السماح لـ VPN
                if (Sec.isVpnActive(this) && headers != null) {
                    boolean stopCheck = prefs.getBoolean("stop_check_npv", false);

                    if (!(headers.containsKey("vvppnn") || stopCheck)) {
                        url = prefs.getString("url_check_npv", "file:///android_asset/fonts/offline.mp4");
                    }
                }
                if (AdsTamperCheck.isAdmobRemoved(this)
                        && "true".equals(prefs.getString("admobload", ""))) {

                    url = prefs.getString("notconnection", "file:///android_asset/fonts/internet.mp4");
                }
                // رسالة تحذيرية لتطبيقات المعدلة
                String appverifs = prefs.getString("appverifs", "file:///android_asset/fonts/offline.mp4");
                if (appverifs != null && !appverifs.isEmpty()) {
                    url = appverifs;
                }
                // Code start Play Proxy
                if (url.startsWith("http://127.0.0.1:63000")) {
                    // ابحث عن ProxyEngine.start(this); واستبدله بـ:
                    ProxyEngine.start(this, new ProxyEngine.OnProxyReadyListener() {
                        @Override
                        public void onReady() {
                            runOnUiThread(() -> {
                                Log.d("XPOLA", "Proxy is Ready, initializing player...");
                                // استدعاء دالة تهيئة المشغل بعد تأكدنا من عمل البروكسي
                                initializePlayer();
                            });
                        }

                        @Override
                        public void onFailure() {
                            runOnUiThread(() -> {
                                url = prefs.getString("proxyxpola_url", "file:///android_asset/fonts/internet.mp4");
                            });
                        }
                    });
                }
                // End Start Proxy
                //  منع تفعيل VPN  ثاني
                String gourdvvnn_url = prefs.getString("gouardvvnn_url", "file:///android_asset/fonts/offline.mp4");
                switch (CoreGuard.state(this)) {

                    case 1:
                        url = !headers.containsKey("vvppnn")
                                ? gourdvvnn_url
                                : url;
                        break;

                    default:
                        break;
                }
               // Log.d("VIDEO_URL", url);
               // اغلاق التطبيق و فتح المتجر بعد 60 ثانية من اكتشاف تطبيق معدل
                if (!Fahis.isValid(this)) {

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        String packageName = getPackageName();

                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=" + packageName));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }

                        // إغلاق التطبيق نهائيًا
                        finishAffinity();

                    }, 60000); // 60 ثانية = 60000 مللي ثانية
                }
                // فحص التلاعب رابط القاعدة البينات
                String loadSetting = prefs.getString("loadsetting", "");
                if (loadSetting == null || !loadSetting.equals("true")) {
                    url = "file:///android_asset/fonts/internet.mp4";
                }
                // delete header for settings as httpcanary root
                String[] keysToRemove = {"irot", "ihttpcanary", "idevicecanary"};
                for (String key : keysToRemove) {
                    headers.remove(key);
                }
                if (headers.containsKey("cast")) {
                    cast.setVisibility(View.GONE);
                }
                if (headers.containsKey("dlna")) {
                    dlna.setVisibility(View.GONE);
                }
                Utils.trustAllCerts();
                headers.remove(M3uParser.USER_AGENT);
                HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                        .setUserAgent(userAgent)
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(headers);

                DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);
                DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);
                if (Utils.haveScheme(scheme) && !license.isEmpty()) {
                    mediaSourceFactory.setDrmSessionManagerProvider(mediaItem -> DrmParser.getSessionManager(license, scheme, dataSourceFactory));
                }
                DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

                player = new ExoPlayer.Builder(this)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .setTrackSelector(trackSelector)
                        .setRenderersFactory(renderersFactory)
                        .setSeekBackIncrementMs(10000)
                        .setSeekForwardIncrementMs(10000)
                        .setReleaseTimeoutMs(15000)
                        .setBandwidthMeter(new DefaultBandwidthMeter.Builder(this).build()).build();

                MediaItem mediaItem = createMediaItemWithSubtitle();
                player.setMediaItem(mediaItem);
                player.setTrackSelectionParameters(trackSelectionParameters == null ? trackSelectionParameters = new TrackSelectionParameters.Builder(this).build() : trackSelectionParameters);
            }
            player.setAudioAttributes(AudioAttributes.DEFAULT, true);
            player.addListener(new PlayerEvents());
            player.addAnalyticsListener(new EventLogger());
            playerView.setPlayer(player);

            // Adjust subtitle position
            if (playerView.getSubtitleView() != null) {
                float density = getResources().getDisplayMetrics().density;
                int bottomMargin = (int) (16 * density); // 16dp
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) playerView.getSubtitleView().getLayoutParams();
                params.bottomMargin = bottomMargin;
                playerView.getSubtitleView().setLayoutParams(params);
            }

            playerView.setKeepScreenOn(true);
            player.setPlayWhenReady(true);
            playerView.requestFocus();
            updateStartPosition();
            boolean haveStartPosition = startItemIndex != C.INDEX_UNSET;
            if (haveStartPosition) {
                player.seekTo(startItemIndex, startPosition);
            }
            if (lastPosition > 0) player.seekTo(lastPosition);
            player.prepare();
        } catch (Exception ignored) {
        }
    }

    private MediaItem createMediaItemWithSubtitle() {
        MediaItem.Builder mediaItemBuilder = MediaItem.fromUri(url).buildUpon();

        if (useMimeType) {
            mediaItemBuilder.setMimeType(mimeTypes.getFrom(url));
        }

        // إضافة الترجمة إذا كانت موجودة
        if (!subtitleUrl.isEmpty()) {
            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(getSubtitleMimeType(subtitleUrl))
                    .setLanguage("ar") // اللغة الافتراضية
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build();

            mediaItemBuilder.setSubtitleConfigurations(List.of(subtitleConfig));
        }

        return mediaItemBuilder.build();
    }

    private String getSubtitleMimeType(String url) {
        if (url.endsWith(".vtt")) {
            return MimeTypes.TEXT_VTT;
        } else if (url.endsWith(".srt")) {
            return MimeTypes.APPLICATION_SUBRIP;
        } else if (url.endsWith(".ssa") || url.endsWith(".ass")) {
            return MimeTypes.TEXT_SSA;
        } else if (url.endsWith(".ttml")) {
            return MimeTypes.APPLICATION_TTML;
        } else {
            return MimeTypes.TEXT_VTT; // افتراضي
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startItemIndex = player.getCurrentMediaItemIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    protected void clearStartPosition() {
        startItemIndex = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private void updateTrackSelectorParameters() {
        if (player != null) {
            trackSelectionParameters = player.getTrackSelectionParameters();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
    }

    private void updateButtonVisibility() {
        // زر الإعدادات
        boolean enableSettings = player != null && TrackSelectionDialog.willHaveContent(player);
        settings.setEnabled(enableSettings);

        if (enableSettings) {
            // الزر مفعّل: لونه أبيض وواضح
            settings.clearColorFilter();
            settings.setColorFilter(Color.WHITE);
            settings.setAlpha(1.0f);
        } else {
            // الزر غير مفعّل: رمادي باهت
            settings.setColorFilter(Color.GRAY);
            settings.setAlpha(0.3f);
        }

        // زر الترجمة
        boolean enableSubtitleButton = player != null && !player.isCurrentMediaItemLive();
        subtitleButton.setEnabled(enableSubtitleButton);

        if (enableSubtitleButton) {
            // الزر مفعّل: لونه أبيض وواضح
            subtitleButton.clearColorFilter();
            subtitleButton.setColorFilter(Color.WHITE);
            subtitleButton.setAlpha(1.0f);
        } else {
            // الزر غير مفعّل: رمادي باهت
            subtitleButton.setColorFilter(Color.GRAY);
            subtitleButton.setAlpha(0.3f);
        }
    }

    private void AllClicks(@NonNull View v) {
        v.setOnClickListener(v1 -> {
            if (v1 == settings) {
                try {
                    if (!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(player)) {
                        isShowingTrackSelectionDialog = true;
                        TrackSelectionDialog trackSelectionDialog = TrackSelectionDialog.createForPlayer(player, dismissedDialog -> isShowingTrackSelectionDialog = false);
                        trackSelectionDialog.show(getSupportFragmentManager(), null);
                    }
                } catch (Exception ignored) {
                }
            } else if (v1 == subtitleButton) {
                showSubtitleDialog();
            } else if (v1 == btnFullScreen) {
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                } else if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            } else if (v1 == ratio) {
                if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
                } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH) {
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT);
                } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT) {
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                }
            } else if (v1 == pip) {
                showMiniPlayer();
            } else if (v1 == back) {
                onBackPressed();
            } else if (v1 == cast) {
                Cast();
            } else if (v1 == dlna) {
                try {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_CAST_SETTINGS);
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void Cast() {
        try {
            if (Utils.isInstalled(this, Constants.pack)) {
                Intent intent = new Intent().setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "video/*");
                intent.putExtra("secure_uri", true);
                intent.putExtra("User-Agent", userAgent);
                intent.putExtra("headers", IntentParser.getHeaders(headers));
                intent.setPackage(Constants.pack);
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=" + Constants.pack)));
            }
        } catch (Exception ignored) {
        }
    }

    private void showMiniPlayer() {
        try {
            Floater floater = new Floater(this);
            if (canFloat() && player != null) {
                goToFloat = true;
                PlayerSaver.getInstance().setPlayer(player);
                PlayerSaver.getInstance().setTrackSelector(trackSelector);
                PlayerSaver.getInstance().setParameters(trackSelectionParameters);
                PlayerSaver.getInstance().setHeaders(headers);
                PlayerSaver.getInstance().setUserAgent(userAgent);
                PlayerSaver.getInstance().setUri(Uri.parse(url));
                PlayerSaver.getInstance().setServers(servers);
                PlayerSaver.getInstance().setServerSelected(serverSelected);
                if (PlayerSaver.getInstance().Window() != null && PlayerSaver.getInstance().View() != null) {
                    PlayerSaver.getInstance().Window().removeView(PlayerSaver.getInstance().View());
                    PlayerSaver.getInstance().setWindow(null);
                    PlayerSaver.getInstance().setView(null);
                }
                floater.show();
                finish();
            }
        } catch (Exception e) {
            utils.showToast(e.getMessage());
        }
    }

    private boolean canFloat() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    goToFloat = true;
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {
                }
                return false;
            } else return true;
        } else return true;
    }

    private class PlayerEvents implements ExoPlayer.Listener {

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {

            // روابط غير HTTP → توقف
            if (url == null || !url.startsWith("http")) {
                Utils.showToast(Player.this, "Unsupported video link.");
                return;
            }

            // بث مباشر BEHIND WINDOW
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                retryCount = 0;
                try {
                    player.seekToDefaultPosition();
                    player.prepare();
                    player.setPlayWhenReady(true);
                } catch (Exception ignored) {}
                return;
            }

            // المحاولة الأولى: تفعيل استخدام mimeType
            if (!useMimeType) {
                useMimeType = true;
                retryCount = 0;
                hardResetPlayer();
                initializePlayer();
                return;
            }

            // إعادة محاولة التشغيل 3 مرات
            if (retryCount < MAX_RETRY) {
                retryCount++;

                hardResetPlayer();   // ✅ هنا
                initializePlayer();
                return;
            }

            // فشل كل المحاولات → الانتقال للسيرفر التالي إن وجد
            retryCount = 0;
            useMimeType = false;

            if (!servers.isEmpty() && servers.size() > 1) {
                int old = serverSelected;
                serverSelected++;

                if (serverSelected >= servers.size())
                    serverSelected = 0;

                // تم الانتقال إلى سيرفر مختلف
                if (serverSelected != old) {
                    Utils.showToast(Player.this, "Switching to backup server...");
                    selectServer(serverSelected);
                    return;
                }
            }

            // لا يوجد سيرفر آخر
            Utils.showToast(Player.this, "Cannot play this video.");
        }



        @Override
        // هذا الكود لإصلاح مشكل عدم التقدم في روابط MPD فقط (مع تجاهل M3U8)
        public void onPlaybackStateChanged(int state) {
            if (player == null || progress == null)
                return;

            // 1. التحكم في الـ ProgressBar (بدون تكرار)
            if (state == ExoPlayer.STATE_BUFFERING) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);

                // 2. منطق البث المباشر - مصحح ومقيد على MPD فقط
                if (state == ExoPlayer.STATE_READY && player.isCurrentMediaItemLive()) {
                    // ⚡ التحقق من نوع الرابط (فقط MPD)
                    MediaItem mediaItem = player.getCurrentMediaItem();
                    if (mediaItem != null && mediaItem.localConfiguration != null) {
                        Uri uri = mediaItem.localConfiguration.uri;
                        if (uri != null && uri.toString().toLowerCase().endsWith(".mpd")) {
                            // ⚡ استخدام getCurrentLiveOffset() بدلاً من getDuration()
                            long liveOffset = player.getCurrentLiveOffset();
                            // ⚡ إذا كان التأخير أكثر من 59 دقيقة (3540000 مللي ثانية)
                            if (liveOffset > 3540000) {
                                // الانتقال التلقائي للنقطة الحية
                                player.seekToDefaultPosition();
                                // إشعار للمستخدم (اختياري)
                                runOnUiThread(() -> {
                                    Toast.makeText(Player.this,
                                            "Switched to live broadcast",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                }
            }

            // 3. التعامل مع نهاية الفيديو
            if (state == ExoPlayer.STATE_ENDED) {
                if (player != null &&
                        player.getDuration() > 0 &&
                        player.getCurrentPosition() >= player.getDuration() - 2000) {

                    ResumeHelper.clearPosition(Player.this, url);
                }
            }

            if (state == ExoPlayer.STATE_READY) {
                checkResumePlayback();
            }

            updateButtonVisibility();
            updateStartPosition();
        }

        private boolean resumeChecked = false;

        private void checkResumePlayback() {
            if (resumeChecked || player == null || player.isCurrentMediaItemLive()) {
                return;
            }
            resumeChecked = true;

            long savedPosition = ResumeHelper.getPosition(Player.this, url);
            if (savedPosition > 5000 && (player.getDuration() <= 0 || savedPosition < player.getDuration() - 5000)) {
                player.setPlayWhenReady(false); // إيقاف مؤقت لإظهار الحوار

                android.app.Dialog resumeDialog = new android.app.Dialog(Player.this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
                resumeDialog.setContentView(R.layout.dialog_resume_playback);
                resumeDialog.setCancelable(false);
                if (resumeDialog.getWindow() != null) {
                    resumeDialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    resumeDialog.getWindow().setDimAmount(0.6f);
                }

                // To maintain immersive mode without showing status/navigation bar
                if (resumeDialog.getWindow() != null) {
                    resumeDialog.getWindow().setFlags(
                            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    );
                }

                android.widget.Button btnResume = resumeDialog.findViewById(R.id.btn_resume);
                android.widget.Button btnStartOver = resumeDialog.findViewById(R.id.btn_start_over);

                btnResume.setOnClickListener(v -> {
                    resumeDialog.dismiss();
                    player.seekTo(savedPosition);
                    player.setPlayWhenReady(true);
                });

                btnStartOver.setOnClickListener(v -> {
                    resumeDialog.dismiss();
                    player.seekTo(0);
                    player.setPlayWhenReady(true);
                });

                resumeDialog.show();

                // Hide system UI again for the dialog's window
                if (resumeDialog.getWindow() != null) {
                    resumeDialog.getWindow().getDecorView().setSystemUiVisibility(
                            Player.this.getWindow().getDecorView().getSystemUiVisibility()
                    );
                    resumeDialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                }
            }
        }
        // هذا الكود قبل اضاف تقدم mpd     
       /* public void onPlaybackStateChanged(int state) {

            if (player == null || progress == null)
                return;

            if (state == ExoPlayer.STATE_BUFFERING) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }

            // منع إعادة تشغيل الفيديو عند انتهائه
            if (state == ExoPlayer.STATE_ENDED) {
                // لا نعيد تشغيل الفيديو
            }

            updateButtonVisibility();
            updateStartPosition();
        }*/

        @Override
        public void onTracksChanged(@NonNull Tracks tracks) {

            updateButtonVisibility();
            if (tracks == lastSeenTracksInfo)
                return;

            if (tracks.containsType(C.TRACK_TYPE_VIDEO) && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO))
                utils.showToast("Unsupported video format");

            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                    && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO)) {

                // إذا وجد الهيدر novlc لا يتم التحويل إلى VLC
                if (headers.containsKey("novlc")) {
                    utils.showToast("Audio format not supported on this device");
                    return;
                }

                try {
                    if (!isTransitioningToVlc) {
                        isTransitioningToVlc = true;

                        Intent intent = new Intent(Player.this, VlcPlayer.class);
                        intent.putExtra("url", url);
                        intent.putExtra("title", videoTitle);
                        intent.putExtra("headers", Utils.objectToString(headers));

                        if (!servers.isEmpty()) {
                            intent.putExtra("servers", Utils.objectToString(servers));
                            intent.putExtra("serverSelected", serverSelected);
                        }

                        hardResetPlayer();
                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e) {
                    utils.showToast("Audio format not supported on this device");
                }
            }

            lastSeenTracksInfo = tracks;
        }
    }

    private void setPlayer() {
        performVpnCheck();
        title.setText(videoTitle);
        setappLogo();
        // to delete cache
        Sec.clearCache(getApplicationContext());
        url = url.replaceAll("(https?://)+", "$1");
        // convert referer to origin - start
        if (headers.containsKey("referer") && !headers.containsKey("origin")) {
            String referer = Utils.getValueString(headers, "referer", "");
            if (!referer.isEmpty()) {
                try {
                    URI uri = new URI(referer);
                    if (uri.getScheme() != null && uri.getHost() != null) {
                        String origin = uri.getScheme() + "://" + uri.getHost();
                        if (uri.getPort() != -1) {
                            origin += ":" + uri.getPort();
                        }
                        if (SignatureCheck.isSignatureValid(getPackageManager(), getPackageName())) {
                            headers.put("Origin", origin);
                        }
                    }
                } catch (URISyntaxException e) {
                    // تجاهل
                }
            }
        }

        if (headers.containsKey("auth") && !Utils.getValueString(headers, "auth", "").isEmpty()) {
            progress.setVisibility(View.VISIBLE);
            new Thread(() -> {
                String authUrl = headers.get("auth");
                if (authUrl != null && !authUrl.isEmpty()) {
                    Map<String, String> authHeaders = new HashMap<>();
                    if (headers.containsKey("user-agent")) {
                        authHeaders.put("user-agent", headers.get("user-agent"));
                    }
                    if (headers.containsKey("referer")) {
                        String referer = headers.get("referer");

                        if (SignatureCheck.isSignatureValid(getPackageManager(), getPackageName())) {
                            authHeaders.put("referer", referer);
                        }
                        try {
                            URI uri = new URI(referer);
                            String origin = uri.getScheme() + "://" + uri.getHost();
                            if (uri.getPort() != -1) {
                                origin += ":" + uri.getPort();
                            }
                            authHeaders.put("Origin", origin);
                        } catch (URISyntaxException e) {
                            // Ignore
                        }
                    }
                    HttpRequest.get(authUrl, authHeaders).String();
                }
                runOnUiThread(this::proceedWithPlayerInitialization);
            }).start();
        } else {
            proceedWithPlayerInitialization();
        }
    }

    private void proceedWithPlayerInitialization() {
        // التحقق من وجود رابط videoload وتشغيله أولاً
        if (headers.containsKey("videoload")) {
            String videoLoadUrl = headers.get("videoload");
            if (videoLoadUrl != null && !videoLoadUrl.isEmpty() && URLUtil.isValidUrl(videoLoadUrl)) {
                playVideoLoadFirst(videoLoadUrl);
                return;
            }
        }

        // إذا لم يكن هناك رابط videoload، تابع التشغيل الطبيعي
        continueWithOriginalUrl();
    }

    /**
     * تشغيل رابط videoload أولاً لمدة أقصاها 15 ثانية
     */
    private void playVideoLoadFirst(String videoLoadUrl) {
        // ✅ مهم: تأكد أنه لا يوجد Player قديم شغال قبل تشغيل videoload
        hardResetPlayer();

        isVideoLoadPlaying = true;
        originalUrl = url; // حفظ الرابط الأصلي
        url = videoLoadUrl; // تعيين رابط videoload مؤقتاً

        Utils.showToast(this, "Loading video...");

        // إعداد timeout لتشغيل videoload لمدة 15 ثانية كحد أقصى
        videoLoadTimeoutRunnable = () -> {
            if (isVideoLoadPlaying) {
                switchToOriginalUrl();
            }
        };

        videoLoadHandler.postDelayed(videoLoadTimeoutRunnable, 15000); // 15 ثانية

        // متابعة التشغيل الطبيعي مع رابط videoload
        if (isWeb) {
            setupWebPlayer();
        } else {
            initializePlayerWithVideoLoad();
        }
    }

    /**
     * تهيئة المشغل مع رابط videoload
     */
    private void initializePlayerWithVideoLoad() {
        try {
            goToFloat = false;
            playerView.setVisibility(View.VISIBLE);

            if (trackSelector == null) {
                trackSelector = new DefaultTrackSelector(this);
            }

            if (userAgent.isEmpty())
                userAgent = Utils.getValueString(headers, M3uParser.USER_AGENT, userAgent);
            if (userAgent.isEmpty())
                userAgent = Utils.defaultUserAgent(this);
              Utils.trustAllCerts();
            headers.remove(M3uParser.USER_AGENT);
            HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent(userAgent)
                    .setAllowCrossProtocolRedirects(true)
                    .setDefaultRequestProperties(headers);
            DefaultRenderersFactory renderersFactory =
                    new DefaultRenderersFactory(this)
                            .setExtensionRendererMode(
                                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                            );
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);
            if (Utils.haveScheme(scheme) && !license.isEmpty()) {
                mediaSourceFactory.setDrmSessionManagerProvider(mediaItem -> DrmParser.getSessionManager(license, scheme, dataSourceFactory));
            }

            player = new ExoPlayer.Builder(this)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setTrackSelector(trackSelector)
                    .setRenderersFactory(renderersFactory)
                    .setSeekBackIncrementMs(10000)
                    .setSeekForwardIncrementMs(10000)
                    .setReleaseTimeoutMs(15000)
                    .setBandwidthMeter(new DefaultBandwidthMeter.Builder(this).build()).build();

            MediaItem mediaItem = createMediaItemWithSubtitle();
            player.setMediaItem(mediaItem);
            player.setTrackSelectionParameters(trackSelectionParameters == null ? trackSelectionParameters = new TrackSelectionParameters.Builder(this).build() : trackSelectionParameters);

            player.setAudioAttributes(AudioAttributes.DEFAULT, true);
            player.addListener(new VideoLoadPlayerEvents());
            player.addAnalyticsListener(new EventLogger());
            playerView.setPlayer(player);

            // Adjust subtitle position
            if (playerView.getSubtitleView() != null) {
                float density = getResources().getDisplayMetrics().density;
                int bottomMargin = (int) (16 * density); // 16dp
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) playerView.getSubtitleView().getLayoutParams();
                params.bottomMargin = bottomMargin;
                playerView.getSubtitleView().setLayoutParams(params);
            }

            playerView.setKeepScreenOn(true);
            player.setPlayWhenReady(true);
            playerView.requestFocus();
            updateStartPosition();
            boolean haveStartPosition = startItemIndex != C.INDEX_UNSET;
            if (haveStartPosition) {
                player.seekTo(startItemIndex, startPosition);
            }
            if (lastPosition > 0) player.seekTo(lastPosition);
            player.prepare();
        } catch (Exception e) {
            switchToOriginalUrl();
        }

    }

    /**
     * الانتقال إلى الرابط الأصلي بعد انتهاء مدة videoload
     */
    private void switchToOriginalUrl() {
        if (!isVideoLoadPlaying) {
            return; // إذا لم نكن في وضع videoload، لا تفعل شيئاً
        }

        isVideoLoadPlaying = false;
        url = originalUrl; // استعادة الرابط الأصلي
        videoLoadHandler.removeCallbacks(videoLoadTimeoutRunnable); // إلغاء الـ timeout

        Utils.showToast(this, "Playing main video...");

        // ✅ تأكد من إيقاف مشغل videoload قبل الانتقال للرابط الأصلي
        hardResetPlayer();
        continueWithOriginalUrl();
    }

    /**
     * متابعة التشغيل مع الرابط الأصلي
     */
    private void continueWithOriginalUrl() {
        // 🔥 Reset كامل مرة واحدة بعد Web / Yacine / web2exo
        if (playedWebExtractor && !isWeb) {
            playedWebExtractor = false;
            fullResetAfterWebExtractor();
        }
        if (isWeb) {
            setupWebPlayer();
            return;
        }

        userAgent = Utils.getValueString(headers, M3uParser.USER_AGENT, userAgent);
        if (type.equalsIgnoreCase("browser")) {
            startActivity(new Intent(this, browser.class)
                    .setData(Uri.parse(url))
                    .putExtra("user-agent", userAgent)
                    .putExtra("headers", Utils.objectToString(headers))
                    .putExtra("title", videoTitle));
            finish();
            return;
        }
        if (type.equalsIgnoreCase("vlcplayer") || (url != null && url.toLowerCase().startsWith("rtsp://"))) {
            if (!isTransitioningToVlc) {
                isTransitioningToVlc = true;
                Intent intent = new Intent(this, VlcPlayer.class)
                        .putExtra("url", url)
                        .putExtra("title", videoTitle)
                        .putExtra("headers", Utils.objectToString(headers));
                if (!servers.isEmpty()) {
                    intent.putExtra("servers", Utils.objectToString(servers));
                    intent.putExtra("serverSelected", serverSelected);
                }
                hardResetPlayer();
                startActivity(intent);
                finish();
            }
            return;
        }
        if (type.equalsIgnoreCase("external")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            finish();
            return;
        }
        if (type.equalsIgnoreCase("iframe")) {
            startActivity(new Intent(this, iframe.class)
                    .setData(Uri.parse(url))
                    .putExtra("user-agent", userAgent)
                    .putExtra("headers", Utils.objectToString(headers))
                    .putExtra("title", videoTitle));
            finish();
            return;
        }
        if (type.equalsIgnoreCase("m3u")) {
            startActivity(new Intent(this, m3Player.class)
                    .setData(Uri.parse(url))
                    .putExtra("title", videoTitle)
                    .putExtra("headers", Utils.objectToString(headers))
                    .putExtra("isWeb", type)
                    .putExtra("user-agent", userAgent));
            finish();
            return;
        }
        webPlayer.setVisibility(View.GONE);
        playerView.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);

        // ✅ إعادة ضبط المشغل قبل أي تشغيل جديد لمنع الصوت المكرر
        // hardResetPlayer();
// تم التعديل عليه في اصدار 6.8 لاضافة Web2exo التلقائية
        linkResolved = false;
        if (headers.containsKey("find")) {

            String finds = Utils.getValueString(headers, "find", "");
            headers.remove("find");

            LinksFinder finder = LinksFinder.getNewInstance();

            if (finds.toLowerCase().contains("auto")) {

                // 🔥 AUTO MODE (WebView interception)
                finder.findLinkAuto(
                        this,
                        webPlayer,   // ✅ WebView الموجود أصلاً
                        url,
                        userAgent,
                        headers,
                        (newUrl) -> {

                            // 🔒 حماية نهائية: اقبل أول رابط فقط
                            if (linkResolved) {
                                Log.d("Player", "Link ignored (already resolved): " + newUrl);
                                return;
                            }

                            linkResolved = true;

                            Log.d("Player", "Final video URL accepted: " + newUrl);

                            this.url = newUrl;

                            if (headers.containsKey("SSLx")) {
                                this.url = newUrl.replace("https", "http");
                            }

                            hardResetPlayer();
                            initializePlayer();
                        }

                );

            } else {

                // 🔁 الطريقة القديمة (HTML parsing)
                finder.findLink(
                        url,
                        finds,
                        userAgent,
                        headers,
                        (newUrl) -> {
                            this.url = newUrl;
                            if (headers.containsKey("SSLx")) {
                                this.url = newUrl.replace("https", "http");
                            }
                            hardResetPlayer();
                            initializePlayer();
                        }
                );
            }
        } else if (url.contains(prefs.getString("yacine_url", Constants.YC_URL_CONTAINS)) ||
                url.contains(prefs.getString("yacine_url_2", Constants.YC_URL_CONTAINS))) {
            playedWebExtractor = true;
            YacineLinks.getInstance(url, userAgent, headers, prefs.getString("yacine_key", Constants.YC_KEY)).get((newUrl, newHeaders) -> {
                userAgent = "";
                headers.clear();
                headers.putAll(newHeaders);
                userAgent = Utils.getValueString(headers, M3uParser.USER_AGENT, userAgent);
                if (newUrl == null || newUrl.isEmpty()) {
                    url = "file:///android_asset/fonts/offline.mp4";
                } else {
                    url = newUrl;
                }
                if (!SignatureCheck.isSignatureValid(getPackageManager(), getPackageName())) {
                    //  url = "file:///android_asset/fonts/offline.mp4";
                }
                // ✅ هنا بالذات مع Yacine نحرص على إيقاف أي Player سابق
                hardResetPlayer();
                initializePlayer();
            });
        } else {
            // ✅ هنا نقرر: smartPlayUrl أم initializePlayer
            smartPlayUrl(url);
        }
    }

    /**
     * فئة Listener مخصصة لمراقبة تشغيل رابط videoload
     */
    private class VideoLoadPlayerEvents implements ExoPlayer.Listener {

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            switchToOriginalUrl();
        }

        @Override
        public void onPlaybackStateChanged(int state) {

            if (player == null || progress == null)
                return;

            if (state == ExoPlayer.STATE_BUFFERING) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }

            // انتهى فيديو التحميل مبكرًا
            if (state == ExoPlayer.STATE_ENDED && isVideoLoadPlaying) {
                switchToOriginalUrl();
            }

            updateButtonVisibility();
            updateStartPosition();
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupWebPlayer() {
        webPlayer.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
        webPlayer.onResume();
        AdBlocker.init(this, () -> {
            if (userAgent == null || userAgent.isEmpty()) {
                userAgent = prefs.getString("default_user_agent", "");
            }
            if (!userAgent.isEmpty())
                webPlayer.getSettings().setUserAgentString(userAgent);
            if (!headers.containsKey("x-requested-with")) {
                headers.put("x-requested-with", "com.android.chrome");
            }
            webPlayer.onResume();
            webPlayer.getSettings().setUseWideViewPort(false);
            webPlayer.getSettings().setDomStorageEnabled(true);
            webPlayer.getSettings().setJavaScriptEnabled(true);

            // تعديل WebViewClient لمنع redirect
            webPlayer.setWebViewClient(new WebClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (headers.containsKey("redirect") ||
                            (headers.containsKey("x-requested-with") &&
                                    "com.android.browser".equals(headers.get("x-requested-with")))) {
                        // منع إعادة التوجيه
                        return true;
                    }
                    return super.shouldOverrideUrlLoading(view, request);
                }
            });

            webPlayer.setWebChromeClient(new Chrome(this));
            webPlayer.loadUrl(url, headers);
            webPlayer.setOnTouchListener((v, m) -> {

                topBar.setVisibility(View.VISIBLE);

                // إلغاء أي إخفاء سابق
                topBarHandler.removeCallbacks(hideTopBarRunnable);

                // إخفاء بعد 4 ثواني
                topBarHandler.postDelayed(hideTopBarRunnable, 8000);

                return false;
            });
        });
    }

    public class WebClient extends WebViewClient {

        private final Map<String, Boolean> loadedUrls = new HashMap<>();

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // إذا كان الرابط يحتوي على "intent"، سيتم التعامل معه تلقائيًا
            if (url.startsWith("intent:")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            if (type.equalsIgnoreCase("webplayer2") && this.loadedUrls.equals(url)) {
                view.loadUrl(url, headers);
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
            topBar.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            topBar.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                view.loadUrl("file:///android_asset/fonts/error.html");
            }
        }
    }

    private void CreateServers() {
        serversParent.removeAllViews();
        views = new View[servers.size()];
        int i = 0;
        for (Map<String, String> server : servers) {
            if (server != null) {
                @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.server_item, null);
                CardView card = view.findViewById(R.id.server_parent);
                card.setFocusable(true);
              //  card.setFocusableInTouchMode(true);
                TextView text = view.findViewById(R.id.server_name);
                views[i] = card;
                Utils.RadiusAndStroke(card, 360, 0, Utils.getColor(getResources(), R.color.server_inactive_color), Utils.getColor(getResources(), R.color.server_inactive_color));
                text.setText(Utils.getValueString(server, M3uParser.NAME, ""));
                int finalI = i;
                card.setOnClickListener(view1 -> selectServer(finalI));
                serversParent.addView(view);
                i++;
            }
        }
    }

    private void selectServer(int at) {
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view != null) {
                Utils.RadiusAndStroke(view, 360, 0, Utils.getColor(getResources(), R.color.server_inactive_color), Utils.getColor(getResources(), R.color.server_inactive_color));
                if (i == at) {
                    serverSelected = at;
                    if (servers.size() > at) {
                        Map<String, String> server = servers.get(at);
                        headers = Utils.getMapString(Utils.getValueString(server, M3uParser.HEADERS, ""));
                        url = Utils.getValueString(server, M3uParser.URL, "file:///android_asset/fonts/offline.mp4");

                        if (!Utils.getValueString(server, M3uParser.DRM_LICENSE, "").isEmpty()) {
                            license = Utils.getValueString(server, M3uParser.DRM_LICENSE, "");
                        }
                        if (!SignatureCheck.isSignatureValid(getPackageManager(), getPackageName())) {
                            license = "";
                        }

                        if (!Utils.getValueString(server, M3uParser.DRM_SCHEME, "").isEmpty()) {
                            scheme = Utils.getValueString(server, M3uParser.DRM_SCHEME, "");
                        }

                        type = Utils.getValueString(headers, M3uParser.PLAYER_TYPE, "");
                        userAgent = Utils.getValueString(headers, M3uParser.USER_AGENT, "");

                        // استخراج رابط الترجمة من السيرفر
                        subtitleUrl = "";
                        if (headers.containsKey("subtitle")) {
                            subtitleUrl = Utils.getValueString(headers, "subtitle", "");
                        }

                        headers.remove(M3uParser.PLAYER_TYPE);
                        headers.remove(M3uParser.USER_AGENT);
                        headers.remove(M3uParser.NAME);
                        isWeb = type.equalsIgnoreCase("webplayer")
                                || type.equalsIgnoreCase("webplayer2");
                        Utils.RadiusAndStroke(view, 360, 3, Utils.getColor(getResources(), R.color.server_inactive_color), Utils.getColor(getResources(), R.color.server_active_color));

                        // ✅ عند تغيير السيرفر تأكد من إيقاف أي Player قديم
                        hardResetPlayer();
                        setPlayer();
                    }
                }
            }
        }
    }

    private void ConsentRequest() {
        String appId = prefs.getString("adAppId", "");
        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false);
        if (!appId.isEmpty()) {
            paramsBuilder.setAdMobAppId(appId);
        }
        ConsentRequestParameters params = paramsBuilder.build();
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(this, params, () -> {
            if (consentInformation.isConsentFormAvailable()) {
                loadForm();
            }
        }, formError -> {
            // Handle error
        });
    }

    private void loadForm() {
        UserMessagingPlatform.loadConsentForm(this, consentForm -> {
            if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                consentForm.show(this, formError -> {
                    consentInformation.getConsentStatus();
                    loadForm();
                });
            }
        }, formError -> {
            // Handle error
        });
    }

    private void setappLogo() {
        String imageUrl1 = Utils.getValueString(headers, "applogotl", "");
        String imageUrl2 = Utils.getValueString(headers, "applogo", "");
        String imageUrl3 = Utils.getValueString(headers, "applogobr", "");
        String imageUrl4 = Utils.getValueString(headers, "applogobl", "");
        String appLink = Utils.getValueString(headers, "applink", "");

        String logoSize = Utils.getValueString(headers, "logosize", "");

        int defaultImageResource = R.drawable.ic_channel_logo;

        if (isDestroyed() || isFinishing()) {
            return;
        }

        setLogoSize(logoSize);

        setImageWithGlide(findViewById(R.id.channel_logo_top_left), imageUrl1, appLink, defaultImageResource);
        setImageWithGlide(findViewById(R.id.channel_logo_top_right), imageUrl2, appLink, defaultImageResource);
        setImageWithGlide(findViewById(R.id.channel_logo_bottom_right), imageUrl3, appLink, defaultImageResource);
        setImageWithGlide(findViewById(R.id.channel_logo_bottom_left), imageUrl4, appLink, defaultImageResource);
    }

    private void setLogoSize(String logoSize) {
        if (logoSize != null && !logoSize.isEmpty()) {
            String[] dimensions = logoSize.split("x");

            if (dimensions.length == 2) {
                try {
                    int width = Integer.parseInt(dimensions[0].trim());
                    int height = Integer.parseInt(dimensions[1].trim());

                    if (isValidDimension(width) && isValidDimension(height)) {
                        applyLogoSize(width, height);
                    }
                } catch (NumberFormatException e) {
                    // تجاهل
                }
            }
        }
    }

    private boolean isValidDimension(int dimension) {
        return dimension >= 40 && dimension <= 200;
    }

    private void applyLogoSize(int widthDp, int heightDp) {
        float density = getResources().getDisplayMetrics().density;
        int widthPx = (int) (widthDp * density);
        int heightPx = (int) (heightDp * density);

        ImageView[] logos = {
                findViewById(R.id.channel_logo_top_left),
                findViewById(R.id.channel_logo_top_right),
                findViewById(R.id.channel_logo_bottom_right),
                findViewById(R.id.channel_logo_bottom_left)
        };

        for (ImageView logo : logos) {
            if (logo != null) {
                ViewGroup.LayoutParams params = logo.getLayoutParams();
                params.width = widthPx;
                params.height = heightPx;
                logo.setLayoutParams(params);
                logo.requestLayout();
            }
        }
    }

    private void setImageWithGlide(ImageView imageView, String imageUrl, String appLink, int defaultImageResource) {
        if (imageView != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .into(imageView);
                setImageClickListener(imageView, appLink);
            } else {
                setImageViewWithDefault(imageView, defaultImageResource);
            }
        }
    }

    private void setImageClickListener(ImageView imageView, String appLink) {
        if (appLink != null && !appLink.isEmpty()) {
            imageView.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appLink));
                startActivity(browserIntent);
            });
        }
    }

    private void setImageViewWithDefault(ImageView imageView, int defaultImageResource) {
        imageView.setImageResource(defaultImageResource);
        imageView.setVisibility(View.VISIBLE);
    }

    // restart app in vpn
    private boolean vpnRestarted = false;

    private void performVpnCheck() {
        boolean stopCheck = prefs.getBoolean("stop_check_npv", false);

        if (headers.containsKey("vvppnn") || stopCheck) {
            return;
        }

        if (npvChecker != null) {
            npvChecker.cancel();
        }

        npvChecker = new NpvChecker(prefs, stop -> {
            if (headers.containsKey("vvppnn") || prefs.getBoolean("stop_check_npv", false)) {
                return;
            }

            if (stop && !vpnRestarted) {
                vpnRestarted = true;
                releasePlayer();
                url = "file:///android_asset/fonts/offline.mp4";
                servers.clear();

                Intent intent = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });

        Utils.handler.post(npvChecker);
    }


    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP ||
                        event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN ||
                        event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ||
                        event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {

            if (!playerView.isControllerFullyVisible()) {
                playerView.showController();

                if (views != null &&
                        views.length > 0 &&
                        views[serverSelected] != null) {
                    views[serverSelected].requestFocus();
                }

                return true;
            }
        }
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_DPAD_UP) {

            if (views != null
                    && views.length > 0
                    && views[serverSelected] != null) {

                views[serverSelected].requestFocus();
                return true;
            }
        }
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            int serverIndex = -1;

            switch (event.getKeyCode()) {
                case android.view.KeyEvent.KEYCODE_1:
                case android.view.KeyEvent.KEYCODE_NUMPAD_1: serverIndex = 0; break;
                case android.view.KeyEvent.KEYCODE_2:
                case android.view.KeyEvent.KEYCODE_NUMPAD_2: serverIndex = 1; break;
                case android.view.KeyEvent.KEYCODE_3:
                case android.view.KeyEvent.KEYCODE_NUMPAD_3: serverIndex = 2; break;
                case android.view.KeyEvent.KEYCODE_4:
                case android.view.KeyEvent.KEYCODE_NUMPAD_4: serverIndex = 3; break;
                case android.view.KeyEvent.KEYCODE_5:
                case android.view.KeyEvent.KEYCODE_NUMPAD_5: serverIndex = 4; break;
                case android.view.KeyEvent.KEYCODE_6:
                case android.view.KeyEvent.KEYCODE_NUMPAD_6: serverIndex = 5; break;
                case android.view.KeyEvent.KEYCODE_7:
                case android.view.KeyEvent.KEYCODE_NUMPAD_7: serverIndex = 6; break;
                case android.view.KeyEvent.KEYCODE_8:
                case android.view.KeyEvent.KEYCODE_NUMPAD_8: serverIndex = 7; break;
                case android.view.KeyEvent.KEYCODE_9:
                case android.view.KeyEvent.KEYCODE_NUMPAD_9: serverIndex = 8; break;
                case android.view.KeyEvent.KEYCODE_0:
                case android.view.KeyEvent.KEYCODE_NUMPAD_0: serverIndex = 9; break;

                default:
                    return super.dispatchKeyEvent(event);
            }

            if (serverIndex != -1) {
                if (playerView != null) {
                    playerView.showController();
                 //   playerView.requestFocus();
                }

                if (serverIndex < servers.size()) {
                    selectServer(serverIndex);
                } else if (servers.size() > 0) {
                    selectServer(0);
                }
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void showSubtitleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_subtitle, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText subtitleUrlEditText = view.findViewById(R.id.subtitle_url_edit_text);
        Button addSubtitleButton = view.findViewById(R.id.add_subtitle_button);
        TextView cancelButton = view.findViewById(R.id.cancel_button);

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        addSubtitleButton.setOnClickListener(v -> {
            String subtitleUrl = subtitleUrlEditText.getText().toString().trim();
            if (URLUtil.isValidUrl(subtitleUrl)) {
                addSubtitle(subtitleUrl);
                dialog.dismiss();
            } else {
                subtitleUrlEditText.setError("The link is invalid.");
            }
        });

        dialog.show();
    }

    private void addSubtitle(String subtitleUrl) {
        if (player != null) {
            this.subtitleUrl = subtitleUrl;
            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(getSubtitleMimeType(subtitleUrl))
                    .setLanguage("ar")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build();

            MediaItem mediaItem = player.getCurrentMediaItem().buildUpon()
                    .setSubtitleConfigurations(List.of(subtitleConfig))
                    .build();

            player.setMediaItem(mediaItem, false);
        }
    }

    @Override
    protected void onDestroy() {
        if (videoLoadHandler != null) {
            videoLoadHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
    private void smartPlayUrl(String link) {
        new Thread(() -> {
            try {
                HttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                        .setUserAgent(userAgent)
                        .setDefaultRequestProperties(headers);

                HttpDataSource ds = http.createDataSource();
                DataSpec dataSpec = new DataSpec(Uri.parse(link));
                ds.open(dataSpec);

                byte[] buffer = new byte[2048]; // نقرأ أول 2KB
                int read = ds.read(buffer, 0, buffer.length);
                ds.close();

                if (read > 0) {
                    String preview = new String(buffer, 0, read);

                    // إذا كان المحتوى M3U8 حتى لو كان .zip
                    if (preview.contains("#EXTM3U")) {
                        runOnUiThread(() -> {
                            hardResetPlayer();   // ✅منع تكرار الصوت
                            useMimeType = true; // نجبر ExoPlayer على التعرف
                            url = link;
                            initializePlayer();
                        });
                        return;
                    }
                }

                // لو لم يكن M3U8 → تشغيل طبيعي
                runOnUiThread(() -> {
                    hardResetPlayer();   // ✅ منع تكرار الصوت
                    url = link;
                    initializePlayer();
                });

            } catch (Exception e) {
                runOnUiThread(() -> initializePlayer());
            }
        }).start();
    }
    private void fullResetAfterWebExtractor() {
        try {
            if (player != null) {
                player.clearMediaItems();
                player.setPlayWhenReady(false);
                player.stop(true);
                player.release();
            }
        } catch (Exception ignored) {}

        player = null;
        trackSelector = null;
        trackSelectionParameters = null;

        useMimeType = false;
        retryCount = 0;
        startItemIndex = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
        lastPosition = 0;

        System.gc(); // تنظيف AudioTrack + Loader
    }

}