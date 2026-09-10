package com.xpola.player.Connections;

import android.os.Handler;
import android.os.Looper;

import com.xpola.player.Interfaces.INet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

public class NetRequests {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private INet inet;
    private String url = "";

    public NetRequests() {
    }

    public void get(String url, INet inet) {
        this.inet = inet;
        this.url = url;
        getData(null);
    }

    public void get(String url, Map<String, String> headers, INet inet) {
        this.inet = inet;
        this.url = url;
        getData(headers);
    }

    private void getData(Map<String, String> headers) {
        new Thread(() -> {
            try {
                InputStream stream = getInputStream(url, headers);
                final StringBuilder sb = new StringBuilder();
                Scanner scanner = new Scanner(stream, "UTF-8");
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine());
                }
                String resp = sb.toString().replaceAll("\\s+", " ").trim();
                if (resp.isEmpty()) {
                    postError("No Data");
                } else {
                    postResponse(resp);
                }
            } catch (Exception e) {
                postError(e.getMessage());
            }
        }).start();

    }

    public static InputStream getInputStream(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        HttpURLConnection.setFollowRedirects(true);
        con.setInstanceFollowRedirects(true);
        con.setUseCaches(false);
        con.setConnectTimeout(20000);
        con.setReadTimeout(30000);
        con.connect();
        return con.getInputStream();
    }

    private void postError(String err) {
        handler.post(() -> inet.onFailed(err, url));
    }

    private void postResponse(String data) {
        handler.post(() -> inet.onSuccess(data, url));
    }

}
