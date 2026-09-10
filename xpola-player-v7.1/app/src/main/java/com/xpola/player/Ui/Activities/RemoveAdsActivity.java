package com.xpola.player.Ui.Activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.xpola.player.Billing.BillingManager;
import com.xpola.player.R;
import com.xpola.player.Utils.Prefs;

public class RemoveAdsActivity extends AppCompatActivity {

    private Prefs prefs;
    private BillingManager billingManager;

    private RewardedAd rewardedAd;
    private int rewardProgress = 0;

    private TextView txtProgress, txtStatus;
    private TextView txtPriceMonthly, txtPrice6Months, txtPriceYearly;
    private ProgressBar progressBar;
    private Button btnReward, btnMonth, btn6Months, btnYear, btnRestore;

    private String rewardID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_ads);

        prefs = new Prefs(this);
        billingManager = new BillingManager(this);

        initViews();
        setupBilling();
        loadRewardState();
        updateUI();
        loadRewardAd();
    }

    private void setupBilling() {
        billingManager.setPricesListener(prices -> {
            if (prices.containsKey(BillingManager.MONTHLY_ID)) {
                txtPriceMonthly.setText(prices.get(BillingManager.MONTHLY_ID));
            }
            if (prices.containsKey(BillingManager.SIX_MONTHS_ID)) {
                txtPrice6Months.setText(prices.get(BillingManager.SIX_MONTHS_ID));
            }
            if (prices.containsKey(BillingManager.YEARLY_ID)) {
                txtPriceYearly.setText(prices.get(BillingManager.YEARLY_ID));
            }
        });
    }

    private void initViews() {

        btnRestore = findViewById(R.id.btn_restore);
        txtProgress = findViewById(R.id.txt_progress);
        txtStatus = findViewById(R.id.txt_status);

        txtPriceMonthly = findViewById(R.id.txt_price_monthly);
        txtPrice6Months = findViewById(R.id.txt_price_6months);
        txtPriceYearly = findViewById(R.id.txt_price_yearly);

        progressBar = findViewById(R.id.progress_bar);
        btnReward = findViewById(R.id.btn_reward);

        btnMonth = findViewById(R.id.btn_month);
        btn6Months = findViewById(R.id.btn_6months);
        btnYear = findViewById(R.id.btn_year);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        rewardID = prefs.getString("adReward", "");
        rewardProgress = prefs.getRewardProgress();

        btnReward.setOnClickListener(v -> showConfirmationDialog());

        btnMonth.setOnClickListener(v -> billingManager.purchase(BillingManager.MONTHLY_ID));
        btn6Months.setOnClickListener(v -> billingManager.purchase(BillingManager.SIX_MONTHS_ID));
        btnYear.setOnClickListener(v -> billingManager.purchase(BillingManager.YEARLY_ID));

        btnRestore.setOnClickListener(v -> billingManager.restorePurchases());
    }

    private void loadRewardState() {
        rewardProgress = prefs.getRewardProgress();
    }

    // -----------------------------------------------------------------
    // CONFIRMATION BEFORE WATCHING AD
    // -----------------------------------------------------------------
    private void showConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CenterDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_reward_confirm, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_watch).setOnClickListener(v -> {
            dialog.dismiss();
            showRewardAd();
        });

        dialog.show();
    }

    // -----------------------------------------------------------------
    // REWARD AD LOADING
    // -----------------------------------------------------------------
    private void loadRewardAd() {

        if (rewardID.isEmpty()) {
            Toast.makeText(this, "Reward Ad is not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        RewardedAd.load(this, rewardID, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError error) {
                        rewardedAd = null;
                    }
                });
    }

    private void showRewardAd() {

        if (rewardedAd == null) {
            Toast.makeText(this, "Ad not ready. Try again.", Toast.LENGTH_SHORT).show();
            loadRewardAd();
            return;
        }

        rewardedAd.show(this, rewardItem -> {

            rewardProgress++;
            prefs.setRewardProgress(rewardProgress);

            if (rewardProgress >= 2) {
                long oneHourLater = System.currentTimeMillis() + 3600000L;
                prefs.setAdsRemovedUntil(oneHourLater);

                rewardProgress = 0;
                prefs.setRewardProgress(0);

                showRestartDialog();

            } else {
                txtStatus.setText("Watch 1 more to activate reward");
                progressBar.setProgress(rewardProgress);
            }

            updateUI();
            loadRewardAd();
        });
    }

    // -----------------------------------------------------------------
    // UI UPDATE SYSTEM
    // -----------------------------------------------------------------
    private void updateUI() {

        progressBar.setMax(2);
        progressBar.setProgress(rewardProgress);

        // Active subscription
        if (prefs.isSubscriptionActive()) {

            long exp = prefs.getSubscriptionExpiry();
            String time = android.text.format.DateFormat.format("dd MMM - HH:mm", exp).toString();

            txtStatus.setText("Subscription active until: " + time);
            txtProgress.setText("Premium subscription");

            disable(btnReward);

            String type = prefs.getSubscriptionType();

            if (type.equals("monthly")) disable(btnMonth);
            if (type.equals("6months")) {
                disable(btnMonth);
                disable(btn6Months);
            }
            if (type.equals("yearly")) {
                disable(btnMonth);
                disable(btn6Months);
                disable(btnYear);
            }

            return;
        }

        // Reward active
        if (prefs.isAdsStillRemoved()) {

            long until = prefs.getAdsRemovedUntil();
            String time = android.text.format.DateFormat.format("HH:mm", until).toString();

            txtStatus.setText("Ads disabled until: " + time);
            txtProgress.setText("Reward active");

            disable(btnReward);

            return;
        }

        // Normal mode
        btnReward.setEnabled(true);
        btnReward.setAlpha(1f);

        txtProgress.setText(rewardProgress + "/2 ads watched");
        txtStatus.setText("Watch 2 ads to remove ads for 1 hour");
    }

    private void disable(Button b) {
        b.setEnabled(false);
        b.setAlpha(0.4f);
    }

    // -----------------------------------------------------------------
    // RESTART APP DIALOG
    // -----------------------------------------------------------------
    private void showRestartDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CenterDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_restart, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        view.findViewById(R.id.btn_restart).setOnClickListener(v -> {
            dialog.dismiss();
            restartApp();
        });

        dialog.show();
    }

    // -----------------------------------------------------------------
    // THANK YOU DIALOG AFTER PURCHASE
    // -----------------------------------------------------------------
    public void showThankYouDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CenterDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_thank_you, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        view.findViewById(R.id.btn_restart).setOnClickListener(v -> {
            dialog.dismiss();
            restartApp();
        });

        dialog.show();
    }

    // -----------------------------------------------------------------
    // RESTART APP
    // -----------------------------------------------------------------
    private void restartApp() {
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finishAffinity();
        }
    }
}
