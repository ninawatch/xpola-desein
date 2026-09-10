package com.xpola.player.Ads;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.xpola.player.Interfaces.OnAdListener;
import com.xpola.player.Utils.Prefs;

import java.util.ArrayList;
import java.util.List;

public class adMob {
    @SuppressLint("StaticFieldLeak")
    private static Activity ctx;
    private static InterstitialAd interstitialAd;
    private static String banner = "", inter = "", nativeAd = "";
    @SuppressLint("StaticFieldLeak")
    private static adMob admob;
    private static final List<NativeAd> nativeAds = new ArrayList<>();

    public static synchronized adMob getInstance(Activity ctx) {
        if (admob == null) admob = new adMob();
        adMob.ctx = ctx;
        Prefs prefs = new Prefs(ctx);
        String appID = prefs.getString("appId", "");
        if (!appID.isEmpty()) {
            try {
                ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
                ai.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", appID);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        MobileAds.initialize(ctx, initializationStatus -> {
        });
        banner = prefs.getString("adBanner", "");
        inter = prefs.getString("adInter", "");
        nativeAd = prefs.getString("adNative", "");
        if (!inter.isEmpty()) {
            loadFullPage();
        }
        return admob;
    }

    public boolean haveBanner() {
        return banner != null && !banner.isEmpty();
    }

    public boolean haveInterstitial() {
        return inter != null && !inter.isEmpty();
    }


    public void setBanner(@NonNull LinearLayout v) {
        if (!banner.isEmpty()) {
            v.setVisibility(View.VISIBLE);
            AdView adView = new AdView(ctx);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(banner);
            v.addView(adView);
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    public static void loadFullPage() {
        if (interstitialAd == null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(ctx, inter, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    interstitialAd = null;
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interAd) {
                    super.onAdLoaded(interAd);
                    interstitialAd = interAd;
                }
            });
        }
    }

    public void showWhenLoad() {
        if (!inter.isEmpty()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(ctx, inter, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interAd) {
                    super.onAdLoaded(interAd);
                    interAd.show(ctx);
                }
            });
        }
    }


    public void LoadNative(OnAdListener onAdListener, int max) {
        if (max > 0) {
            if (!nativeAds.isEmpty()) {
                onAdListener.onAdLoadListener(nativeAds, true);
                return;
            }
            if (!nativeAd.isEmpty()) {
                AdLoader adLoader = new AdLoader.Builder(ctx, nativeAd)
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                super.onAdFailedToLoad(loadAdError);
                                nativeAds.add(null);
                                if (nativeAds.size() >= max)
                                    onAdListener.onAdLoadListener(nativeAds, true);
                            }

                            @Override
                            public void onAdLoaded() {
                                super.onAdLoaded();
                            }
                        }).forNativeAd(nativeAd -> {
                            nativeAds.add(nativeAd);
                            if (nativeAds.size() >= max)
                                onAdListener.onAdLoadListener(nativeAds, true);
                        })
                        .build();
                adLoader.loadAds(new AdRequest.Builder().build(), max);
            }
        }
    }

    public void showAd() {
        if (interstitialAd != null) {
            interstitialAd.show(ctx);
        }
        loadFullPage();
    }


}

