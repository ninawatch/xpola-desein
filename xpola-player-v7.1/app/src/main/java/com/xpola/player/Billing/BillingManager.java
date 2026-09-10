package com.xpola.player.Billing;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.xpola.player.Ui.Activities.RemoveAdsActivity;
import com.xpola.player.Utils.Prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingManager implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private Activity activity;

    public static final String MONTHLY_ID = "xpola_remove_ads_monthly";
    public static final String SIX_MONTHS_ID = "xpola_remove_ads_6months";
    public static final String YEARLY_ID = "xpola_remove_ads_yearly";

    private Prefs prefs;
    private Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private BillingPricesListener pricesListener;

    public interface BillingPricesListener {
        void onPricesFetched(Map<String, String> prices);
    }

    public BillingManager(Activity activity) {
        this.activity = activity;
        prefs = new Prefs(activity);

        billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this)
                .build();

        startConnection();
    }

    public void setPricesListener(BillingPricesListener listener) {
        this.pricesListener = listener;
        if (!productDetailsMap.isEmpty() && pricesListener != null) {
            notifyPrices();
        }
    }

    // -----------------------------------------
    // CHECK IF GOOGLE PLAY STORE EXISTS
    // -----------------------------------------
    private boolean isGooglePlayAvailable() {
        try {
            activity.getPackageManager().getPackageInfo("com.android.vending", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------
    // START BILLING CONNECTION SAFELY
    // -----------------------------------------
    private void startConnection() {

        if (!isGooglePlayAvailable()) {
            Toast.makeText(activity,
                    "Google Play Store not available on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    fetchProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                startConnection();
            }
        });
    }

    private void fetchProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SIX_MONTHS_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, list) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                for (ProductDetails details : list) {
                    productDetailsMap.put(details.getProductId(), details);
                }
                notifyPrices();
            }
        });
    }

    private void notifyPrices() {
        if (pricesListener == null) return;

        Map<String, String> prices = new HashMap<>();
        for (Map.Entry<String, ProductDetails> entry : productDetailsMap.entrySet()) {
            ProductDetails details = entry.getValue();
            if (details.getSubscriptionOfferDetails() != null && !details.getSubscriptionOfferDetails().isEmpty()) {
                String price = details.getSubscriptionOfferDetails().get(0)
                        .getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                prices.put(entry.getKey(), price);
            }
        }

        activity.runOnUiThread(() -> {
            if (pricesListener != null) {
                pricesListener.onPricesFetched(prices);
            }
        });
    }

    // -----------------------------------------
    // PURCHASE PRODUCT
    // -----------------------------------------
    public void purchase(String productId) {

        if (!isGooglePlayAvailable()) {
            Toast.makeText(activity,
                    "This device does not support Google Play Billing",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ProductDetails productDetails = productDetailsMap.get(productId);
        if (productDetails == null) {
            Toast.makeText(activity, "Subscription not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String offerToken = "";
        if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            offerToken = productDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build());

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, flowParams);
    }

    // -----------------------------------------
    // HANDLE PURCHASE RESULT
    // -----------------------------------------
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult,
                                   List<Purchase> purchases) {

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {

            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(activity, "Purchase canceled!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Error: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            String productId = purchase.getProducts().get(0);
            long now = System.currentTimeMillis();

            switch (productId) {

                case MONTHLY_ID:
                    prefs.setSubscription("monthly", now + 30L * 24 * 60 * 60 * 1000);
                    break;

                case SIX_MONTHS_ID:
                    prefs.setSubscription("6months", now + 180L * 24 * 60 * 60 * 1000);
                    break;

                case YEARLY_ID:
                    prefs.setSubscription("yearly", now + 365L * 24 * 60 * 60 * 1000);
                    break;
            }

            if (activity instanceof RemoveAdsActivity) {
                ((RemoveAdsActivity) activity).showThankYouDialog();
            }

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams params =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(params, br -> {});
            }
        }
    }

    // -----------------------------------------
    // RESTORE PREVIOUS SUBSCRIPTION
    // -----------------------------------------
    public void restorePurchases() {

        if (!isGooglePlayAvailable()) {
            Toast.makeText(activity,
                    "Google Play Store not available on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }

        QueryPurchasesParams params =
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();

        billingClient.queryPurchasesAsync(params, (billingResult, list) -> {

            if (list != null && !list.isEmpty()) {

                for (Purchase p : list) {
                    handlePurchase(p);
                }

                Toast.makeText(activity, "Subscription Restored", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(activity, "No active subscription found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
