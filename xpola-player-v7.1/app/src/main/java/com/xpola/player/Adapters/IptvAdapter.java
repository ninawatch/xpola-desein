package com.xpola.player.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.google.android.ads.nativetemplates.TemplateView;
import com.xpola.player.R;
import com.xpola.player.Ui.Activities.Player;
import com.xpola.player.Ui.Activities.browser;
import com.xpola.player.Ui.Activities.m3Player;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Parser.M3uParser;
import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IptvAdapter extends RecyclerView.Adapter<IptvAdapter.AdapterVh> {

    private final Context context;
    private List<HashMap<String, Object>> list = new ArrayList<>();
    private final Utils util;


    public IptvAdapter(Context context) {
        this.context = context;
        util = new Utils(context);
    }


    public void setList(List<HashMap<String, Object>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public AdapterVh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AdapterVh(LayoutInflater.from(context).inflate(viewType, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull AdapterVh adaptervh, int position) {
        onBind(adaptervh, position);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        HashMap<String, Object> index = list.get(position);
        if (Utils.getValue(index, "type", "").equalsIgnoreCase("admob")) {
            return R.layout.template;
        } else if (Utils.getValue(index, "type", "").equalsIgnoreCase("fb")) {
            return R.layout.fb_template;
        } else return R.layout.channels_item;
    }

    public static class AdapterVh extends RecyclerView.ViewHolder {
        View view;
        TextView name;

        ImageView poster;
        CardView card;
        LinearLayout parlin;

        public AdapterVh(View v) {
            super(v);
            view = v;
            parlin = v.findViewById(R.id.relative_item_parent);
            card = v.findViewById(R.id.card_parent);
            name = v.findViewById(R.id.item_name);
            poster = v.findViewById(R.id.item_logo);
        }
    }

    private void onBind(AdapterVh vh, int position) {
        if (getItemViewType(position) == R.layout.template) {
            AdmobNativeView(vh, position);
        } else if (getItemViewType(position) == R.layout.fb_template) {
            FacebookNativeView(vh, position);
        } else {
            ChannelsItems(vh, position);
        }
    }

    private void ChannelsItems(AdapterVh adaptervh, int position) {
        try {
            adaptervh.name.setText("");
            final Map<String, Object> index = list.get(position);
            if (index != null) {
                adaptervh.name.setText(Utils.getValue(index, "name", ""));
                util.setIcon(Utils.getValue(index, "logo", ""), adaptervh.poster);
                Utils.Radius(adaptervh.card, 20, Color.WHITE);
                Utils.Radius(adaptervh.poster, 20, Color.TRANSPARENT);
                Utils.Radius(adaptervh.name, 20, Color.TRANSPARENT);
                Utils.SetFocus(adaptervh.card, Color.WHITE, Color.WHITE);
                adaptervh.name.setClipToOutline(true);
                adaptervh.poster.setClipToOutline(true);
                adaptervh.card.setOnClickListener(v -> {
                    try {
                        String headers = Utils.getValue(index, "headers", "{}");
                        String userAgent2 = Utils.getValue(index, "user-agent", "");
                        String playerType = Utils.getValueString(Utils.getMapString(headers), "player-type", "");
                        if (index.containsKey("url")) {
                            if (playerType.equalsIgnoreCase("external")) {
                                context.startActivity(new Intent(Intent.ACTION_VIEW)
                                        .setData(Uri.parse(Utils.getValue(index, "url", ""))));
                            } else {
                                if (playerType.equalsIgnoreCase("m3u")) {
                                    context.startActivity(new Intent(context, m3Player.class)
                                            .setAction(Intent.ACTION_VIEW)
                                            .setData(Uri.parse(Utils.getValue(index, "url", "")))
                                            .putExtra("user-agent", userAgent2)
                                            .putExtra("headers", headers)
                                            .putExtra(M3uParser.DRM_SCHEME, Utils.getValue(index, M3uParser.DRM_SCHEME, ""))
                                            .putExtra(M3uParser.DRM_LICENSE, Utils.getValue(index, M3uParser.DRM_LICENSE, ""))
                                            .putExtra("title", Utils.getValue(index, "name", "")));
                                } else if (playerType.equalsIgnoreCase("browser")) {
                                    context.startActivity(new Intent(context, browser.class)
                                            .setAction(Intent.ACTION_VIEW)
                                            .setData(Uri.parse(Utils.getValue(index, "url", "")))
                                            .putExtra("user-agent", userAgent2)
                                            .putExtra("headers", headers)
                                            .putExtra(M3uParser.DRM_SCHEME, Utils.getValue(index, M3uParser.DRM_SCHEME, ""))
                                            .putExtra(M3uParser.DRM_LICENSE, Utils.getValue(index, M3uParser.DRM_LICENSE, ""))
                                            .putExtra("title", Utils.getValue(index, "name", "")));
                                } else {
                                    context.startActivity(new Intent(context, Player.class)
                                            .setAction(Intent.ACTION_VIEW)
                                            .setData(Uri.parse(Utils.getValue(index, "url", "")))
                                            .putExtra("user-agent", userAgent2)
                                            .putExtra("headers", headers)
                                            .putExtra(M3uParser.DRM_SCHEME, Utils.getValue(index, M3uParser.DRM_SCHEME, ""))
                                            .putExtra(M3uParser.DRM_LICENSE, Utils.getValue(index, M3uParser.DRM_LICENSE, ""))
                                            .putExtra("title", Utils.getValue(index, "name", "")));
                                }
                            }
                            CustomIntent.customType(context, CustomIntent.LEFT_TO_RIGHT);
                        } else if (index.containsKey("servers")) {
                            context.startActivity(new Intent(context, Player.class)
                                    .setAction(Intent.ACTION_VIEW)
                                    .putExtra("servers", Uri.parse(Utils.getValue(index, "servers", "")))
                                    .putExtra("user-agent", userAgent2)
                                    .putExtra("headers", headers)
                                    .putExtra(M3uParser.DRM_SCHEME, Utils.getValue(index, M3uParser.DRM_SCHEME, ""))
                                    .putExtra(M3uParser.DRM_LICENSE, Utils.getValue(index, M3uParser.DRM_LICENSE, ""))
                                    .putExtra("title", Utils.getValue(index, "name", "")));
                            CustomIntent.customType(context, CustomIntent.LEFT_TO_RIGHT);
                        }
                    } catch (Exception ignored) {

                    }
                });

            }
        } catch (Exception ignored) {

        }
    }


    //admob NativeView
    private void AdmobNativeView(@NonNull AdapterVh vh, int position) {
        HashMap<String, Object> index = list.get(position);
        TemplateView templateView = vh.view.findViewById(R.id.template);
        templateView.setVisibility(View.VISIBLE);
        templateView.setNativeAd((com.google.android.gms.ads.nativead.NativeAd) Objects.requireNonNull(index.get("ad")));
    }

    //Facebook NativeView
    private void FacebookNativeView(@NonNull AdapterVh vh, int position) {
        HashMap<String, Object> index = list.get(position);
        inflateAd(vh, (NativeAd) Objects.requireNonNull(index.get("ad")));
    }

    private void inflateAd(@NonNull AdapterVh vh, @NonNull NativeAd nativeAd) {
        nativeAd.unregisterView();
        NativeAdLayout nativeAdLayout = vh.view.findViewById(R.id.native_layout);

        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout adView = (LinearLayout) inflater.inflate(R.layout.fb_native_view, nativeAdLayout, false);
        nativeAdLayout.addView(adView);

        LinearLayout adChoicesContainer = nativeAdLayout.findViewById(R.id.ad_choices_container);
        AdOptionsView adOptionsView = new AdOptionsView(context, nativeAd, nativeAdLayout);
        adChoicesContainer.removeAllViews();
        adChoicesContainer.addView(adOptionsView, 0);


        MediaView nativeAdIcon = adView.findViewById(R.id.native_ad_icon);
        TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
        MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
        TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
        TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
        TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
        Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);


        nativeAdTitle.setText(nativeAd.getAdvertiserName());
        nativeAdBody.setText(nativeAd.getAdBodyText());
        nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
        nativeAdCallToAction.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.GONE);
        nativeAdCallToAction.setText(nativeAd.getAdCallToAction());
        sponsoredLabel.setText(nativeAd.getSponsoredTranslation());


        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(nativeAdLayout);
        clickableViews.add(nativeAdTitle);
        clickableViews.add(nativeAdCallToAction);

        nativeAd.registerViewForInteraction(adView, nativeAdMedia, nativeAdIcon, clickableViews);

    }

}

