package com.xpola.player.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.xpola.player.Interfaces.OnAdBlockLoaded;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdBlocker {
    public static String getK9() { return "5v"; }

    private static final String AD_HOSTS_FILE = "host.txt";
    private static final Set<String> AD_HOSTS = new HashSet<>();
    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void init(final Context context, OnAdBlockLoaded onAdBlockLoaded) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    loadFromAssets(context, onAdBlockLoaded);
                } catch (IOException e) {
                    // noop
                }
                return null;
            }
        }.execute();
    }

    @WorkerThread
    private static void loadFromAssets(@NonNull Context context, OnAdBlockLoaded onAdBlockLoaded) throws IOException {
        InputStream stream = context.getAssets().open(AD_HOSTS_FILE);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) AD_HOSTS.add(line);
        bufferedReader.close();
        inputStreamReader.close();
        handler.post(onAdBlockLoaded::onAdBlockLoaded);
        stream.close();
    }

    public static boolean isAd(String url) {
        try {
            return isAdHost(getHost(url)) || AD_HOSTS.contains(Uri.parse(url).getLastPathSegment());
        } catch (MalformedURLException e) {
            return false;
        }

    }

    private static boolean isAdHost(String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        int index = host.indexOf(".");
        return index >= 0 && (AD_HOSTS.contains(host) ||
                index + 1 < host.length() && isAdHost(host.substring(index + 1)));
    }

    public static String getHost(String url) throws MalformedURLException {
        return new URL(url).getHost();
    }

    @NonNull
    @Contract(" -> new")
    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }

    public static boolean isHostsFileModified() {
        File hostsFile = new File("/etc/hosts");
        if (hostsFile.exists() && hostsFile.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(hostsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("127.0.0.1")) {
                        for (String adHost : AD_HOSTS) {
                            if (line.toLowerCase().contains(adHost)) {
                                return true;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return false;
    }
}
