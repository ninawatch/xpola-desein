package com.xpola.player.Ui.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;

import com.xpola.player.R;
import com.xpola.player.Sec.NetworkOptimizer;
import com.xpola.player.Sec.Sec;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;
import android.widget.HorizontalScrollView;
import com.xpola.player.Utils.VlcEngine;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import androidx.cardview.widget.CardView;
import java.util.List;

import com.xpola.player.Utils.Parser.M3uParser;
import com.xpola.player.Utils.PlayerSaver;


import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VlcPlayer extends AppCompatActivity implements IVLCVout.Callback, MediaPlayer.EventListener {
    private static final String TAG = "VlcPlayer";

    private SurfaceView mSurfaceView;

    private String mUrl;
    private String mTitle;
    private Map<String, String> mHeaders;

    private RelativeLayout mControlsLayout;
    private ImageButton mBtnPlayPause;
    private SeekBar mSeekBar;
    private TextView mTextCurrentTime;
    private TextView mTextDuration;
    private TextView mTextTitle;
    private ProgressBar mLoading;
    private ImageButton mBtnRatio;

    private HorizontalScrollView mScrollView;
    private LinearLayout mServersParent;
    private List<Map<String, String>> mServers = new ArrayList<>();
    private int mServerSelected = 0;
    private View[] mViews = new View[0];

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsControlsVisible = true;
    private boolean mIsDragging = false;
    private final Runnable mHideControlsRunnable = this::hideControls;

    // Aspect Ratio
    private static final String[] ASPECT_RATIOS = {null, "16:9", "4:3", "1:1", "fill"}; // null is default
    private int mCurrentAspectRatioIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vlc_player);

        // Hide system UI (status bar and navigation bar)
        hideSystemUI();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        releasePlayer();
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        mUrl = NetworkOptimizer.processUrl(intent.getStringExtra("url"));
        mTitle = intent.getStringExtra("title");
        String headersJson = intent.getStringExtra("headers");
        mHeaders = new HashMap<>();
        if (headersJson != null && !headersJson.isEmpty()) {
            mHeaders = Utils.getMapString(NetworkOptimizer.getHeaders(headersJson));
        }

        String serversJson = intent.getStringExtra("servers");
        if (serversJson != null && !serversJson.isEmpty()) {
            mServers = Utils.getListString(serversJson);
            mServerSelected = intent.getIntExtra("serverSelected", 0);
        } else {
            mServers.clear();
        }

        if (!mServers.isEmpty() && mScrollView != null && mServersParent != null) {
            mScrollView.setVisibility(View.VISIBLE);
            if (mTextTitle != null) {
                mTextTitle.setVisibility(View.GONE);
            }
            CreateServers();
            selectServer(mServerSelected, true);
        } else if (mScrollView != null) {
            mScrollView.setVisibility(View.GONE);
            if (mTextTitle != null) {
                mTextTitle.setVisibility(View.VISIBLE);
            }
        }

        if (mUrl == null) {
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Sec.isProxyEnabled(this)) {
            String fallbackUrl = new Prefs(this).getString("appSnfr", "file:///android_asset/fonts/offline.mp4");
            if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                mUrl = fallbackUrl;
            }
        }

        if (mTitle != null && mTextTitle != null) {
            mTextTitle.setText(mTitle);
        }

        initVLC();
    }

    private void CreateServers() {
        if (mServersParent == null) return;
        mServersParent.removeAllViews();
        mViews = new View[mServers.size()];
        int i = 0;
        for (Map<String, String> server : mServers) {
            if (server != null) {
                @android.annotation.SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.server_item, null);
                CardView card = view.findViewById(R.id.server_parent);
                TextView text = view.findViewById(R.id.server_name);
                mViews[i] = text;

                Utils.RadiusAndStroke(card, 360, 0, Utils.getColor(getResources(), R.color.server_inactive_color), Utils.getColor(getResources(), R.color.server_inactive_color));

                text.setText(Utils.getValueString(server, M3uParser.NAME, ""));
                int finalI = i;
                card.setOnClickListener(view1 -> {
                    if (mServerSelected != finalI) {
                        selectServer(finalI, false);
                    }
                });
                mServersParent.addView(view);
                i++;
            }
        }
    }

    private void selectServer(int at, boolean fromInit) {
        for (int i = 0; i < mViews.length; i++) {
            View view = mViews[i];
            if (view != null) {
                Utils.RadiusAndStroke(view, 360, 0, Utils.getColor(getResources(), R.color.server_inactive_color), Utils.getColor(getResources(), R.color.server_inactive_color));
                if (i == at) {
                    mServerSelected = at;
                    Utils.RadiusAndStroke(view, 360, 3, Utils.getColor(getResources(), R.color.server_inactive_color), Utils.getColor(getResources(), R.color.server_active_color));

                    if (!fromInit && mServers.size() > at) {
                        Map<String, String> server = mServers.get(at);
                        Map<String, String> headers = Utils.getMapString(Utils.getValueString(server, M3uParser.HEADERS, ""));
                        String url = Utils.getValueString(server, M3uParser.URL, "file:///android_asset/fonts/offline.mp4");
                        String type = Utils.getValueString(headers, M3uParser.PLAYER_TYPE, "");

                        if (type.equalsIgnoreCase("vlcplayer") || (url != null && url.toLowerCase().startsWith("rtsp://"))) {
                            String newUrl = NetworkOptimizer.processUrl(url);
                            if (mUrl != null && mUrl.equals(newUrl) && VlcEngine.getInstance().getMediaPlayer() != null) {
                                return; // Already playing this URL in VLC.
                            }
                            mUrl = newUrl;
                            mHeaders = headers;
                            releasePlayer();
                            initVLC();
                        } else {
                            PlayerSaver.getInstance().setServers(mServers);
                            PlayerSaver.getInstance().setServerSelected(at);
                            Intent intent = new Intent(this, Player.class);
                            intent.putExtra("fromFloater", true);
                            releasePlayer(); // Release before starting new activity
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            }
        }
    }

    private void releasePlayer() {
        MediaPlayer mediaPlayer = VlcEngine.getInstance().getMediaPlayer();
        if (mediaPlayer != null) {
            IVLCVout vout = mediaPlayer.getVLCVout();
            if (vout != null) {
                vout.detachViews();
                vout.removeCallback(this);
            }
        }
        VlcEngine.getInstance().stopAndRelease();
    }

    /**
     * Hides the status bar and navigation bar for immersive fullscreen experience
     */
    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);

            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(
                        android.view.WindowInsets.Type.statusBars()
                                | android.view.WindowInsets.Type.navigationBars()
                );

                getWindow().getInsetsController().setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (VlcEngine.getInstance().getMediaPlayer() != null && !VlcEngine.getInstance().getMediaPlayer().isPlaying()) {
            VlcEngine.getInstance().getMediaPlayer().play();
        }
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.vlc_surface);
        mControlsLayout = findViewById(R.id.controls_layout);
        mBtnPlayPause = findViewById(R.id.player_play_pause);
        mSeekBar = findViewById(R.id.player_seekbar);
        mTextCurrentTime = findViewById(R.id.player_current_time);
        mTextDuration = findViewById(R.id.player_duration);
        mTextTitle = findViewById(R.id.title);
        mLoading = findViewById(R.id.loading);
        mBtnRatio = findViewById(R.id.player_ratio);
        ImageButton btnBack = findViewById(R.id.player_back);
        ImageButton btnRewind = findViewById(R.id.player_rewind);
        ImageButton btnForward = findViewById(R.id.player_forward);
        mScrollView = findViewById(R.id.servers_scroll_view);
        mServersParent = findViewById(R.id.servers_parent);

        if (mTitle != null) {
            mTextTitle.setText(mTitle);
        }

        btnBack.setOnClickListener(v -> {
            releasePlayer();
            finish();
        });

        mBtnPlayPause.setOnClickListener(v -> {
            if (VlcEngine.getInstance().getMediaPlayer() == null) return;

            // Check if media is seekable AND has a valid duration.
            // If length is <= 0, it's likely a live stream or not fully loaded, so we use stop/play behavior.
            if (VlcEngine.getInstance().getMediaPlayer().isSeekable() && VlcEngine.getInstance().getMediaPlayer().getLength() > 0) {
                if (VlcEngine.getInstance().getMediaPlayer().isPlaying()) {
                    VlcEngine.getInstance().getMediaPlayer().pause();
                } else {
                    VlcEngine.getInstance().getMediaPlayer().play();
                }
            } else {
                if (VlcEngine.getInstance().getMediaPlayer().isPlaying()) {
                    VlcEngine.getInstance().getMediaPlayer().stop();
                } else {
                    VlcEngine.getInstance().getMediaPlayer().play();
                }
            }
            showControls();
        });

        btnRewind.setOnClickListener(v -> seek(-10000));
        btnForward.setOnClickListener(v -> seek(10000));

        mBtnRatio.setOnClickListener(v -> toggleAspectRatio());

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mTextCurrentTime.setText(milliSecondsToTimer(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mHandler.removeCallbacks(mHideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsDragging = false;
                if (VlcEngine.getInstance().getMediaPlayer() != null) {
                    VlcEngine.getInstance().getMediaPlayer().setTime(seekBar.getProgress());
                }
                showControls();
            }
        });

        mSurfaceView.setOnClickListener(v -> toggleControls());
        mControlsLayout.setOnClickListener(v -> toggleControls());
    }

    private void initVLC() {
        VlcEngine vlcEngine = VlcEngine.getInstance();
        vlcEngine.init(this); // Ensure initialized

        MediaPlayer mediaPlayer = vlcEngine.getMediaPlayer();
        mediaPlayer.setEventListener(this);

        IVLCVout vout = mediaPlayer.getVLCVout();
        if (vout.areViewsAttached()) {
            vout.detachViews();
        }
        vout.setVideoView(mSurfaceView);
        vout.addCallback(this);
        vout.attachViews();

        vlcEngine.playMedia(this, mUrl, mHeaders);

        mediaPlayer.setScale(0);
        mediaPlayer.setAspectRatio(null);
        updatePlayPauseButton();

        // Hide controls initially after a delay
        mHandler.postDelayed(mHideControlsRunnable, 3000);
    }

    private void updatePlayPauseButton() {
        if (VlcEngine.getInstance().getMediaPlayer() == null) return;
        if (VlcEngine.getInstance().getMediaPlayer().isPlaying()) {
            mBtnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            mBtnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void seek(long delta) {
        if (VlcEngine.getInstance().getMediaPlayer() == null) return;
        long current = VlcEngine.getInstance().getMediaPlayer().getTime();
        long newTime = current + delta;
        if (newTime < 0) newTime = 0;
        if (newTime > VlcEngine.getInstance().getMediaPlayer().getLength()) newTime = VlcEngine.getInstance().getMediaPlayer().getLength();
        VlcEngine.getInstance().getMediaPlayer().setTime(newTime);
        showControls();
    }

    private void toggleAspectRatio() {
        if (VlcEngine.getInstance().getMediaPlayer() == null) return;
        mCurrentAspectRatioIndex++;
        if (mCurrentAspectRatioIndex >= ASPECT_RATIOS.length) {
            mCurrentAspectRatioIndex = 0;
        }
        VlcEngine.getInstance().getMediaPlayer().setAspectRatio(ASPECT_RATIOS[mCurrentAspectRatioIndex]);
        String ratio = ASPECT_RATIOS[mCurrentAspectRatioIndex];
        Toast.makeText(this, ratio == null ? "Default" : ratio, Toast.LENGTH_SHORT).show();
    }

    private void toggleControls() {
        if (mIsControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        mControlsLayout.setVisibility(View.VISIBLE);
        mIsControlsVisible = true;
        mHandler.removeCallbacks(mHideControlsRunnable);
        mHandler.postDelayed(mHideControlsRunnable, 3000);
    }

    private void hideControls() {
        mControlsLayout.setVisibility(View.GONE);
        mIsControlsVisible = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (VlcEngine.getInstance().getMediaPlayer() != null) {
            VlcEngine.getInstance().getMediaPlayer().stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (VlcEngine.getInstance().getMediaPlayer() != null) {
            VlcEngine.getInstance().getMediaPlayer().stop();
            updatePlayPauseButton();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {
        VlcEngine.getInstance().getMediaPlayer().getVLCVout().setWindowSize(
                mSurfaceView.getWidth(),
                mSurfaceView.getHeight()
        );
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {}

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Buffering:
                if (event.getBuffering() < 100.0) {
                    mLoading.setVisibility(View.VISIBLE);
                } else {
                    mLoading.setVisibility(View.GONE);
                }
                break;
            case MediaPlayer.Event.Playing:
                mLoading.setVisibility(View.GONE);
                updatePlayPauseButton();
                mHandler.post(mUpdateProgress);
                break;
            case MediaPlayer.Event.Paused:
                updatePlayPauseButton();
                break;
            case MediaPlayer.Event.Stopped:
                updatePlayPauseButton();
                break;
            case MediaPlayer.Event.EndReached:
                finish();
                break;
        }
    }

    private final Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
            if (VlcEngine.getInstance().getMediaPlayer() != null && VlcEngine.getInstance().getMediaPlayer().isPlaying()) {
                long current = VlcEngine.getInstance().getMediaPlayer().getTime();
                long duration = VlcEngine.getInstance().getMediaPlayer().getLength();

                if (!mIsDragging) {
                    mSeekBar.setMax((int) duration);
                    mSeekBar.setProgress((int) current);
                    mTextCurrentTime.setText(milliSecondsToTimer(current));
                }
                mTextDuration.setText(milliSecondsToTimer(duration));

                mHandler.postDelayed(this, 1000);
            }
        }
    };

    private String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hours > 0) {
            finalTimerString = hours + ":";
            if (minutes < 10) {
                finalTimerString += "0" + minutes + ":";
            } else {
                finalTimerString += minutes + ":";
            }
        } else {
            finalTimerString += minutes + ":";
        }

        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + secondsString;

        return finalTimerString;
    }

    public void onNewLayout(IVLCVout vout, int width, int height,
                            int visibleWidth, int visibleHeight,
                            int sarNum, int sarDen) {

        if (width == 0 || height == 0) return;

        int screenWidth = mSurfaceView.getWidth();
        int screenHeight = mSurfaceView.getHeight();

        VlcEngine.getInstance().getMediaPlayer().getVLCVout().setWindowSize(screenWidth, screenHeight);

        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = screenWidth;
        lp.height = screenHeight;
        mSurfaceView.setLayoutParams(lp);
    }
}