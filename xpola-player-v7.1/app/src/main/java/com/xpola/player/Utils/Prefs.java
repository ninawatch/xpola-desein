package com.xpola.player.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.xpola.player.R;

public class Prefs {

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public Prefs(@NonNull Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public Prefs(@NonNull Context context, String name) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // --------------------------------------
    // BASIC SETTERS / GETTERS
    // --------------------------------------
    public void setInt(String key, int value) {
        editor.putInt(key, value).commit();
    }

    public void setString(String key, String value) {
        editor.putString(key, value).commit();
    }

    public void setBoolean(String key, boolean value) {
        editor.putBoolean(key, value).commit();
    }

    public boolean getBoolean(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }

    public int getInt(String key, int def) {
        return sharedPreferences.getInt(key, def);
    }

    public String getString(String key, String def) {
        return sharedPreferences.getString(key, def);
    }

    public void setLong(String key, long value) {
        editor.putLong(key, value).commit();
    }

    public long getLong(String key, long def) {
        return sharedPreferences.getLong(key, def);
    }

    public Context getContext() {
        return context;
    }

    // --------------------------------------
    // REWARD ADS SYSTEM (REMOVE ADS 1 HOUR)
    // --------------------------------------
    public void setAdsRemovedForOneHour() {
        long expire = System.currentTimeMillis() + (60 * 60 * 1000);
        editor.putLong("ads_removed_until", expire).commit();
    }

    public void setAdsRemovedUntil(long time) {
        setLong("ads_removed_until", time);
    }

    public long getAdsRemovedUntil() {
        return sharedPreferences.getLong("ads_removed_until", 0);
    }

    public boolean isAdsStillRemoved() {
        long until = getAdsRemovedUntil();
        return System.currentTimeMillis() < until;
    }

    public int getRewardProgress() {
        return sharedPreferences.getInt("reward_progress", 0);
    }

    public void setRewardProgress(int value) {
        editor.putInt("reward_progress", value).commit();
    }

    // --------------------------------------
    // SUBSCRIPTION SYSTEM
    // --------------------------------------
    public void setSubscription(String type, long expiry) {
        // type => "monthly" / "6months" / "yearly"
        editor.putString("sub_type", type).commit();
        editor.putLong("sub_expiry", expiry).commit();
    }

    public String getSubscriptionType() {
        return sharedPreferences.getString("sub_type", "");
    }

    public long getSubscriptionExpiry() {
        return sharedPreferences.getLong("sub_expiry", 0);
    }

    public boolean isSubscriptionActive() {
        long exp = getSubscriptionExpiry();
        return System.currentTimeMillis() < exp;
    }

    // --------------------------------------
    // FULL ADS REMOVAL CHECK (SUB + REWARD)
    // --------------------------------------
    public boolean isAdsRemoved() {

        // 1️⃣ الاشتراك المدفوع
        if (isSubscriptionActive()) return true;

        // 2️⃣ الإزالة عبر مشاهدة الإعلانات
        return isAdsStillRemoved();
    }
}
