package com.xpola.player.Ui.Fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xpola.player.Adapters.MainAdapter;
import com.xpola.player.R;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinksFragment extends Fragment {
    private RecyclerView recycler;
    private GridLayoutManager gridLayoutManager = null;
    private MainAdapter adapter;
    private List<HashMap<String, Object>> allList = new ArrayList<>(), toSearch = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.links_fragment, container, false);
        initializeLogic(view);
        return view;
    }

    private void initializeLogic(View view) {
        prefs = new Prefs(requireActivity());
        recycler = view.findViewById(R.id.main_list);
        adapter = new MainAdapter(requireActivity());
        refreshData();
    }

    @Override
    public void onResume() {
        refreshData();
        super.onResume();
    }

    private void refreshData() {
        try {
            recycler.setVisibility(View.GONE);
            String listString = prefs.getString("list", "[]");
            allList = Utils.getListObject(listString);
            if (allList == null) allList = new ArrayList<>();
            toSearch = Utils.getListObject(listString);
            if (toSearch == null) toSearch = new ArrayList<>();
            if (!allList.isEmpty()) {
                recycler.setVisibility(View.VISIBLE);
            }
            adapter.Config(allList, (item, i, v) -> refreshData());
            if (gridLayoutManager == null) {
                gridLayoutManager = new GridLayoutManager(requireActivity(), 1);
                recycler.setLayoutManager(gridLayoutManager);
            }
            if (recycler.getAdapter() == null) recycler.setAdapter(adapter);
            notifyChanged();
        } catch (Exception ignored) {
            allList = new ArrayList<>();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyChanged() {
        if (recycler.getAdapter() != null) {
            recycler.getAdapter().notifyDataSetChanged();
        }
    }

    public void search(String newText) {
        if (newText.trim().isEmpty()) {
            refreshData();
        } else {
            allList.clear();
        }
        executor.execute(() -> {
            for (HashMap<String, Object> index : toSearch) {
                if (index.containsKey("name") && Objects.requireNonNull(index.get("name")).toString().toLowerCase().replaceAll("\\+", " ").contains(newText.toLowerCase().replaceAll("\\+", " "))) {
                    if (!allList.contains(index)) allList.add(index);
                }
            }
            handler.post(this::notifyChanged);
        });
    }
}
