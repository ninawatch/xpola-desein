package com.xpola.player.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.xpola.player.Ui.Fragments.Content;

import java.util.HashMap;
import java.util.List;

public class PagerAdapter extends FragmentStateAdapter {
    private int size = 0;
    private List<HashMap<String, Object>> list;

    public PagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    public PagerAdapter init(int size) {
        this.size = size;
        return this;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new Content();
    }

    @Override
    public int getItemCount() {
        return size;
    }
}
