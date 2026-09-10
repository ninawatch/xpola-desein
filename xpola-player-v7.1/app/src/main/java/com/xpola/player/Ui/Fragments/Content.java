package com.xpola.player.Ui.Fragments;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.xpola.player.Adapters.IptvAdapter;
import com.xpola.player.Ads.AdsManager;
import com.xpola.player.R;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Content extends Fragment {
    private RecyclerView recyclerView;
    private List<HashMap<String, Object>> list = new ArrayList<>();
    private static List<?> listAds = new ArrayList<>();
    private int maxNativeAd = 0;
    private Prefs prefs;
    private int spanCount = 0;
    private boolean isAttached = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        isAttached = true;
        recyclerView = view.findViewById(R.id.list_item);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Refresh(list);
    }

    private void Refresh(List<HashMap<String, Object>> list) {
        if (isAttached) {
            if (list == null) list = new ArrayList<>();
            IptvAdapter iptvAdapter = new IptvAdapter(requireActivity());
            iptvAdapter.setList(list);
            spanCount = (int) Math.floor((float) getResources().getDisplayMetrics().widthPixels / getResources().getDimensionPixelSize(R.dimen.channel_item_width));
            GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(), spanCount);
            gridLayoutManager.setSpanSizeLookup(new LookupSpan(list, spanCount));
            recyclerView.setLayoutManager(gridLayoutManager);
            //if (recyclerView.getAdapter() == null)
            recyclerView.setAdapter(iptvAdapter);
            notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyDataSetChanged() {
        if (recyclerView.getAdapter() != null) recyclerView.getAdapter().notifyDataSetChanged();
    }


    public void setList(List<HashMap<String, Object>> data) {
        this.list = data;
        try {
            requireView().setVisibility(View.VISIBLE);
        } catch (Exception ignored) {
        }
        Utils.removeAds(list);
        Refresh(list);
        PushAds();
    }


    //load native
    private void PushAds() {
        if (isAttached) {
            if (!AdsManager.listAds.isEmpty()) {
                listAds = AdsManager.listAds;
                showNative();
                return;
            }
            AdsManager adsManager = AdsManager.getInstance(requireActivity());
            maxNativeAd = prefs.getInt("max", 5);
            if (maxNativeAd > 0) {
                adsManager.loadNative(maxNativeAd, (ad, loaded) -> {
                    if (ad instanceof List && isAttached) {
                        listAds = (List<?>) ad;
                        showNative();
                    }
                });
            }
        }
    }

    // push native to list
    private void showNative() {
        if (!listAds.isEmpty() && isAttached) {
            int line = (int) Math.floor((float) getResources().getDisplayMetrics().widthPixels / getResources().getDimensionPixelSize(R.dimen.channel_item_width));
            int position = Math.min(listAds.size() - 1, line);
            for (int i = 0; i <= maxNativeAd; i++) {
                Object o = listAds.get(i % listAds.size());
                HashMap<String, Object> index = new HashMap<>();
                index.put("name", "AD");
                if (o instanceof com.facebook.ads.NativeAd) {
                    index.put("type", "fb");
                    index.put("ad", o);
                } else if (o instanceof NativeAd) {
                    index.put("type", "admob");
                    index.put("ad", o);
                }
                if (list.size() > position && list.get(position) != null &&
                        !Utils.getValue(list.get(position), "name", "")
                                .equalsIgnoreCase("ad")) {
                    list.add(position, index);
                    //next position to add nativeAd
                    //spanCount == line ===> (spanCount * 3) == 3 lines
                    position += (spanCount * 3) + 1;
                }
            }
            Refresh(list);
        }

    }

    public static class LookupSpan extends GridLayoutManager.SpanSizeLookup {
        private final List<HashMap<String, Object>> channels;
        private final int spanCount;

        public LookupSpan(List<HashMap<String, Object>> channels, int spanCount) {
            this.channels = channels;
            this.spanCount = spanCount;
        }

        @Override
        public int getSpanSize(int i) {
            boolean isAd = channels != null && channels.size() > i && i > -1 && Utils.getValue(channels.get(i), "name", "").equalsIgnoreCase("ad");
            return isAd ? spanCount : 1;

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAttached = false;
    }
}
