package com.xpola.player.Ui.Activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.xpola.player.R;
import com.xpola.player.Utils.CustomIntent;
import com.xpola.player.Utils.Dialogs;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;
import com.xpola.player.Utils.NpvChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AddLink extends AppCompatActivity {
    private TextInputEditText name, link, user_agent, ref, auth;
    private Button saver;


    private ImageView back;
    private Intent intent;
    private CheckBox exo, web, forceM3u;
    private boolean isWebPlayer = false, isM3u = false;
    private Prefs prefs;
    private List<Map<String, String>> list = new ArrayList<>();
    private String color = "";
    private AlertDialog dialog;
    private CheckBox webBrowser;
    private TextInputEditText license;
    private TextInputEditText cookie;
    private AutoCompleteTextView schemeDropdown;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_link);
        prefs = new Prefs(this);
        dialog = new AlertDialog.Builder(this).create();
        name = findViewById(R.id.addlinks_name);
        link = findViewById(R.id.addlinks_link);
        user_agent = findViewById(R.id.addlinks_user_agent);
        ref = findViewById(R.id.addlinks_ref);
        auth = findViewById(R.id.addlinks_auth);
        cookie = findViewById(R.id.addlinks_cookie);
        license = findViewById(R.id.addlinks_license); // ربط متغير license بالـ XML
        saver = findViewById(R.id.addlinks_saver);
        back = findViewById(R.id.addlinks_back);
        exo = findViewById(R.id.check_exo_player);
        web = findViewById(R.id.check_web_player);
        forceM3u = findViewById(R.id.force_m3u);
        webBrowser = findViewById(R.id.check_web_browser); // ربط متغير webBrowser بالـ XML
        schemeDropdown = findViewById(R.id.addlinks_scheme); // ربط متغير schemeDropdown بالـ XML
        Utils.handler.post(new NpvChecker(prefs, stop -> {
            if (stop) {
                Dialogs.DisableVPN(this, dialog);
            }
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            intent = getIntent();
            list = Utils.getListString(prefs.getString("list", "[]"));
            checkForItems();
            AddItem(new EditText[]{name, link, user_agent, ref, auth});
            Utils.Radius(saver, 10, Color.WHITE);
            Utils.SetFocus(saver, Color.WHITE, Color.WHITE);
            Utils.SetFocus(exo, Color.TRANSPARENT, Color.TRANSPARENT);
            Utils.SetFocus(web, Color.TRANSPARENT, Color.TRANSPARENT);
            Utils.SetFocus(forceM3u, Color.TRANSPARENT, Color.TRANSPARENT);
            back.setOnClickListener(v -> {
                Utils.Radius(v, 0, Color.TRANSPARENT);
                onBackPressed();
            });

            // إعداد القائمة المنسدلة
            AutoCompleteTextView schemeDropdown = findViewById(R.id.addlinks_scheme);

            // القيم التي ستظهر في القائمة المنسدلة
            String[] schemes = new String[]{"clearkey", "widevine", "playready"};

            // إعداد الـ ArrayAdapter وتحديد التصميم الافتراضي لقائمة الخيارات
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, schemes);

            // ربط الـ adapter بالـ AutoCompleteTextView
            schemeDropdown.setAdapter(adapter);

        } catch (Throwable ignored) {
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void checkForItems() {
        forceM3u.setChecked(false);
        try {
            if (intent.hasExtra("position") && intent.getIntExtra("position", -1) > -1) {
                Map<String, String> index = list.get(intent.getIntExtra("position", -1));
                name.setText(index.get("name"));
                String u = Utils.getValueString(index, "url", "");
                if (!Utils.isUrl(u) && !Utils.isArray(u))
                    u = Utils.fromBase64(u);
                link.setText(u);
                color = index.containsKey("color") ? index.get("color") : Utils.ColorString();
                isM3u = index.containsKey("isM3u") && Objects.requireNonNull(index.get("isM3u")).equalsIgnoreCase("true");
                forceM3u.setChecked(isM3u);
                user_agent.setText(index.get("user_agent"));
                cookie.setText(index.containsKey("cookie") ? index.get("cookie") : "");
                // تعيين حالة الـ CheckBox بناءً على البيانات
                if (index.containsKey("player")) {
                    String player = index.get("player");
                    if (player != null && player.equals("browser")) {
                        webBrowser.setChecked(true);
                        exo.setChecked(false);
                        web.setChecked(false);
                        forceM3u.setChecked(false);
                        isWebPlayer = false;
                        isM3u = false;
                    } else if (player != null && player.equals("webplayer")) {
                        web.setChecked(true);
                        exo.setChecked(false);
                        webBrowser.setChecked(false);
                        forceM3u.setChecked(false);
                        isWebPlayer = true;
                        isM3u = false;
                    } else if (player != null && player.equals("exoplayer")) {
                        exo.setChecked(true);
                        web.setChecked(false);
                        webBrowser.setChecked(false);
                        forceM3u.setChecked(isM3u); // الحفاظ على حالة M3U
                        isWebPlayer = false;
                    }
                }
                ref.setText(index.containsKey("referer") ? index.get("referer") : "");
                auth.setText(index.containsKey("auth") ? index.get("auth") : "");

                // تعيين قيم الحقول license و scheme
                if (index.containsKey("license")) {
                    license.setText(index.get("license"));
                }
                if (index.containsKey("scheme")) {
                    schemeDropdown.setText(index.get("scheme"));
                }
            }
        } catch (Exception ignored) {
        }
    }


    private void AddItem(@NonNull EditText[] edt) {
        for (EditText et : edt) {
            et.setOnFocusChangeListener((v, isFocused) -> {
                name.setSelection(0);
                link.setSelection(0);
                user_agent.setSelection(0);
                ref.setSelection(0);
                auth.setSelection(0);
            });
        }

        link.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (Utils.isM3u(charSequence.toString())) {
                    forceM3u.setChecked(true);
                    isM3u = true;
                    exo.setChecked(true); // تلقائيًا تفعيل ExoPlayer عند تفعيل M3U
                    web.setChecked(false);
                    webBrowser.setChecked(false);
                } else {
                    // لا تتدخل في حالة ExoPlayer إذا لم يكن هناك M3U
                    isM3u = false;
                    if (!exo.isChecked()) {
                        forceM3u.setChecked(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        forceM3u.setOnClickListener(v -> {
            Utils.Radius(forceM3u, 10, Color.TRANSPARENT);
            if (forceM3u.isChecked()) {
                isM3u = true;
                exo.setChecked(true); // تلقائيًا تفعيل ExoPlayer عند تفعيل M3U
                web.setChecked(false);
                webBrowser.setChecked(false);
            } else {
                isM3u = false;
                // لا تتدخل في حالة ExoPlayer إذا تم إلغاء تفعيل M3U
            }
        });

        exo.setOnClickListener(v -> {
            Utils.Radius(exo, 10, Color.TRANSPARENT);
            if (exo.isChecked()) {
                isWebPlayer = false;
                web.setChecked(false);
                webBrowser.setChecked(false);
                // إذا كان M3U مفعلًا، تأكد من إبقاء ExoPlayer مفعلًا
                if (isM3u) {
                    forceM3u.setChecked(true);
                }
            } else {
                // إذا تم إلغاء تفعيل ExoPlayer، قم بإلغاء تفعيل M3U إذا لم يكن هناك خيارات أخرى مفعلة
                if (isM3u && !web.isChecked() && !webBrowser.isChecked()) {
                    forceM3u.setChecked(false);
                    isM3u = false;
                }
            }
        });

        web.setOnClickListener(v -> {
            Utils.Radius(web, 10, Color.TRANSPARENT);
            if (web.isChecked()) {
                isWebPlayer = true;
                exo.setChecked(false);
                webBrowser.setChecked(false);
                // إذا تم تفعيل WebPlayer، قم بإلغاء تفعيل M3U
                if (isM3u) {
                    forceM3u.setChecked(false);
                    isM3u = false;
                }
            } else {
                // إذا تم إلغاء تفعيل WebPlayer، تحقق من حالة ExoPlayer
                if (!exo.isChecked()) {
                    isWebPlayer = false;
                }
            }
        });

        webBrowser.setOnClickListener(v -> {
            Utils.Radius(webBrowser, 10, Color.TRANSPARENT);
            if (webBrowser.isChecked()) {
                exo.setChecked(false);
                web.setChecked(false);
                forceM3u.setChecked(false);
                isWebPlayer = false;
                isM3u = false;
            }
        });

        saver.setOnClickListener(v -> {
            if (Objects.requireNonNull(name.getText()).toString().trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), getString(R.string.add_name), Toast.LENGTH_SHORT).show();
                return;
            }
            if (Objects.requireNonNull(link.getText()).toString().trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), getString(R.string.add_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (color.isEmpty()) color = Utils.ColorString();
            String fullUrl = link.getText().toString();

            String ext = Utils.getExtension(link.getText().toString()).trim().toUpperCase();
            Map<String, String> index = new HashMap<>();
            index.put("name", name.getText().toString().trim().toUpperCase());
            index.put("url", Utils.isArray(fullUrl) ? Utils.toBase64(fullUrl) : fullUrl);
            if (fullUrl.startsWith("http")) {
                if (fullUrl.contains("|drmScheme=") && fullUrl.contains("&drmLicense=")) {
                    String drmScheme = "";
                    String drmLicense = "";
                    // استخراج جزء url قبل |drmScheme
                    String[] parts = fullUrl.split("\\|drmScheme=");
                    String baseUrl = parts[0];  // الجزء الأساسي من الرابط (url)
                    // استخراج drmScheme و drmLicense من الجزء الثاني
                    String drmPart = parts[1];
                    String[] drmParams = drmPart.split("&drmLicense=");
                    drmScheme = drmParams[0];  // drmScheme
                    drmLicense = drmParams[1];  // drmLicense
                    // تحديث القيم في index
                    index.put("url", baseUrl.trim());
                    index.put("scheme", drmScheme.trim());
                    index.put("license", drmLicense.trim());
                }
            }

            index.put("isM3u", String.valueOf(isM3u));
            index.put("mimeType", ext.isEmpty() && isM3u ? "M3U" : ext);
            index.put("user_agent", Utils.objectToString(user_agent.getText()));
            index.put("referer", Utils.objectToString(ref.getText()));
            index.put("auth", Utils.objectToString(auth.getText()));
            index.put("cookie", Utils.objectToString(cookie.getText()));
            index.put("color", color);

            if (webBrowser.isChecked()) {
                index.put("player", "browser");
            } else if (isWebPlayer) {
                index.put("player", "webplayer");
            } else {
                index.put("player", "exoplayer");
            }

            String licenseValue = Objects.requireNonNull(license.getText()).toString().trim();
            String schemeValue = Objects.requireNonNull(schemeDropdown.getText()).toString().trim();

            if (!licenseValue.isEmpty()) {
                index.put("license", licenseValue);
                index.put("scheme", schemeValue);
            }

            if (intent.hasExtra("position") && intent.getIntExtra("position", -1) > -1) {
                list.set(intent.getIntExtra("position", -1), index);
            } else {
                list.add(index);
            }

            prefs.setString("list", new Gson().toJson(list));
            name.setText("");
            link.setText("");
            user_agent.setText("");
            auth.setText("");
            finish();
        });
    }


    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(AddLink.this, CustomIntent.RIGHT_TO_LEFT);
    }
}
