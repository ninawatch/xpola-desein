package com.xpola.player.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.xpola.player.Interfaces.OnItemChanged;
import com.xpola.player.R;
import com.xpola.player.Ui.Activities.Player;
import com.xpola.player.Ui.Activities.m3Player;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.AdapterVh> {


    private final Activity context;
    private List<HashMap<String, Object>> allList = new ArrayList<>();
    private OnItemChanged onItemChanged;
    private final Utils utils;

    public MainAdapter(Activity context) {
        this.context = context;
        utils = new Utils(context);

    }

    public void Config(List<HashMap<String, Object>> allList, OnItemChanged onItemChanged) {
        this.allList = allList;
        this.onItemChanged = onItemChanged;
    }

    @NonNull
    @Override
    public AdapterVh onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        return new AdapterVh(LayoutInflater.from(context).inflate(R.layout.main_item_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterVh holder, final int position) {
        try {
            final HashMap<String, Object> index = allList.get(position);
            CustomView(index, holder, position);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int getItemCount() {
        return allList != null ? allList.size() : 0;
    }

    public static class AdapterVh extends RecyclerView.ViewHolder {
        CardView card;
        TextView title, extension, url;
        ImageView logo;

        public AdapterVh(View itemView) {
            super(itemView);
            try {
                card = itemView.findViewById(R.id.card_parent);
                title = itemView.findViewById(R.id.item_name);
                url = itemView.findViewById(R.id.item_url);
                extension = itemView.findViewById(R.id.item_extension);
                logo = itemView.findViewById(R.id.item_logo);
            } catch (Exception ignored) {

            }
        }
    }

    private void CustomView(final HashMap<String, Object> index, AdapterVh holder, int position) {
        try {
            holder.title.setText(Utils.getValue(index, "name", ""));
            holder.url.setText(Utils.getValue(index, "url", ""));
            if (index.containsKey("mimeType") && !Utils.getValue(index, "mimeType", "").isEmpty()) {
                holder.extension.setText(Utils.getValue(index, "mimeType", ""));
                holder.extension.setVisibility(View.VISIBLE);
            } else {
                holder.extension.setVisibility(View.GONE);
            }
            Utils.Radius(holder.logo, 360, Color.parseColor(Utils.getValue(index, "color", "#00000000")));
            holder.logo.setClipToOutline(true);
            holder.title.setClipToOutline(true);
            Utils.Radius(holder.card, 20, Color.WHITE);
            Utils.Radius(holder.extension, 10, Color.BLACK);
            Utils.SetFocus(holder.card, 0xFFFFFFFF, Color.WHITE);
            holder.card.setOnClickListener((view) -> {
                try {
                    if (index.containsKey("isM3u") && Objects.requireNonNull(index.get("isM3u")).toString().equalsIgnoreCase("true")) {
                        context.startActivity(new Intent(context, m3Player.class)
                                .setAction(Intent.ACTION_VIEW)
                                .setData(Uri.parse(Utils.getValue(index, "url", "")))
                                .putExtra("title", Utils.getValue(index, "name", ""))
                                .putExtra("isWeb", Utils.getValue(index, "player", ""))
                                .putExtra("referer", Utils.getValue(index, "referer", ""))
                                .putExtra("user-agent", Utils.getValue(index, "user_agent", "")));
                    } else {
                        HashMap<String, Object> headers = new HashMap<>();
                        if (!Utils.getValue(index, "referer", "").isEmpty())
                            headers.put("referer", Utils.getValue(index, "referer", ""));
                        headers.put("cookie", Utils.getValue(index, "cookie", ""));
                        headers.put("user-agent", Utils.getValue(index, "user_agent", Utils.defaultUserAgent(context)));
                        headers.put("player-type", Utils.getValue(index, "player", ""));
                        // لتشغيل روابط web2exo
                        String Find = Utils.getValue(index, "cookie", "");
                        if (Find != null && Find.contains("web2exo:")) {
                            // استخراج قيمة web2exo من السلسلة Find
                            String web2exoValue = Find.split("web2exo:")[1]; // على افتراض وجود تكرار واحد فقط لـ "web2exo="
                            // وضع القيمة المستخرجة في خريطة headers
                            headers.put("find", web2exoValue);
                        } else if (Find != null && Find.contains("find:")) {
                            // استخراج قيمة web2exo من السلسلة Find
                            String web2exoValue = Find.split("find:")[1]; // على افتراض وجود تكرار واحد فقط لـ "web2exo="
                            // وضع القيمة المستخرجة في خريطة headers
                            headers.put("find", web2exoValue);
                        }

                        // Check if scheme is not empty before adding to headers
                        String schemeValue = Utils.getValue(index, "scheme", "");
                        if (!schemeValue.isEmpty()) {
                            headers.put("scheme", schemeValue);
                        }

                        // Check if license is not empty before adding to headers
                        String licenseValue = Utils.getValue(index, "license", "");
                        if (!licenseValue.isEmpty()) {
                            headers.put("license", licenseValue);
                        }

                        String authValue = Utils.getValue(index, "auth", "");
                        if (!authValue.isEmpty()) {
                            headers.put("auth", authValue);
                        }

                        Intent intent = new Intent(context, Player.class)
                                .setAction(Intent.ACTION_VIEW)
                                .setData(Uri.parse(Utils.getValue(index, "url", "")))
                                .putExtra("title", Utils.getValue(index, "name", ""))
                                .putExtra("headers", Utils.objectToString(headers));
                        context.startActivity(intent);
                    }
                    CustomIntent.customType(context, CustomIntent.LEFT_TO_RIGHT);
                } catch (Exception ignored) {
                }
            });
            holder.card.setOnLongClickListener((view) -> {
                Dialogs dialogs = new Dialogs(context);
                dialogs.ShowOptions(holder.card, position, onItemChanged);
                return true;
            });
        } catch (Exception e) {
            utils.showToast(e.getMessage());
        }
    }

}


