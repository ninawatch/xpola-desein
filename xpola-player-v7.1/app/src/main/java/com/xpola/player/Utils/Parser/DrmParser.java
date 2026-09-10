package com.xpola.player.Utils.Parser;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.LocalMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Util;
import com.xpola.player.Utils.Utils;

import java.util.UUID;

public class DrmParser {


    @NonNull
    private static String getKey(@NonNull String key) {
        // إذا كانت القيمة أصلاً بصيغة JSON جاهز، نعيدها كما هي
        if (Utils.isObject(key)) {
            return key;
        }

        // نقسم لو عندنا أكثر من مفتاح
        String[] keyPairs = key.split(",");
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"keys\":[");

        for (int i = 0; i < keyPairs.length; i++) {
            String pair = keyPairs[i].trim();
            int index = pair.indexOf(":");
            if (index > -1) {
                String keyId = pair.substring(0, index);
                String keyValue = pair.substring(index + 1);
                jsonBuilder.append("{");
                jsonBuilder.append("\"kty\":\"oct\",");
                jsonBuilder.append("\"k\":\"").append(Utils.isHex(keyValue) ? Utils.toUrlBase64(Utils.hexToBytes(keyValue)) : keyValue).append("\",");
                jsonBuilder.append("\"kid\":\"").append(Utils.isHex(keyId) ? Utils.toUrlBase64(Utils.hexToBytes(keyId)) : keyId).append("\"");
                jsonBuilder.append("}");
                if (i != keyPairs.length - 1) {
                    jsonBuilder.append(",");
                }
            }
        }

        jsonBuilder.append("],\"type\":\"temporary\"}");

        return jsonBuilder.toString();
    }

    public static DrmSessionManager getSessionManager(String license, String scheme, DataSource.Factory factory) {
        DrmSessionManager manager = null;
        UUID UScheme = Util.getDrmUuid(scheme);
        if (UScheme != null) {
            if (!license.startsWith("http")) {
                LocalMediaDrmCallback drmCallback = new LocalMediaDrmCallback(getKey(license).getBytes());
                manager = new DefaultDrmSessionManager.Builder().setMultiSession(true).setPlayClearSamplesWithoutKeys(true).setUuidAndExoMediaDrmProvider(UScheme, FrameworkMediaDrm.DEFAULT_PROVIDER).build(drmCallback);
            } else {
                MediaDrmCallback mediaDrmCallback = new HttpMediaDrmCallback(license, factory);
                manager = new DefaultDrmSessionManager.Builder().setMultiSession(true).setPlayClearSamplesWithoutKeys(true).setUuidAndExoMediaDrmProvider(UScheme, FrameworkMediaDrm.DEFAULT_PROVIDER).build(mediaDrmCallback);
            }
        }
        return manager;
    }

}
