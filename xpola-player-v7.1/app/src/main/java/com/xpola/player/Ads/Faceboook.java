package com.xpola.player.Ads;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdListener;
import com.xpola.player.Interfaces.OnAdListener;
import com.xpola.player.Utils.Prefs;

import java.util.ArrayList;
import java.util.List;

public class Faceboook {
    private static InterstitialAd interstitialAd;
    private final Context ctx;
    private static String fbInter = "", fbBanner = "", fbNative = "";
    private static Faceboook faceboook;
    private static final List<NativeAd> nativeAds = new ArrayList<>();

    private Faceboook(Context ctx) {
        this.ctx = ctx;
        Prefs prefs = new Prefs(ctx);
        fbInter = prefs.getString("fb_inter", "");
        fbBanner = prefs.getString("fb_banner", "");
        fbNative = prefs.getString("fb_native", "");

    }

    public static Faceboook getInstance(Context context) {
        if (faceboook == null) faceboook = new Faceboook(context);
        AudienceNetworkAds.initialize(context);
        if (!fbInter.isEmpty() && interstitialAd == null) {
            interstitialAd = new InterstitialAd(context, fbInter);
            interstitialAd.loadAd(interstitialAd.buildLoadAdConfig().withAdListener(new AdListener()).build());
        }
        return faceboook;
    }

    public boolean haveBanner() {
        return fbBanner != null && !fbBanner.trim().isEmpty();
    }

    public boolean haveInterstitial() {
        return fbInter != null && !fbInter.trim().isEmpty();
    }

    public void setBanner(@NonNull LinearLayout v) {
        if (!fbBanner.isEmpty()) {
            AdView adView = new AdView(ctx, fbBanner, AdSize.BANNER_HEIGHT_50);
            v.addView(adView);
            v.setVisibility(View.VISIBLE);
            adView.loadAd(adView.buildLoadAdConfig().withAdListener(new AdListener()).build());
        }
    }


    public void LoadNative(OnAdListener onAdListener, int max) {
        if (!fbNative.isEmpty() && max > 0) {
            if (!nativeAds.isEmpty()) {
                onAdListener.onAdLoadListener(nativeAds, true);
                return;
            }
            for (int i = 0; i < max; i++) {
                NativeAd nativeAd = new NativeAd(ctx, fbNative);
                NativeAdListener nativeAdListener = new NativeAdListener() {
                    @Override
                    public void onMediaDownloaded(Ad ad) {

                    }

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        nativeAds.add(null);
                        if (nativeAds.size() >= max) {
                            onAdListener.onAdLoadListener(nativeAds, true);
                        }
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        nativeAds.add(nativeAd);
                        if (nativeAds.size() >= max) {
                            onAdListener.onAdLoadListener(nativeAds, true);
                        }
                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                };
                nativeAd.loadAd(nativeAd.buildLoadAdConfig()
                        .withAdListener(nativeAdListener).build());
            }
        }
    }

    public void showAd() {
        if (interstitialAd != null) {
            if (interstitialAd.isAdLoaded()) {
                interstitialAd.show();
            }
            interstitialAd.loadAd(interstitialAd.buildLoadAdConfig().withAdListener(new AdListener()).build());
        }
    }

    public void showWhenLoad() {
        if (!fbInter.isEmpty()) {
            InterstitialAd interstitialAd = new InterstitialAd(ctx, fbInter);
            interstitialAd.loadAd(interstitialAd.buildLoadAdConfig().withAdListener(new AdListener()).build());
            interstitialAd.loadAd(interstitialAd.buildLoadAdConfig().withAdListener(new InterstitialAdListener() {
                @Override
                public void onInterstitialDisplayed(Ad ad) {

                }

                @Override
                public void onInterstitialDismissed(Ad ad) {

                }

                @Override
                public void onError(Ad ad, AdError adError) {

                }

                @Override
                public void onAdLoaded(Ad ad) {
                    interstitialAd.show();
                }

                @Override
                public void onAdClicked(Ad ad) {

                }

                @Override
                public void onLoggingImpression(Ad ad) {

                }
            }).build());
        }
    }


    private static class AdListener implements InterstitialAdListener {
        @Override
        public void onInterstitialDisplayed(Ad ad) {
        }

        @Override
        public void onInterstitialDismissed(Ad ad) {
            interstitialAd = null;
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            interstitialAd = null;
        }

        @Override
        public void onAdLoaded(Ad ad) {
        }

        @Override
        public void onAdClicked(Ad ad) {

        }

        @Override
        public void onLoggingImpression(Ad ad) {

        }
    }


}
