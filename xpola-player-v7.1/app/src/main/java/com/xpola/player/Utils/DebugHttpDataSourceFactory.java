package com.xpola.player.Utils;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugHttpDataSourceFactory implements HttpDataSource.Factory {

    private final DefaultHttpDataSource.Factory baseFactory;
    private final Map<String, String> defaultHeaders = new HashMap<>();

    public DebugHttpDataSourceFactory(Map<String, String> headers, String userAgent) {

        if (headers != null) {
            defaultHeaders.putAll(headers);
        }

        baseFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(defaultHeaders);
    }

    @Override
    public HttpDataSource createDataSource() {
        HttpDataSource base = baseFactory.createDataSource();
        return new DebugHttpDataSource(base, defaultHeaders);
    }

    /** ⬇⬇ required by the Factory interface */
    @Override
    public HttpDataSource.Factory setDefaultRequestProperties(Map<String, String> requestProperties) {

        if (requestProperties != null) {
            defaultHeaders.putAll(requestProperties);
            baseFactory.setDefaultRequestProperties(defaultHeaders);
        }

        return this;
    }
    /** ⬆⬆ **/

    private static class DebugHttpDataSource implements HttpDataSource {

        private final HttpDataSource base;
        private final Map<String, String> headers;

        DebugHttpDataSource(HttpDataSource base, Map<String, String> headers) {
            this.base = base;
            this.headers = headers;
        }

        @Override
        public long open(DataSpec dataSpec) throws HttpDataSourceException {

            Log.e("HTTP_DEBUG", "--------------------------------------------------");
            Log.e("HTTP_DEBUG", "REQUEST URL = " + dataSpec.uri);
            Log.e("HTTP_DEBUG", "HEADERS SENT:");

            for (String k : headers.keySet())
                Log.e("HTTP_DEBUG", k + " : " + headers.get(k));

            try {
                long result = base.open(dataSpec);

                Log.e("HTTP_DEBUG", "RESPONSE CODE = " + safeResponseCode());
                Log.e("HTTP_DEBUG", "RESPONSE HEADERS = " + base.getResponseHeaders());
                Log.e("HTTP_DEBUG", "--------------------------------------------------");

                return result;

            } catch (Exception e) {
                Log.e("HTTP_DEBUG", "ERROR = " + e.getMessage());
                throw e;
            }
        }

        private int safeResponseCode() {
            try { return base.getResponseCode(); }
            catch (Exception e) { return -1; }
        }

        @Override public Uri getUri() { return base.getUri(); }
        @Override public int getResponseCode() { return base.getResponseCode(); }
        @Override public Map<String, List<String>> getResponseHeaders() { return base.getResponseHeaders(); }
        @Override public void close() throws HttpDataSourceException { base.close(); }

        @Override
        public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
            return base.read(buffer, offset, readLength);
        }

        @Override public void setRequestProperty(String name, String value) { base.setRequestProperty(name, value); }
        @Override public void clearRequestProperty(String name) { base.clearRequestProperty(name); }
        @Override public void clearAllRequestProperties() { base.clearAllRequestProperties(); }
        @Override public void addTransferListener(com.google.android.exoplayer2.upstream.TransferListener transferListener)
        { base.addTransferListener(transferListener); }
    }
}
