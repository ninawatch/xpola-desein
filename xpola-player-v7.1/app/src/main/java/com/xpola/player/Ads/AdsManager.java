package com.xpola.player.Ads;

import android.app.Activity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.xpola.player.Interfaces.OnAdListener;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class AdsManager {

    private final Prefs prefs;
    public static final int ADMOB = 0;
    public static final int APPLOVIN = 1;
    public static final int FACEBOOK = 2;

    private final adMob adm;
    private final wApplivon AppLovin;
    private final Faceboook faceboook;
    public static List<?> listAds = new ArrayList<>();

    @NonNull
    public static synchronized AdsManager getInstance(Activity ctx) {
        return new AdsManager(ctx);
    }

    private AdsManager(Activity ctx) {
        prefs = new Prefs(ctx);
        adm = adMob.getInstance(ctx);
        AppLovin = wApplivon.getInstance(ctx);
        faceboook = Faceboook.getInstance(ctx);
    }

    public void setAdType(int adType) {
        prefs.setInt("ad_type_banner", adType);
    }

    public void setAdTypeINI(int adType) {
        prefs.setInt("ad_type_ini", adType);
    }

    public void setAdTypeINI2(int adType) {
        prefs.setInt("ad_type_ini", adType);
    }

    public void setAdTypeNative(int adType) {
        prefs.setInt("ad_type_native", adType);
    }

    public void setBanner(LinearLayout frame) {
        int x = prefs.getInt("ad_type_banner", 0);
        if (x == ADMOB) {
            if (adm.haveBanner()) {
                adm.setBanner(frame);
            } else if (AppLovin.haveBanner()) {
                AppLovin.setBanner(frame);
            } else if (faceboook.haveBanner()) {
                faceboook.setBanner(frame);
            }
            setAdType(APPLOVIN);
        } else if (x == APPLOVIN) {
            if (AppLovin.haveBanner()) {
                AppLovin.setBanner(frame);
            } else if (faceboook.haveBanner()) {
                faceboook.setBanner(frame);
            } else if (adm.haveBanner()) {
                adm.setBanner(frame);
            }
            setAdType(FACEBOOK);
        } else if (x == FACEBOOK) {
            if (faceboook.haveBanner()) {
                faceboook.setBanner(frame);
            } else if (adm.haveBanner()) {
                adm.setBanner(frame);
            } else if (AppLovin.haveBanner()) {
                AppLovin.setBanner(frame);
            }
            setAdType(ADMOB);
        }
    }

    public void loadNative(int max, OnAdListener onAdListener) {
        int x = prefs.getInt("ad_type_native", 0);
        if (listAds != null && !listAds.isEmpty()) {
            onAdListener.onAdLoadListener(listAds, true);
            return;
        }
        if (x == ADMOB && adm != null) {
            adm.LoadNative((ad, loaded) -> {
                if (ad instanceof List) {
                    listAds = Utils.removeNullObjects((List<?>) ad);
                    onAdListener.onAdLoadListener(listAds, true);
                }
            }, max);
            setAdTypeNative(FACEBOOK);
        } else if (x == FACEBOOK && faceboook != null) {
            faceboook.LoadNative((ad, loaded) -> {
                listAds = Utils.removeNullObjects((List<?>) ad);
                onAdListener.onAdLoadListener(listAds, true);
            }, max);
            setAdTypeNative(ADMOB);
        }
    }

    public void showAd() {
        int counter = prefs.getInt("counter", 0);
        if (counter >= prefs.getInt("ads_counter", 3)) {
            counter = -1;
            int x = prefs.getInt("ad_type_ini", 0);
            if (x == ADMOB) {
                if (adm.haveInterstitial()) {
                    adm.showAd();
                } else if (AppLovin.haveInterstitial()) {
                    AppLovin.showAd();
                } else if (faceboook.haveInterstitial()) {
                    faceboook.showAd();
                }
                setAdTypeINI(APPLOVIN);
            } else if (x == APPLOVIN) {
                if (AppLovin.haveInterstitial()) {
                    AppLovin.showAd();
                } else if (faceboook.haveInterstitial()) {
                    faceboook.showAd();
                } else if (adm.haveInterstitial()) {
                    adm.showAd();
                }
                setAdTypeINI(FACEBOOK);
            } else if (x == FACEBOOK) {
                if (faceboook.haveInterstitial()) {
                    faceboook.showAd();
                } else if (adm.haveInterstitial()) {
                    adm.showAd();
                } else if (AppLovin.haveInterstitial()) {
                    AppLovin.showAd();
                }
                setAdTypeINI(ADMOB);
            }
        }
        prefs.setInt("counter", counter + 1);
    }


    public void showAdWhenOpenPlayer() {
        int counter = prefs.getInt("counter", 0);
        if (counter >= prefs.getInt("ads_counter", 3)) {
            counter = -1;
            int x = prefs.getInt("ad_type_ini", 0);
            if (x == ADMOB) {
                if (adm.haveInterstitial()) {
                    adm.showWhenLoad();
                } else if (AppLovin.haveInterstitial()) {
                    AppLovin.showWhenLoad();
                } else if (faceboook.haveInterstitial()) {
                    faceboook.showWhenLoad();
                }
                setAdTypeINI(APPLOVIN);
            } else if (x == APPLOVIN) {
                if (AppLovin.haveInterstitial()) {
                    AppLovin.showWhenLoad();
                } else if (faceboook.haveInterstitial()) {
                    faceboook.showWhenLoad();
                } else if (adm.haveInterstitial()) {
                    adm.showWhenLoad();
                }
                setAdTypeINI(FACEBOOK);
            } else if (x == FACEBOOK) {
                if (faceboook.haveInterstitial()) {
                    faceboook.showWhenLoad();
                } else if (adm.haveInterstitial()) {
                    adm.showWhenLoad();
                } else if (AppLovin.haveInterstitial()) {
                    AppLovin.showWhenLoad();
                }
                setAdTypeINI(ADMOB);
            }
        }
        prefs.setInt("counter", counter + 1);
    }

    public void showAdWhenOpenFromWeb() {
        int counter = prefs.getInt("counter_2", 0);
        if (counter >= prefs.getInt("ads_counter_web", 3)) {
            counter = -1;
            int x = prefs.getInt("ad_type_ini_2", 0);
            if (x == ADMOB) {
                if (adm.haveInterstitial()) {
                    adm.showWhenLoad();
                } else if (AppLovin.haveInterstitial()) {
                    AppLovin.showWhenLoad();
                } else if (faceboook.haveInterstitial()) {
                    faceboook.showWhenLoad();
                }
                setAdTypeINI2(APPLOVIN);
            } else if (x == APPLOVIN) {
                if (AppLovin.haveInterstitial()) {
                    AppLovin.showWhenLoad();
                } else if (faceboook.haveInterstitial()) {
                    faceboook.showWhenLoad();
                } else if (adm.haveInterstitial()) {
                    adm.showWhenLoad();
                }
                setAdTypeINI2(FACEBOOK);
            } else if (x == FACEBOOK) {
                if (faceboook.haveInterstitial()) {
                    faceboook.showWhenLoad();
                } else if (adm.haveInterstitial()) {
                    adm.showWhenLoad();
                } else if (AppLovin.haveInterstitial()) {
                    AppLovin.showWhenLoad();
                }
                setAdTypeINI2(ADMOB);
            }
        }
        prefs.setInt("counter_2", counter + 1);
    }
}
