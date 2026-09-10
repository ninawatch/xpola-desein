package com.xpola.player.Ui.Activities;

import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_VIDEO;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.xpola.player.Adapters.PagerAdapter;
import com.xpola.player.Ads.AdsManager;
import com.xpola.player.R;
import com.xpola.player.Ui.Fragments.Content;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Parser.Data;
import com.xpola.player.Utils.Parser.IntentParser;
import com.xpola.player.Utils.Parser.M3uParser;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;
import com.xpola.player.Utils.NpvChecker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class m3Player extends AppCompatActivity {
    private String name = "", link = "";
    private Utils util;
    private ProgressBar progress;
    private ImageView searchView;
    private ImageView back;
    private TextView titleBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<HashMap<String, Object>> toSearch = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> cats = new ArrayList<>();
    private String userAgent = "";
    private String headers = "";
    private String andId = "";

    private Prefs prefs;

    private TabLayout tabLayout;
    private ViewPager2 pager2;
    private BottomSheetDialog bottomSheetDialog;
    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m_pg);
        util = new Utils(this);
        prefs = new Prefs(this);
        dialog = new AlertDialog.Builder(this).create();
        if (Utils.checkUsageAgreements(this, prefs)) {
            return;
        }
        bottomSheetDialog = new BottomSheetDialog(this);
        progress = findViewById(R.id.m_progress);
        searchView = findViewById(R.id.m3pg_search_view);
        back = findViewById(R.id.m3pg_back);
        tabLayout = findViewById(R.id.m_tab);
        pager2 = findViewById(R.id.m_pager);
        titleBar = findViewById(R.id.title_bar);
        andId = Utils.getDeviceId(this);

        handler.postDelayed(() -> {
            LinearLayout adView = findViewById(R.id.m_adview);
            AdsManager wAdsAdapter = AdsManager.getInstance(m3Player.this);
            wAdsAdapter.setBanner(adView);
        }, 2000);
        Utils.handler.post(new NpvChecker(prefs, stop -> {
            if (stop) {
                Dialogs.DisableVPN(this, dialog);
                setIntent(new Intent());
                link = "";
            }
        }));
        initialize();

    }

    private void initialize() {
        Intent intent = getIntent();
        IntentParser.getData(intent, this, new IntentParser.DataCallback() {
            @Override
            public void onDataReceived(Data data) {
                Map<String, String> heads = data.getHeaders();
                heads.remove("player-type");
                headers = Utils.objectToString(heads);
                link = data.getUrl();
                name = data.getTitle();
                userAgent = data.getUserAgent();

                if (intent.hasExtra("title")) {
                    name = name.isEmpty() ? intent.getStringExtra("title") : name;
                }
                if (intent.getData() != null) {
                    link = link.isEmpty() ? String.valueOf(intent.getData()) : link;
                }
                if (intent.hasExtra("user-agent")) {
                    userAgent = userAgent.isEmpty() ? intent.getStringExtra("user-agent") : userAgent;
                }
                if (intent.hasExtra("headers")) {
                    headers = headers.isEmpty() ? intent.getStringExtra("headers") : headers;
                }
                setTitle(name);
                titleBar.setText(name);
                back.setOnClickListener(v -> {
                    Utils.Radius(v, 20, Color.TRANSPARENT);
                    onBackPressed();
                });
                searchView.setOnClickListener(v -> Dialogs.AllChannels(m3Player.this, bottomSheetDialog, toSearch));
                if (IntentParser.isFilesScheme(Utils.objectToString(intent.getScheme())) && !Utils.isUrl(link)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            String[] mediaStorage = {READ_MEDIA_VIDEO, READ_MEDIA_AUDIO};
                            if (ContextCompat.checkSelfPermission(m3Player.this, mediaStorage[0]) != PackageManager.PERMISSION_GRANTED
                                    || ContextCompat.checkSelfPermission(m3Player.this, mediaStorage[1]) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(mediaStorage, 1);
                            } else {
                                Parser(link);
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(m3Player.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                            } else {
                                Parser(link);
                            }
                        }
                    } else {
                        Parser(link);
                    }
                } else {
                    Parser(link);
                }
            }

            @Override
            public void onError(Exception e) {
                util.showToast("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Parser(link);
            }
        } else if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Parser(link);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initialize();
    }

    @Override
    public void onBackPressed() {
        finish();
    }


    private void Parser(String url) {
        if (!Utils.isNetworkAvailable(this)) {
            util.showToast(getString(R.string.you_are_offline));
            progress.setVisibility(View.GONE);
            return;
        }
        M3uParser.IHlsParser iHlsParser = new M3uParser.IHlsParser() {
            @Override
            public void onDataParsedSuccessfully(List<HashMap<String, Object>> data) {
                cats.clear();
                if (data != null && !data.isEmpty()) {
                    cats.addAll(data);
                    setUpPager();
                } else {
                    util.showToast("List empty");
                    progress.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailed(String error) {
                util.showToast(error);
                progress.setVisibility(View.GONE);
            }
        };
        progress.setVisibility(View.VISIBLE);
        (new M3uParser(this, prefs, iHlsParser)).parse(url, userAgent.isEmpty() ? prefs.getString("default_user_agent", "") : userAgent, headers, andId == null ? "" : andId);
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(m3Player.this, CustomIntent.RIGHT_TO_LEFT);
    }

    private void setUpPager() {
        try {
            PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), getLifecycle()).init(cats.size());
            pager2.setAdapter(pagerAdapter);
            try {
                if (!cats.isEmpty()) {
                    toSearch.clear();
                    List<HashMap<String, Object>> subList = Utils.getListObject(Utils.getValue(cats.get(0), "sub_list", "[]"));
                    toSearch.addAll(subList);
                }
            } catch (Throwable ignored) {
            }
            for (HashMap<String, Object> index : cats) {
                String name = Utils.getValue(index, "name", "");
                tabLayout.addTab(tabLayout.newTab().setText(name.toUpperCase()));
            }
            progress.setVisibility(View.GONE);
            pager2.setOffscreenPageLimit(Math.max(3, pagerAdapter.getItemCount() - 1));
            tabLayout.setVisibility(View.VISIBLE);
            pager2.setVisibility(View.VISIBLE);
            if (cats.size() <= 1) {
                tabLayout.setVisibility(View.GONE);
                pager2.setUserInputEnabled(false);
            }
            pager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    tabLayout.selectTab(tabLayout.getTabAt(position));
                    selectFragment(position);
                }
            });
            titleBar.setText(MessageFormat.format("{0} ({1})", name, toSearch.size()));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    pager2.setCurrentItem(tab.getPosition(), true);
                    selectFragment(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        } catch (Throwable e) {
            progress.setVisibility(View.GONE);
        }
    }

    private void selectFragment(int position) {
        try {
            hideAllFragments();
            HashMap<String, Object> item = cats.get(position);
            List<HashMap<String, Object>> subList = Utils.getListObject(Utils.getValue(item, "sub_list", "[]"));
            ((Content) getSupportFragmentManager().getFragments().get(position)).setList(subList);
        } catch (Exception ignored) {
        }
    }

    private void hideAllFragments() {
        try {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                fragment.requireView().setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {
        }
    }

}
