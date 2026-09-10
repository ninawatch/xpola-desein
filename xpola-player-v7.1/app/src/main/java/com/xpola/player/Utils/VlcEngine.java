package com.xpola.player.Utils;

import android.content.Context;
import android.net.Uri;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.Map;

public class VlcEngine {
    private static VlcEngine instance;
    private LibVLC mLibVLC;
    private MediaPlayer mMediaPlayer;

    private VlcEngine() {}

    public static synchronized VlcEngine getInstance() {
        if (instance == null) {
            instance = new VlcEngine();
        }
        return instance;
    }

    public synchronized void init(Context context) {
        if (mLibVLC == null) {
            ArrayList<String> options = new ArrayList<>();
            options.add("--no-drop-late-frames");
            options.add("--no-skip-frames");
            options.add("--rtsp-tcp");
            options.add("--clock-jitter=0"); // Audio/video sync improvement
            options.add("-vvv"); // Debug

            mLibVLC = new LibVLC(context.getApplicationContext(), options);
        }
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer(mLibVLC);
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public LibVLC getLibVLC() {
        return mLibVLC;
    }

    public synchronized void playMedia(Context context, String url, Map<String, String> headers) {
        if (mLibVLC == null || mMediaPlayer == null) {
            init(context);
        }

        mMediaPlayer.stop();

        Media media = new Media(mLibVLC, Uri.parse(url));

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String value = entry.getValue();
                if (key.equals("user-agent")) {
                    media.addOption(":http-user-agent=" + value);
                } else if (key.equals("referer")) {
                    media.addOption(":http-referrer=" + value);
                }
            }
        }

        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=1500");
        media.addOption(":clock-jitter=0");
        media.addOption(":clock-synchro=0");

        mMediaPlayer.setMedia(media);
        media.release();

        mMediaPlayer.play();
    }

    public synchronized void stopAndRelease() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            // Don't release to avoid constant recreation, unless absolutely necessary.
            // If the app completely closes, we can release.
        }
    }

    public synchronized void destroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
    }
}
