package com.xpola.player.Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xpola.player.Adapters.IptvAdapter;
import com.xpola.player.Interfaces.OnItemChanged;
import com.xpola.player.R;
import com.xpola.player.Ui.Activities.AddLink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class Dialogs {
    private final Context ctx;
    private final Utils util;

    private List<HashMap<String, Object>> list = new ArrayList<>();
    private final Prefs prefs;

    public Dialogs(Context ctx) {
        this.ctx = ctx;
        this.prefs = new Prefs(ctx);
        this.util = new Utils(ctx);
    }

    public void ShowOptions(View itemClicked, int position, OnItemChanged onItemChanged) {
        try {
            list = Utils.getListObject(prefs.getString("list", "[]"));
            if (list == null) list = new ArrayList<>();
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_options, null);
            final PopupWindow dialog = new PopupWindow();

            CardView delete = v.findViewById(R.id.dialog_options_delete),
                    edit = v.findViewById(R.id.dialog_options_edit),
                    close = v.findViewById(R.id.dialog_options_close);


            dialog.setContentView(v);
           /* Utils.Radius(delete, 20, Color.WHITE);
            Utils.Radius(edit, 20, Color.WHITE);
            Utils.Radius(close, 20, Color.WHITE);
            Utils.SetFocus(delete, Color.WHITE, Color.WHITE);
            Utils.SetFocus(edit, Color.WHITE, Color.WHITE);
            Utils.SetFocus(close, Color.WHITE, Color.WHITE);*/
            dialog.setWidth((int) util.getDip(300));
            dialog.setHeight(-2);
            dialog.setOutsideTouchable(true);
            dialog.setFocusable(true);
            dialog.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.showAtLocation(itemClicked, Gravity.CENTER, 0, 0);
            close.setOnClickListener(v13 -> {
                Utils.Radius(v13, 20, 0xFF66658D);
                dialog.dismiss();
            });
            delete.setOnClickListener(v1 -> {
                Utils.Radius(v1, 20, 0xFF66658D);
                if (list != null && position < list.size()) list.remove(position);
                dialog.dismiss();
                prefs.setString("list", Utils.objectToString(list));
                onItemChanged.onItemChanged("", position, v1);
            });
            edit.setOnClickListener(v12 -> {
                Utils.Radius(v12, 20, 0xFF66658D);
                dialog.dismiss();
                try {
                    ctx.startActivity(new Intent(ctx, AddLink.class)
                            .putExtra("position", position)
                            .putExtra("list", Utils.objectToString(list)));
                    CustomIntent.customType(ctx, CustomIntent.LEFT_TO_RIGHT);
                } catch (Exception ignored) {

                }
            });
            dialog.setOnDismissListener(() -> {
            });
        } catch (Exception e) {
            util.showToast(e.getMessage());
        }
    }

    public static void privacy(Activity ctx, String text) {
        AlertDialog builder = new AlertDialog.Builder(ctx).create();
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.simple_dialog, null);
        TextView title = view.findViewById(R.id.dialog_title),
                message = view.findViewById(R.id.dialog_message);
        Button ok = view.findViewById(R.id.dialog_button_ok),
                cancel = view.findViewById(R.id.dialog_button_cancel);
        Utils.SetFocus(ok, Color.TRANSPARENT, Color.TRANSPARENT);
        cancel.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
        title.setText(R.string.kayan_privacy);
        try {
            Objects.requireNonNull(builder.getWindow()).setWindowAnimations(R.style.Theme_AGH_DEV_DIALOG);
        } catch (Exception ignored) {

        }
        try {
            Objects.requireNonNull(builder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } catch (Exception ignored) {
        }
        builder.setView(view);
        message.setText(R.string.wait);
        builder.show();
        ok.setOnClickListener(view1 -> builder.dismiss());
        message.setText(text);
    }

    public static void Prepare(Activity ctx, boolean prepare, String msg) {
        if (prepare) {
            AlertDialog builder = new AlertDialog.Builder(ctx).create();
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.simple_dialog, null);
            TextView title = view.findViewById(R.id.dialog_title),
                    message = view.findViewById(R.id.dialog_message);
            Button ok = view.findViewById(R.id.dialog_button_ok),
                    cancel = view.findViewById(R.id.dialog_button_cancel);
            Utils.SetFocus(ok, Color.TRANSPARENT, Color.TRANSPARENT);
            Utils.SetFocus(cancel, Color.TRANSPARENT, Color.TRANSPARENT);
            title.setText(R.string.warning);
            builder.setCancelable(false);
            message.setText(msg);
            try {
                Objects.requireNonNull(builder.getWindow()).setWindowAnimations(R.style.Theme_AGH_DEV_DIALOG);
            } catch (Exception ignored) {
            }
            try {
                Objects.requireNonNull(builder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } catch (Exception ignored) {
            }
            builder.setView(view);
            builder.show();
            ok.setOnClickListener(view1 -> ctx.finishAffinity());
        }
    }

    public static void update(Activity ctx, @NonNull String version, String msg, String url, boolean forceDownload) {
        if (version.isEmpty()) return;
        AlertDialog builder = new AlertDialog.Builder(ctx).create();
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.simple_dialog, null);
        TextView title = view.findViewById(R.id.dialog_title),
                message = view.findViewById(R.id.dialog_message);
        Button ok = view.findViewById(R.id.dialog_button_ok),
                cancel = view.findViewById(R.id.dialog_button_cancel);
        ok.setText(R.string.kayan_download);
        Utils.SetFocus(ok, Color.TRANSPARENT, Color.TRANSPARENT);
        Utils.SetFocus(cancel, Color.TRANSPARENT, Color.TRANSPARENT);
        title.setText(R.string.new_update_available);
        message.setText(msg);
        try {
            Objects.requireNonNull(builder.getWindow()).setWindowAnimations(R.style.Theme_AGH_DEV_DIALOG);
        } catch (Exception ignored) {
        }
        try {
            Objects.requireNonNull(builder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } catch (Exception ignored) {
        }
        builder.setView(view);
        if (!Constants.version.equalsIgnoreCase(version)) {
            builder.setCancelable(!forceDownload);
            if (!builder.isShowing()) builder.show();
        }
        if (!forceDownload) {
            cancel.setVisibility(View.VISIBLE);
            cancel.setOnClickListener(view12 -> builder.dismiss());
        }
        ok.setOnClickListener(view1 -> {
            try {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
            } catch (Exception ignored) {
            }
        });
    }

    public static void AllChannels(@NonNull Context ctx, @NonNull BottomSheetDialog bottomSheetDialog,
                                   List<HashMap<String, Object>> list) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_items, null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setOnDismissListener(dialogInterface -> {

        });
        List<HashMap<String, Object>> list1 = new ArrayList<>(Utils.removeAds(list));
        List<HashMap<String, Object>> search = new ArrayList<>(Utils.removeAds(list));
        ImageView close = view.findViewById(R.id.dialog_close);
        SearchView searchView = view.findViewById(R.id.dialog_search);
        searchView.onActionViewExpanded();
        searchView.setFocusable(true);
        RecyclerView channels = view.findViewById(R.id.list);
        IptvAdapter catsAdapter = new IptvAdapter(ctx);

        catsAdapter.setList(list1);
        int numCulmn = (int) Math.floor((float) ctx.getResources().getDisplayMetrics().widthPixels / ctx.getResources().getDimensionPixelSize(R.dimen.channel_item_width));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(ctx, numCulmn);
        channels.setLayoutManager(gridLayoutManager);
        channels.setAdapter(catsAdapter);
        ((View) view.getParent()).setBackgroundColor(Color.TRANSPARENT);
        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.getBehavior().setMaxWidth(ctx.getResources().getDisplayMetrics().widthPixels);
        bottomSheetDialog.show();
        searchView.requestFocus();
        EditText text = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        text.setHintTextColor(0xFFDCDCDC);
        text.setTextColor(Color.WHITE);
        Utils.SetFocus(close, Color.TRANSPARENT, Color.TRANSPARENT);
        close.setOnClickListener(v -> bottomSheetDialog.dismiss());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public boolean onQueryTextChange(String newText) {
                list1.clear();
                for (HashMap<String, Object> index : search) {
                    if (Utils.getValue(index, "name", "")
                            .toLowerCase().contains(newText.toLowerCase())) {
                        list1.add(index);
                    }
                }
                catsAdapter.notifyDataSetChanged();
                return true;
            }
        });
    }


    public static void DisableVPN(Activity activity, AlertDialog builder) {
        try {
            View view = LayoutInflater.from(activity).inflate(R.layout.vpn_dialog, null);
            TextView message = view.findViewById(R.id.app_description),
                    appName = view.findViewById(R.id.app_name);
            ImageView appPic = view.findViewById(R.id.remove_app_icon);
            Button closer = view.findViewById(R.id.closer);
            appPic.setImageResource(R.drawable.ic_vpn);
            message.setText(R.string.disable_vpn);
            appName.setText(R.string.vpn);
            if (builder != null && !builder.isShowing()) {
                try {
                    Objects.requireNonNull(builder.getWindow()).setWindowAnimations(R.style.Theme_AGH_DEV_DIALOG);
                } catch (Exception ignored) {
                }
                try {
                    Objects.requireNonNull(builder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                } catch (Exception ignored) {
                }
                builder.setView(view);
                builder.setCancelable(false);
                builder.show();
            }
            closer.setOnClickListener(view1 -> activity.finishAffinity());
        } catch (Exception ignored) {
        }

    }


}
