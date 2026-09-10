package com.xpola.player.Ads;


import android.app.Activity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.sdk.AppLovinSdk;
import com.xpola.player.R;
import com.xpola.player.Utils.Prefs;


public class wApplivon {

    private static MaxInterstitialAd interstitialAd;
    private static Activity ctx;
    private static String appBanner = "", appInter = "";
    private static wApplivon applivon;

    public static synchronized wApplivon getInstance(Activity ctx) {
        if (applivon == null) applivon = new wApplivon();
        AppLovinSdk.getInstance(ctx).setMediationProvider("max");
        AppLovinSdk.initializeSdk(ctx, configuration -> {
        });
        wApplivon.ctx = ctx;
        Prefs prefs = new Prefs(ctx);
        appBanner = prefs.getString("appBanner", "");
        appInter = prefs.getString("appInter", "");
        if (!appInter.isEmpty()) {
            loadAd();
        }
        return applivon;
    }

    public boolean haveBanner() {
        return appBanner != null && !appBanner.trim().isEmpty();
    }

    public boolean haveInterstitial() {
        return appInter != null && !appInter.trim().isEmpty();
    }

    public void setBanner(@NonNull LinearLayout v) {
        if (!appBanner.isEmpty()) {
            MaxAdView adView = new MaxAdView(appBanner, ctx);
            adView.setListener(new AdViewListener());
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(-2,
                    ctx.getResources().getDimensionPixelSize(R.dimen.banner_height));
            adView.setLayoutParams(params1);
            // adView.setExtraParameter("adaptive_banner", "true");
            v.addView(adView);
            adView.loadAd();
        }
    }

    public void showAd() {
        if (interstitialAd.isReady()) {
            interstitialAd.showAd();
        }
        loadAd();
    }

    private static void loadAd() {
        if (interstitialAd == null) {
            interstitialAd = new MaxInterstitialAd(appInter, ctx);
            interstitialAd.setListener(new AdListener());
            interstitialAd.loadAd();
        }
    }

    public void showWhenLoad() {
        MaxInterstitialAd interstitialAd = new MaxInterstitialAd(appInter, ctx);
        interstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(MaxAd ad) {
                interstitialAd.showAd();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {

            }

            @Override
            public void onAdHidden(MaxAd ad) {

            }

            @Override
            public void onAdClicked(MaxAd ad) {

            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
            }
        });
        interstitialAd.loadAd();
    }

    private static class AdListener implements MaxAdListener {

        @Override
        public void onAdLoaded(MaxAd ad) {
        }

        @Override
        public void onAdDisplayed(MaxAd ad) {

        }

        @Override
        public void onAdHidden(MaxAd ad) {
            interstitialAd = null;
        }

        @Override
        public void onAdClicked(MaxAd ad) {

        }

        @Override
        public void onAdLoadFailed(String adUnitId, MaxError error) {
            interstitialAd = null;
        }

        @Override
        public void onAdDisplayFailed(MaxAd ad, MaxError error) {
            interstitialAd = null;
        }
    }

    private static class AdViewListener implements MaxAdViewAdListener {
        @Override
        public void onAdExpanded(MaxAd ad) {

        }

        @Override
        public void onAdCollapsed(MaxAd ad) {

        }

        @Override
        public void onAdLoaded(MaxAd ad) {

        }

        @Override
        public void onAdDisplayed(MaxAd ad) {

        }

        @Override
        public void onAdHidden(MaxAd ad) {

        }

        @Override
        public void onAdClicked(MaxAd ad) {

        }

        @Override
        public void onAdLoadFailed(String adUnitId, MaxError error) {

        }

        @Override
        public void onAdDisplayFailed(MaxAd ad, MaxError error) {

        }
    }
}
