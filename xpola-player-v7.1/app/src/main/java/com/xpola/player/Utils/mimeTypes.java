package com.xpola.player.Utils;

import com.google.android.exoplayer2.util.MimeTypes;

public class mimeTypes {

    static String[] Hls = {".m3u8", ".m3u", ".ts", ".php"},
            Dash = {".mpd"},
            Mp4 = {".mp4", ".mp3", ".3gp"},
            Mkv = {".mkv"},
            Web = {".webcam", "webm"};

    public static String getFrom(String url) {
        url = url.toLowerCase();
        for (String hls : Hls) {
            if (url.contains(hls.toLowerCase())) {
                return MimeTypes.APPLICATION_M3U8;
            }
        }
        for (String dash : Dash) {
            if (url.contains(dash.toLowerCase())) {
                return MimeTypes.APPLICATION_MPD;
            }
        }
        for (String mp4 : Mp4) {
            if (url.contains(mp4.toLowerCase())) {
                return MimeTypes.APPLICATION_MP4;
            }
        }
        for (String mkv : Mkv) {
            if (url.contains(mkv.toLowerCase())) {
                return MimeTypes.APPLICATION_MATROSKA;
            }
        }
        for (String webm : Web) {
            if (url.contains(webm.toLowerCase())) {
                return MimeTypes.APPLICATION_WEBM;
            }
        }
        return MimeTypes.APPLICATION_M3U8;
    }
}
