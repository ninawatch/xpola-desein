package com.xpola.player.Utils;


import android.net.Uri;
import android.view.View;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerSaver {
    private static PlayerSaver playerSaver;
    private ExoPlayer player;
    private Uri uri;
    private int position = 0;
    private int serverSelected = 0;

    private String userAgent = "";
    private final List<Map<String, String>> servers = new ArrayList<>();
    private Map<String, String> headers = new HashMap<>();
    private DefaultTrackSelector trackSelector;
    private TrackSelectionParameters trackSelectorParameters;
    private WindowManager window;
    private View view;

    public static synchronized PlayerSaver getInstance() {
        if (playerSaver == null) playerSaver = new PlayerSaver();
        return playerSaver;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setServers(List<Map<String, String>> servers) {
        this.servers.clear();
        this.servers.addAll(servers);
    }

    public void setServerSelected(int serverSelected) {
        this.serverSelected = serverSelected;
    }

    public Uri getUri() {
        return this.uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int position() {
        return position;
    }


    public String getUserAgent() {
        return userAgent;
    }

    public void setPosition(int position) {
        this.position = position;
    }


    public void setTrackSelector(DefaultTrackSelector trackSelector) {
        this.trackSelector = trackSelector;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public DefaultTrackSelector TrackSelector() {
        return trackSelector;
    }

    public ExoPlayer Player() {
        return player;
    }


    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    public List<Map<String, String>> getServers() {
        return servers;
    }

    public int getServerSelected() {
        return serverSelected;
    }

    public void clear() {
        setPlayer(null);
        setParameters(null);
        setTrackSelector(null);
        setServers(new ArrayList<>());
        setPosition(0);
        setView(null);
        setWindow(null);
    }

    public int getPosition() {
        return position;
    }

    public WindowManager Window() {
        return window;
    }

    public void setWindow(WindowManager window) {
        this.window = window;
    }

    public void setView(View v) {
        this.view = v;
    }

    public View View() {
        return view;
    }

    public TrackSelectionParameters trackSelectorParameters() {
        return trackSelectorParameters;
    }

    public void setParameters(TrackSelectionParameters parameters) {
        this.trackSelectorParameters = parameters;
    }
}