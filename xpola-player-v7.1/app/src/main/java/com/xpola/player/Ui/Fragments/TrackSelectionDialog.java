
/* * Copyright (C) 2019 The Android Open Source Project * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */


package com.xpola.player.Ui.Fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.TrackSelectionView;
import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.ImmutableList;
import com.xpola.player.R;
import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackSelectionDialog extends DialogFragment {

    public interface TrackSelectionListener {
        void onTracksSelected(TrackSelectionParameters trackSelectionParameters);
    }

    public static final ImmutableList<Integer> SUPPORTED_TRACK_TYPES =
            ImmutableList.of(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT);

    private final SparseArray<TrackSelectionViewFragment> tabFragments;
    private final ArrayList<Integer> tabTrackTypes;

    private DialogInterface.OnClickListener onClickListener;
    private DialogInterface.OnDismissListener onDismissListener;

    public static boolean willHaveContent(Player player) {
        return willHaveContent(player.getCurrentTracks());
    }

    public static boolean willHaveContent(@NonNull Tracks tracks) {
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (SUPPORTED_TRACK_TYPES.contains(trackGroup.getType())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static TrackSelectionDialog createForPlayer(
            @NonNull Player player, DialogInterface.OnDismissListener onDismissListener) {
        return createForTracksAndParameters(
                R.string.app_name, player.getCurrentTracks(),
                player.getTrackSelectionParameters(),
                false,
                false,
                player::setTrackSelectionParameters,
                onDismissListener);
    }

    @NonNull
    public static TrackSelectionDialog createForTracksAndParameters(
            int titleId,
            Tracks tracks,
            TrackSelectionParameters trackSelectionParameters,
            boolean allowAdaptiveSelections,
            boolean allowMultipleOverrides,
            TrackSelectionListener trackSelectionListener,
            DialogInterface.OnDismissListener onDismissListener) {
        TrackSelectionDialog trackSelectionDialog = new TrackSelectionDialog();
        trackSelectionDialog.init(
                tracks,
                trackSelectionParameters,
                allowAdaptiveSelections,
                allowMultipleOverrides,
                (dialog, which) -> {
                    TrackSelectionParameters.Builder builder = trackSelectionParameters.buildUpon();
                    for (int i = 0; i < SUPPORTED_TRACK_TYPES.size(); i++) {
                        int trackType = SUPPORTED_TRACK_TYPES.get(i);
                        builder.setTrackTypeDisabled(trackType, trackSelectionDialog.getIsDisabled(trackType));
                        builder.clearOverridesOfType(trackType);
                        Map<TrackGroup, TrackSelectionOverride> overrides =
                                trackSelectionDialog.getOverrides(trackType);
                        for (TrackSelectionOverride override : overrides.values()) {
                            builder.addOverride(override);
                        }
                    }
                    trackSelectionListener.onTracksSelected(builder.build());
                },
                onDismissListener);
        return trackSelectionDialog;
    }

    public TrackSelectionDialog() {
        tabFragments = new SparseArray<>();
        tabTrackTypes = new ArrayList<>();
        setRetainInstance(true);
    }

    private void init(
            Tracks tracks,
            TrackSelectionParameters trackSelectionParameters,
            boolean allowAdaptiveSelections,
            boolean allowMultipleOverrides,
            DialogInterface.OnClickListener onClickListener,
            DialogInterface.OnDismissListener onDismissListener) {
        this.onClickListener = onClickListener;
        this.onDismissListener = onDismissListener;

        for (int i = 0; i < SUPPORTED_TRACK_TYPES.size(); i++) {
            @C.TrackType int trackType = SUPPORTED_TRACK_TYPES.get(i);
            ArrayList<Tracks.Group> trackGroups = new ArrayList<>();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == trackType) {
                    trackGroups.add(trackGroup);
                }
            }
            if (!trackGroups.isEmpty()) {
                TrackSelectionViewFragment tabFragment = new TrackSelectionViewFragment();
                tabFragment.init(
                        trackGroups,
                        trackSelectionParameters.disabledTrackTypes.contains(trackType),
                        trackSelectionParameters.overrides,
                        allowAdaptiveSelections,
                        allowMultipleOverrides);
                tabFragments.put(trackType, tabFragment);
                tabTrackTypes.add(trackType);
            }
        }
    }

    private static String getTrackTypeString(Resources resources, int trackType) {
        switch (trackType) {
            case C.TRACK_TYPE_VIDEO:
                return resources.getString(R.string.exo_track_selection_title_video);
            case C.TRACK_TYPE_AUDIO:
                return resources.getString(R.string.exo_track_selection_title_audio);
            case C.TRACK_TYPE_TEXT:
                return resources.getString(R.string.exo_track_selection_title_text);
            default:
                return "";
        }
    }

    public void overrideFonts(final View v) {
        try {
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    overrideFonts(child);
                }
            } else if (v instanceof TextView) {
                ((TextView) v).setTextColor(Color.WHITE);
                ((TextView) v).setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "fonts/droid.ttf"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getIsDisabled(int trackType) {
        TrackSelectionViewFragment trackView = tabFragments.get(trackType);
        return trackView != null && trackView.isDisabled;
    }

    public Map<TrackGroup, TrackSelectionOverride> getOverrides(int trackType) {
        TrackSelectionViewFragment trackView = tabFragments.get(trackType);
        return trackView == null ? Collections.emptyMap() : trackView.overrides;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AppCompatDialog(requireActivity());
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = requireActivity().getWindow().getDecorView().getWindowInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                }
            } else {
                requireActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                requireActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
            requireActivity().getWindow().getDecorView().setSystemUiVisibility(3846);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                requireActivity().getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        onDismissListener.onDismiss(dialog);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false);
        TabLayout tabLayout = dialogView.findViewById(R.id.track_selection_dialog_tab_layout);
        ViewPager2 viewPager = dialogView.findViewById(R.id.track_selection_dialog_view_pager);
        Button cancelButton = dialogView.findViewById(R.id.track_selection_dialog_cancel_button);
        Button okButton = dialogView.findViewById(R.id.track_selection_dialog_ok_button);
        for (int x = 0; x < tabFragments.size(); x++) {
            tabLayout.addTab(tabLayout.newTab().setText(getTrackTypeString(getResources(), tabTrackTypes.get(x))));
            TabLayout.Tab tab = tabLayout.getTabAt(x);
            if (tab != null)
                overrideFonts(tab.view);
        }
        viewPager.setAdapter(new FragmentAdapter(getChildFragmentManager(), getLifecycle()));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        tabLayout.setVisibility(tabFragments.size() > 1 ? View.VISIBLE : View.GONE);
        cancelButton.setOnClickListener(view -> dismiss());
        okButton.setOnClickListener(view -> {
            onClickListener.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
            dismiss();
        });
        Utils.SetFocus(okButton, Color.TRANSPARENT, Color.TRANSPARENT);
        Utils.SetFocus(cancelButton, Color.TRANSPARENT, Color.TRANSPARENT);
        overrideFonts(dialogView);
        return dialogView;
    }


    private final class FragmentAdapter extends FragmentStateAdapter {

        public FragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return tabFragments.get(tabTrackTypes.get(position));
        }

        @Override
        public int getItemCount() {
            return tabFragments.size();
        }
    }

    public static final class TrackSelectionViewFragment extends Fragment
            implements TrackSelectionView.TrackSelectionListener {

        private List<Tracks.Group> trackGroups;
        private boolean allowAdaptiveSelections;
        private boolean allowMultipleOverrides;

        boolean isDisabled;
        Map<TrackGroup, TrackSelectionOverride> overrides;

        public TrackSelectionViewFragment() {
            setRetainInstance(true);
        }

        public void init(
                List<Tracks.Group> trackGroups,
                boolean isDisabled,
                Map<TrackGroup, TrackSelectionOverride> overrides,
                boolean allowAdaptiveSelections,
                boolean allowMultipleOverrides) {
            this.trackGroups = trackGroups;
            this.isDisabled = isDisabled;
            this.allowAdaptiveSelections = allowAdaptiveSelections;
            this.allowMultipleOverrides = allowMultipleOverrides;
            this.overrides = new HashMap<>(TrackSelectionView.filterOverrides(overrides, trackGroups, allowMultipleOverrides));
        }

        @Override
        public View onCreateView(
                LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View rootView =
                    inflater.inflate(
                            R.layout.exo_track_selection_dialog, container, false);
            TrackSelectionView trackSelectionView = rootView.findViewById(R.id.exo_track_selection_view);
            trackSelectionView.setShowDisableOption(true);
            trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides);
            trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections);
            trackSelectionView.init(
                    trackGroups,
                    isDisabled,
                    overrides,
                    null,
                    this);
            overrideFonts(trackSelectionView);
            trackSelectionView.setFocusable(false);
            trackSelectionView.setFocusableInTouchMode(false);
            trackSelectionView.setClickable(false);
            trackSelectionView.setDescendantFocusability(
                    ViewGroup.FOCUS_AFTER_DESCENDANTS);

            trackSelectionView.post(() -> {
                if (trackSelectionView.getChildCount() > 0) {
                    View child = trackSelectionView.getChildAt(0);
                    child.setFocusable(true);
                    child.setFocusableInTouchMode(true);
                    child.requestFocus();
                }
            });
            return rootView;
        }

        @Override
        public void onTrackSelectionChanged(
                boolean isDisabled, Map<TrackGroup, TrackSelectionOverride> overrides) {
            this.isDisabled = isDisabled;
            this.overrides = overrides;
        }

        public void overrideFonts(final View v) {
            try {
                if (v instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) v;
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View child = vg.getChildAt(i);
                        overrideFonts(child);
                    }
                } else if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                    ((TextView) v).setTypeface(Typeface.createFromAsset(requireActivity().getAssets(), "fonts/droid.ttf"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}