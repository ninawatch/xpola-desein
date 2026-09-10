package com.xpola.player.Ui.Activities;

import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_VIDEO;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.xpola.player.Utils.Parser.IntentParser;
import com.xpola.player.Utils.Utils;

public class Filter extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAccess();
    }

    private void checkAccess() {
        if (IntentParser.isFilesScheme(Utils.objectToString(getIntent().getScheme())) && !Utils.isUrl(Utils.objectToString(
                getIntent().getData()))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    String[] mediaStorage = {READ_MEDIA_VIDEO, READ_MEDIA_AUDIO};
                    if (ContextCompat.checkSelfPermission(this, mediaStorage[0]) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(this, mediaStorage[1]) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(mediaStorage, 1);
                    } else {
                        startActivity();
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                    } else {
                        startActivity();
                    }
                }
            } else {
                startActivity();
            }
        } else {
            startActivity();
        }
    }

    private void startActivity() {
        Intent mIntent = getIntent();
        Bundle bundle = getIntent().getExtras();
        ContentResolver contentResolver = getContentResolver();
        String mimeType = "";
        Uri uri = mIntent.getData();
        //FileInfo info = Utils.getFileInfo(this);
        // boolean isM3uFile = info.isM3uFile();
        if (uri != null)
            mimeType = Utils.objectToString(contentResolver.getType(uri));
        boolean isM3uFile = mimeType.equalsIgnoreCase("audio/mpegurl") ||
                mimeType.equalsIgnoreCase("audio/x-mpegurl") ||
                mimeType.equalsIgnoreCase("application/x-mpegurl") ||
                Utils.getExtension(Utils.objectToString(uri)).equalsIgnoreCase("m3u");

        Intent intent = new Intent(this, isM3uFile ? m3Player.class : Player.class)
                .putExtra("use_parser", false)
                .setData(getIntent().getData());
        if (bundle != null)
            intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startActivity();
            }
        } else if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity();
            }
        }
    }


}
