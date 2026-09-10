package com.xpola.player.Connections;

import androidx.annotation.NonNull;

import com.xpola.player.Interfaces.OnResponseListener;
import com.xpola.player.Utils.Utils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpRequest {
    private final Map<String, String> headers;
    private final String url;

    private HttpRequest(String url, Map<String, String> headers) {
        this.headers = headers;
        this.url = url;
    }

    @NonNull
    public static HttpRequest get(String url, Map<String, String> headers) {
        return new HttpRequest(url, headers);
    }


    public void getData(OnResponseListener onResponseListener) {
        try {
            OkHttpClient.Builder client = new OkHttpClient.Builder();

            client.followRedirects(true);
            client.callTimeout(60 * 1000, TimeUnit.MILLISECONDS);
            client.readTimeout(120 * 1000, TimeUnit.MILLISECONDS);
            client.hostnameVerifier((hostname, session) -> true);

            Request.Builder request = new Request.Builder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .url(url);

            request.addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "okhttp/4.12.0");

            if (!headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (!key.isEmpty()) {
                        request.addHeader(key, String.valueOf(header.getValue()));
                    }
                }
            }

            client.retryOnConnectionFailure(true);
            client.build().newCall(request.build()).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Utils.handler.post(() -> onResponseListener.onResponseListener("", ""));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        ResponseBody body = response.body();
                        String text = body != null ? body.string() : "";
                        String t = response.header("t");
                        Utils.handler.post(() -> onResponseListener.onResponseListener(text, t));

                    } catch (Throwable ignored) {
                        Utils.handler.post(() -> onResponseListener.onResponseListener("", ""));
                    }
                }
            });
        } catch (Throwable ignored) {
            Utils.handler.post(() -> onResponseListener.onResponseListener("", ""));
        }

    }

    public String String() {
        try {
            OkHttpClient.Builder client = new OkHttpClient.Builder();
            client.followRedirects(true);
            client.callTimeout(60 * 1000, TimeUnit.MILLISECONDS);
            client.readTimeout(120 * 1000, TimeUnit.MILLISECONDS);
            client.hostnameVerifier((hostname, session) -> true);

            Request.Builder request = new Request.Builder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .url(url);


            if (!headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (!key.isEmpty()) {
                        request.addHeader(key, String.valueOf(header.getValue()));
                    }
                }
            }
            client.retryOnConnectionFailure(true);
            Response response = client.build().newCall(request.build()).execute();
            ResponseBody body = response.body();
            String s = body != null ? body.string() : "";
            response.close();
            return s;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return "";
    }
}
