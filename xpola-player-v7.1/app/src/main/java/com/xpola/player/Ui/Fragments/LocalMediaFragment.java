package com.xpola.player.Ui.Fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xpola.player.R;
import com.xpola.player.Ui.Activities.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalMediaFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton addFileFab;
    private FileAdapter fileAdapter;
    private List<File> fileList = new ArrayList<>();

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                loadMediaFiles();
            } else {
                Toast.makeText(requireActivity(), "Permission denied to read external storage", Toast.LENGTH_SHORT).show();
            }
        });

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    Uri uri = data.getData();
                    copyFileToXpolaDirectory(uri);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.local_media_fragment, container, false);

        recyclerView = view.findViewById(R.id.local_media_recycler_view);
        addFileFab = view.findViewById(R.id.add_file_fab);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        fileAdapter = new FileAdapter(fileList);
        recyclerView.setAdapter(fileAdapter);

        addFileFab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        checkAndRequestPermissions();

        return view;
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadMediaFiles();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void loadMediaFiles() {
        fileList.clear();
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File xpolaDir = new File(requireActivity().getExternalFilesDir(null), "xpola");

        addFilesFromDirectory(downloadsDir);
        addFilesFromDirectory(xpolaDir);

        fileAdapter.notifyDataSetChanged();
    }

    private void addFilesFromDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> {
                String lowerCaseName = name.toLowerCase();
                return lowerCaseName.endsWith(".mp3") ||
                        lowerCaseName.endsWith(".mp4") ||
                        lowerCaseName.endsWith(".mkv") ||
                        lowerCaseName.endsWith(".m3u");
            });

            if (files != null) {
                for (File file : files) {
                    fileList.add(file);
                }
            }
        }
    }

    private void copyFileToXpolaDirectory(Uri sourceUri) {
        File xpolaDir = new File(requireActivity().getExternalFilesDir(null), "xpola");
        if (!xpolaDir.exists()) {
            xpolaDir.mkdirs();
        }

        // It's hard to get the original file name from the URI.
        // We'll use a generic name for now.
        String fileName = "imported_file_" + System.currentTimeMillis();
        File destinationFile = new File(xpolaDir, fileName);

        try (InputStream in = requireActivity().getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            Toast.makeText(requireActivity(), "File imported successfully", Toast.LENGTH_SHORT).show();
            loadMediaFiles(); // Refresh the list
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireActivity(), "Failed to import file", Toast.LENGTH_SHORT).show();
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

        private List<File> files;

        public FileAdapter(List<File> files) {
            this.files = files;
        }

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            File file = files.get(position);
            holder.textView.setText(file.getName());
            holder.itemView.setOnClickListener(v -> {
                Intent playerIntent = new Intent(requireActivity(), Player.class);
                playerIntent.setAction(Intent.ACTION_VIEW);
                playerIntent.setData(Uri.fromFile(file));
                startActivity(playerIntent);
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        class FileViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public FileViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
