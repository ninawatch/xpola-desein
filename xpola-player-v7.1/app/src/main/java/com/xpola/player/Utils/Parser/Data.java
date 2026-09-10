package com.xpola.player.Utils.Parser;

import androidx.annotation.NonNull;

import com.xpola.player.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data {
    public static String getK5() { return "S2"; }

    private String url = "", scheme = "", playerType = "",
            userAgent = "", title = "", drmScheme = "", drmLicense = "";
    private Map<String, String> headers = new HashMap<>();
    private List<Map<String, String>> servers = new ArrayList<>();


    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPlayerType(String playerType) {
        this.playerType = playerType;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
        if (headers != null) {
            setUserAgent(Utils.getValueString(headers, "user-agent", ""));
        }
    }

    public void setServers(List<Map<String, String>> servers) {
        this.servers = servers;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setDrmLicense(String drmLicense) {
        this.drmLicense = drmLicense;
    }

    public void setDrmScheme(String drmScheme) {
        this.drmScheme = drmScheme;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUrl() {
        return url;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getPlayerType() {
        return playerType;
    }

    public List<Map<String, String>> getServers() {
        return servers;
    }

    public String getDrmScheme() {
        return drmScheme;
    }

    public String getDrmLicense() {
        return drmLicense;
    }

    @NonNull
    @Override
    public String toString() {
        return "Data{" +
                "url='" + url + '\'' +
                ", scheme='" + scheme + '\'' +
                ", playerType='" + playerType + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", title='" + title + '\'' +
                ", drmScheme='" + drmScheme + '\'' +
                ", drmLicense='" + drmLicense + '\'' +
                ", headers=" + headers +
                ", servers=" + servers +
                '}';
    }
}
