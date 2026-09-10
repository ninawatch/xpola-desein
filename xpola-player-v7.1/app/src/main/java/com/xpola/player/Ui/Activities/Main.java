package com.xpola.player.Ui.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.xpola.player.Adapters.MainAdapter;
import com.xpola.player.Ads.AdsManager;
import com.xpola.player.Connections.LoadSettings;
import com.xpola.player.Connections.NetRequests;
import com.xpola.player.Interfaces.INet;
import com.xpola.player.R;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.DeviceUtils;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;
import com.xpola.player.Utils.NpvChecker;
import com.xpola.player.Sec.Pack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends AppCompatActivity {
    private LinearLayout drawerParent;
    private ImageView privacy, facebook, telegram, contacturl, email, share, webSite, rate, about, wifiControl, adsControl;
    private RecyclerView recycler;
    private GridLayoutManager gridLayoutManager = null;
    private SearchView searchView;
    private DrawerLayout drawer;
    private ImageView openDrawer;
    private LinearLayout adView;
    private CardView addLink;
    private Utils util;
    private MainAdapter adapter;
    private List<HashMap<String, Object>> allList = new ArrayList<>(), toSearch = new ArrayList<>();
    private AdsManager wAdsAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private boolean close = false;
    private ImageView searchBtn;
    private ScrollView usageAgree;
    private CheckBox check;
    private ConsentInformation consentInformation;
    private AlertDialog dialog;
    private Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔒 فحص التعديل على التطبيق (اسم الحزمة)
        Pack.checkAndCloseIfTampered(this);
        setContentView(R.layout.main);
        if (Sec.isDebuggerAttached()) {
            finishAffinity();
            System.exit(0);
        }
        initializeLogic();
        // خاض بنسخة APK
     //   DeviceUtils.checkDevice(this);
    }

    private void initializeLogic() {
        util = new Utils(this);
        prefs = new Prefs(this);
        dialog = new AlertDialog.Builder(this).create();
        extras = getIntent().getExtras();
        setupUserAgree();
        recycler = findViewById(R.id.main_list);
        searchBtn = findViewById(R.id.main_search_btn);
        searchView = findViewById(R.id.main_search_view);
        // إظهار زر X حتى بدون كتابة
        ImageView closeBtn = searchView.findViewById(
                androidx.appcompat.R.id.search_close_btn
        );
        if (closeBtn != null) {
            closeBtn.setVisibility(View.VISIBLE);
        }
        drawer = findViewById(R.id.main_drawer);
        openDrawer = findViewById(R.id.main_open_menu);
        adView = findViewById(R.id.main_adview);
        addLink = findViewById(R.id.main_addlink);
        drawerParent = drawer.findViewById(R.id.drawer_parent);
        facebook = drawer.findViewById(R.id.drawer_facebook);
        privacy = drawer.findViewById(R.id.drawer_privacy);
        telegram = drawer.findViewById(R.id.drawer_telegram);
        contacturl = drawer.findViewById(R.id.drawer_contacturl);
        email = drawer.findViewById(R.id.drawer_gmail);
        share = drawer.findViewById(R.id.drawer_share);
        webSite = drawer.findViewById(R.id.drawer_web);
        rate = drawer.findViewById(R.id.drawer_rate);
        about = drawer.findViewById(R.id.drawer_about);
        wifiControl = drawer.findViewById(R.id.wifi_control);
        adsControl = drawer.findViewById(R.id.ads_control);
        adapter = new MainAdapter(this);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_add) {
                startActivity(new Intent(getApplicationContext(), AddLink.class)
                        .putExtra("position", -1));
                CustomIntent.customType(Main.this, CustomIntent.LEFT_TO_RIGHT);
                return true;
            } else if (itemId == R.id.nav_local_media) {
                startActivity(new Intent(getApplicationContext(), LocalMediaActivity.class));
                CustomIntent.customType(Main.this, CustomIntent.LEFT_TO_RIGHT);
                return true;
            } else if (itemId == R.id.nav_home) {
                return true;
            }
            return false;
        });
        initializeClick();
        executor.execute(() -> handler.postDelayed(() -> {
            wAdsAdapter = AdsManager.getInstance(Main.this);
            wAdsAdapter.setBanner(adView);
        }, 2000));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        Utils.handler.post(new NpvChecker(prefs, stop -> {
            if (stop) {
                Dialogs.DisableVPN(this, dialog);
            }
        }));
    }

    private void initializeClick() {
        Utils.SetFocus(openDrawer, Color.TRANSPARENT, Color.TRANSPARENT);
        drawer.addDrawerListener(new DrawerListener());
        onClick(openDrawer);
        Sec.close(Main.this);
        Utils.Radius(addLink, 360, Color.WHITE);
        EditText text = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        // ===== سلوك زر X: يمسح النص، وإذا كان فارغًا يغلق البحث =====
        ImageView closeBtn2 = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        EditText searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);

        if (closeBtn2 != null && searchText != null) {
            closeBtn2.setOnClickListener(v -> {
                String current = searchText.getText().toString();

                if (!current.isEmpty()) {
                    // إذا يوجد نص: احذفه
                    searchView.setQuery("", false);
                    // خلي زر X يبقى ظاهر
                    closeBtn2.setVisibility(View.VISIBLE);
                } else {
                    // إذا لا يوجد نص: أغلق شريط البحث مثل السابق
                    searchView.setVisibility(View.GONE);
                    searchBtn.setVisibility(View.VISIBLE);
                    searchBtn.requestFocus();
                }
            });
        }

        // ===== إبقاء زر X ظاهرًا حتى لو حذفت النص من الكيبورد =====
        ImageView closeBtn3 = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        EditText searchText2 = searchView.findViewById(androidx.appcompat.R.id.search_src_text);

        if (closeBtn3 != null && searchText2 != null) {

            // اجعله ظاهر من البداية
            closeBtn3.setVisibility(View.VISIBLE);

            searchText2.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // إذا صار النص فارغًا بسبب زر الحذف، لا تخفِ زر X
                    if (searchView.getVisibility() == View.VISIBLE) {
                        closeBtn3.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        text.setHintTextColor(0xFFDCDCDC);
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.CENTER_VERTICAL);
        search();
        addLink.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AddLink.class)
                    .putExtra("position", -1));
            CustomIntent.customType(Main.this, CustomIntent.LEFT_TO_RIGHT);
        });
        Utils.Radii(drawerParent, 0, 20, 20, 0, Color.WHITE);
        DrawerInitialize();
        Dialogs.update(this, prefs.getString("version", ""), prefs.getString("message", ""), prefs.getString("url", ""), prefs.getBoolean("forceUpdate", true));
        Dialogs.Prepare(this, prefs.getBoolean("prepare", false), prefs.getString("message", ""));
        LoadSettings.getSettingsJSONFile(this);
        ConsentRequest();
        // 🔍 دعم البحث للتلفاز
        searchBtn.setOnClickListener(v -> {

            // لا تُخفِ زر القائمة
            // openDrawer يبقى ظاهر

            searchView.setVisibility(View.VISIBLE);
            searchView.setIconified(false);
            searchView.setQuery("", false);
            searchView.requestFocus();

            // إجبار زر X على الظهور
            ImageView closeBtn = searchView.findViewById(
                    androidx.appcompat.R.id.search_close_btn
            );
            if (closeBtn != null) {
                closeBtn.setVisibility(View.VISIBLE);
            }
        });


// إغلاق البحث
        searchView.setOnCloseListener(() -> {

            searchView.setVisibility(View.GONE);

            // لا تغيّر زر menu
            searchBtn.setVisibility(View.VISIBLE);
            searchBtn.requestFocus();

            return true;
        });


    }

    private void DrawerInitialize() {
        ImageView[] images = {facebook, telegram, contacturl, email, share, privacy, webSite, about, wifiControl, adsControl}; // إضافة about إلى المصفوفة
        for (ImageView img : images) {
            Utils.SetFocus(img, Color.TRANSPARENT, Color.TRANSPARENT);
        }
        facebook.setOnClickListener(view -> goToIntent(prefs.getString("fb", "")));
        telegram.setOnClickListener(view -> goToIntent(prefs.getString("telegram", "")));
        contacturl.setOnClickListener(view -> goToIntent(prefs.getString("contacturl", "")));
        email.setOnClickListener(view -> goToIntent("mailto:" + prefs.getString("email", "")));
        share.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, prefs.getString("url", ""));
            startActivity(Intent.createChooser(intent, "Share with"));
        });
        webSite.setOnClickListener(view -> goToIntent(prefs.getString("website", "")));
        privacy.setOnClickListener(view -> {
            drawer.close();
            new NetRequests().get(prefs.getString("privacy", ""), new INet() {
                @Override
                public void onSuccess(String data, String url) {
                    Dialogs.privacy(Main.this, data);
                }

                @Override
                public void onFailed(String error, String url) {
                    Dialogs.privacy(Main.this, error);
                }
            });
        });
        rate.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=com.xpola.player"))));
        about.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), AboutActivity.class));
            CustomIntent.customType(Main.this, CustomIntent.LEFT_TO_RIGHT);
        });
        wifiControl.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), WifiControlActivity.class));
            CustomIntent.customType(Main.this, CustomIntent.LEFT_TO_RIGHT);
        });
        adsControl.setOnClickListener(view -> {
            drawer.close();
            startActivity(new Intent(getApplicationContext(), RemoveAdsActivity.class));
            CustomIntent.customType(Main.this, CustomIntent.LEFT_TO_RIGHT);
        });
    }

    private void goToIntent(String url) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        refreshData();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // لو البحث مفتوح، أغلقه أولًا
        if (searchView.getVisibility() == View.VISIBLE) {
            searchView.setVisibility(View.GONE);
            searchBtn.setVisibility(View.VISIBLE);
            searchBtn.requestFocus();
            return;
        }

        // الكود القديم
        handler.postDelayed(() -> close = false, 2000);
        if (close) {
            finishAffinity();
            return;
        }
        close = true;
        util.showToast("Press back button again to exit");
    }


    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(Main.this, CustomIntent.FADEIN_TO_FADEOUT);
    }

    private void refreshData() {
        try {
            recycler.setVisibility(View.GONE);
            String listString = prefs.getString("list", "[]");
            allList = Utils.getListObject(listString);
            if (allList == null) allList = new ArrayList<>();
            toSearch = Utils.getListObject(listString);
            if (toSearch == null) toSearch = new ArrayList<>();
            if (!allList.isEmpty()) {
                recycler.setVisibility(View.VISIBLE);
            }
            adapter.Config(allList, (item, i, v) -> refreshData());
            if (gridLayoutManager == null) {
                gridLayoutManager = new GridLayoutManager(Main.this, 1);
                recycler.setLayoutManager(gridLayoutManager);
            }
            if (recycler.getAdapter() == null) recycler.setAdapter(adapter);
            notifyChanged();
        } catch (Exception ignored) {
            allList = new ArrayList<>();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyChanged() {
        if (recycler.getAdapter() != null) {
            recycler.getAdapter().notifyDataSetChanged();
        }
    }

    private void search() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.trim().isEmpty()) {
                    refreshData();
                }
                notifyChanged();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    refreshData();
                } else {
                    allList.clear();
                }
                executor.execute(() -> {
                    for (HashMap<String, Object> index : toSearch) {
                        if (index.containsKey("name") && Objects.requireNonNull(index.get("name")).toString().toLowerCase().replaceAll("\\+", " ").contains(newText.toLowerCase().replaceAll("\\+", " "))) {
                            if (!allList.contains(index)) allList.add(index);
                        }
                    }
                    handler.post(() -> notifyChanged());
                });
                return false;
            }
        });
    }

    private void onClick(@NonNull final View v) {
        v.setOnClickListener(v1 -> {
            Utils.Radius(v1, 10, Color.TRANSPARENT);
            if (v1 == openDrawer) {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                    openDrawer.setImageResource(R.drawable.ic_menu);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                    openDrawer.setImageResource(R.drawable.ic_close);
                }
            }
        });
    }

    private class DrawerListener implements DrawerLayout.DrawerListener {
        @Override
        public void onDrawerSlide(@NonNull View v, float f) {
        }

        @Override
        public void onDrawerOpened(@NonNull View v) {
            openDrawer.setImageResource(R.drawable.ic_close);
            drawerParent.setVisibility(View.VISIBLE);
        }

        @Override
        public void onDrawerClosed(@NonNull View v) {
            openDrawer.setImageResource(R.drawable.ic_menu);
            drawerParent.setVisibility(View.GONE);
        }

        @Override
        public void onDrawerStateChanged(int sc) {
        }
    }

    private void setupUserAgree() {
        usageAgree = findViewById(R.id.usage_agreements);
        usageAgree.setVisibility(View.VISIBLE);
        TextView copyRight = findViewById(R.id.copyright);
        TextView policy = findViewById(R.id.policy);
        CardView done = findViewById(R.id.done);
        check = findViewById(R.id.im_agree);
        Utils.Radius(done, 360, Color.TRANSPARENT);
        if (prefs.getBoolean("done", false)) {
            usageAgree.setVisibility(View.GONE);
            if (extras != null && extras.containsKey("class")) {
                startActivity(new Intent(this, Utils.getClass(extras.getString("class"), Player.class)));
            }
        }
        policy.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(prefs.getString("policy_1", ""))));
            } catch (Exception ignored) {
            }
        });
        copyRight.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(prefs.getString("policy_2", ""))));
            } catch (Exception ignored) {
            }
        });
        done.setOnClickListener(view -> {
            if (check.isChecked()) {
                usageAgree.setVisibility(View.GONE);
                prefs.setBoolean("done", true);
                if (extras != null && extras.containsKey("class")) {
                    startActivity(new Intent(this, Utils.getClass(extras.getString("class"), Player.class)));
                }
            } else {
                util.showToast("Agree to continue");
            }
        });
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
        });
    }

    private void loadForm() {
        UserMessagingPlatform.loadConsentForm(this, consentForm -> {
            if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                consentForm.show(Main.this, formError -> {
                    consentInformation.getConsentStatus();
                    loadForm();
                });
            }
        }, formError -> {
        });
    }
}
