package com.xpola.player.Ui.Activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.xpola.player.R;
import com.xpola.player.Utils.CustomIntent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalMediaActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MediaAdapter adapter;
    private List<MediaItem> mediaItems;
    private String currentFolder;
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_media);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mediaItems = new ArrayList<>();
        adapter = new MediaAdapter();
        recyclerView.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED))) {
                requestPermissions(new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                        android.Manifest.permission.READ_MEDIA_VIDEO
                }, STORAGE_PERMISSION_CODE);
            } else {
                loadMediaFolders();
            }
        } else {
            loadMediaFolders();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_local_media);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), Main.class));
                CustomIntent.customType(LocalMediaActivity.this, CustomIntent.RIGHT_TO_LEFT);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                startActivity(new Intent(getApplicationContext(), AddLink.class)
                        .putExtra("position", -1));
                CustomIntent.customType(LocalMediaActivity.this, CustomIntent.LEFT_TO_RIGHT);
                return true;
            } else if (itemId == R.id.nav_local_media) {
                // Reload the activity to go back to the folder list
                Intent intent = new Intent(this, LocalMediaActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMediaFolders();
        } else {
            finish();
        }
    }

    private void loadMediaFolders() {
        mediaItems.clear();
        Set<String> folders = new HashSet<>();

        // Audio files
        String[] audioProjection = {MediaStore.Audio.Media.DATA};
        Cursor audioCursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection, null, null, null);
        if (audioCursor != null) {
            while (audioCursor.moveToNext()) {
                String path = audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                File file = new File(path);
                String parent = file.getParent();
                if (parent != null) folders.add(parent);
            }
            audioCursor.close();
        }

        // Video files
        String[] videoProjection = {MediaStore.Video.Media.DATA};
        Cursor videoCursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection, null, null, null);
        if (videoCursor != null) {
            while (videoCursor.moveToNext()) {
                String path = videoCursor.getString(videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                File file = new File(path);
                String parent = file.getParent();
                if (parent != null) folders.add(parent);
            }
            videoCursor.close();
        }

        for (String folder : folders) {
            mediaItems.add(new MediaItem(folder, true));
        }
        adapter.notifyDataSetChanged();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Local Media");
        }
        currentFolder = null;
    }

    private void loadFilesInFolder(String folderPath) {
        mediaItems.clear();
        currentFolder = folderPath;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(new File(folderPath).getName());
        }

        if (!folderPath.equals("/")) {
            mediaItems.add(new MediaItem("..", true));
        }

        // 🔹 البحث عن الملفات الصوتية والفيديو عبر MediaStore
        String[] audioProjection = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME};
        String selection = MediaStore.Audio.Media.DATA + " like ?";
        String[] selectionArgs = {folderPath + "/%"};
        Cursor audioCursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection, selection, selectionArgs, null);
        if (audioCursor != null) {
            while (audioCursor.moveToNext()) {
                String path = audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                mediaItems.add(new MediaItem(path, false));
            }
            audioCursor.close();
        }

        String[] videoProjection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME};
        Cursor videoCursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection, selection, selectionArgs, null);
        if (videoCursor != null) {
            while (videoCursor.moveToNext()) {
                String path = videoCursor.getString(videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                mediaItems.add(new MediaItem(path, false));
            }
            videoCursor.close();
        }

        // 🔹 إضافة فحص يدوي لملفات m3u و xpl
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".m3u") || name.endsWith(".xpl")) {
                    mediaItems.add(new MediaItem(file.getAbsolutePath(), false));
                }
            }
        }

        adapter.notifyDataSetChanged();
    }


    private class MediaItem {
        String path;
        boolean isFolder;

        MediaItem(String path, boolean isFolder) {
            this.path = path;
            this.isFolder = isFolder;
        }

        String getDisplayName() {
            if (isFolder) return new File(path).getName();
            else return new File(path).getName();
        }
    }

    private class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ImageView icon;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.text1);
                icon = itemView.findViewById(R.id.icon);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MediaItem item = mediaItems.get(position);
            holder.textView.setText(item.getDisplayName());

            if (item.isFolder) {
                holder.icon.setImageResource(R.drawable.ic_folder);
            } else {
                String name = item.getDisplayName().toLowerCase();
                if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a")) {
                    holder.icon.setImageResource(R.drawable.ic_audio);
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")) {
                    holder.icon.setImageResource(R.drawable.ic_video);
                } else if (name.endsWith(".m3u")) {
                    holder.icon.setImageResource(R.drawable.ic_m3u);
                }
                //else if (name.endsWith(".xpl")) {
                 //   holder.icon.setImageResource(R.drawable.ic_m3u);
                //}
                else {
                    holder.icon.setImageResource(R.drawable.ic_file);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (item.isFolder) {
                    if (item.path.equals("..")) {
                        File parent = new File(currentFolder).getParentFile();
                        if (parent != null) loadFilesInFolder(parent.getAbsolutePath());
                        else loadMediaFolders();
                    } else {
                        loadFilesInFolder(item.path);
                    }
                } else {
                    playMediaFile(item.path);
                }
            });
        }

        private void playMediaFile(String filePath) {
            File file = new File(filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // For Android 10+ we need to use MediaStore to get the content URI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore to get the content URI for the file
                Uri contentUri = getContentUriForFile(file);
                if (contentUri != null) {
                    intent.setDataAndType(contentUri, getMimeType(filePath));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    // Fallback to FileProvider if MediaStore doesn't work
                    contentUri = FileProvider.getUriForFile(
                            LocalMediaActivity.this,
                            getApplicationContext().getPackageName() + ".provider",
                            file);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } else {
                // For older versions, use FileProvider
                Uri fileUri = FileProvider.getUriForFile(
                        LocalMediaActivity.this,
                        getApplicationContext().getPackageName() + ".provider",
                        file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(fileUri, getMimeType(filePath));
            }

            intent.setPackage("com.xpola.player");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                // Handle exception (e.g., show toast message)
            }
        }

        private Uri getContentUriForFile(File file) {
            // Try to find the file in MediaStore
            String[] projection = {MediaStore.MediaColumns._ID};
            String selection = MediaStore.MediaColumns.DATA + " = ?";
            String[] selectionArgs = new String[]{file.getAbsolutePath()};

            Uri contentUri = null;
            Cursor cursor = null;

            try {
                // Check both audio and video MediaStore
                cursor = getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection, selection, selectionArgs, null);

                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                    contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                } else {
                    cursor = getContentResolver().query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            projection, selection, selectionArgs, null);

                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                        contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return contentUri;
        }

        private String getMimeType(String path) {
            String type = null;
            String extension = getFileExtension(path);

            if (extension != null) {
                switch (extension.toLowerCase()) {
                    case "mp3":
                    case "wav":
                    case "m4a":
                        type = "audio/*";
                        break;
                    case "mp4":
                        type = "video/mp4";
                        break;
                    case "mkv":
                        type = "video/x-matroska";
                        break;
                    case "avi":
                        type = "video/x-msvideo";
                        break;
                    case "m3u":
                    case "xpl":
                        type = "audio/x-mpegurl";
                        break;
                    default:
                        type = "*/*";
                }
            }
            return type;
        }

        private String getFileExtension(String path) {
            if (path == null || path.lastIndexOf(".") == -1) {
                return null;
            }
            return path.substring(path.lastIndexOf(".") + 1);
        }


        @Override
        public int getItemCount() {
            return mediaItems.size();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFolder != null) {
            // If we are in any subfolder, go back to the main folder list
            loadMediaFolders();
        } else {
            // If we are at the main folder list, perform the default back action
            super.onBackPressed();
        }
    }
}
