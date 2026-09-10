package com.xpola.player.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.xpola.player.Ui.Activities.Player;
import com.xpola.player.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;


public class Floater {
    private final AppCompatActivity act;
    private View floatView;
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private WindowManager windowManager;
    private ImageView maximize, close, playPause;
    private RelativeLayout parent;
    private PlayerView pv;
    private ExoPlayer player;
    private boolean isShow = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable = null;
    private SharedPreferences sp;

    public Floater(AppCompatActivity act) {
        this.act = act;
    }


    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    public void show() {
        try {
            DisplayMetrics metrics = act.getApplicationContext().getResources().getDisplayMetrics();
            int heights = metrics.heightPixels;
            int widths = metrics.widthPixels;
            if (widths < heights) {
                heights = metrics.widthPixels;
                widths = metrics.heightPixels;
            }
            windowManager = (WindowManager) act.getSystemService(Context.WINDOW_SERVICE);
            LayoutInflater inflater = (LayoutInflater) act.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            floatView = inflater.inflate(R.layout.mini_player, null);
            initialize();
            sp = act.getSharedPreferences("windowParams", AppCompatActivity.MODE_PRIVATE);
            int LAYOUT_TYPE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
            }
            floatWindowLayoutParam = new WindowManager.LayoutParams(
                    (int) (widths * (0.40f)),
                    (int) (heights * (0.40f)),
                    LAYOUT_TYPE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            PlayerSaver.getInstance().setWindow(windowManager);
            PlayerSaver.getInstance().setView(floatView);

            floatWindowLayoutParam.gravity = Gravity.CENTER;
            floatWindowLayoutParam.x = ParamX();
            floatWindowLayoutParam.y = ParamY();

            windowManager.addView(floatView, floatWindowLayoutParam);

            maximize.setOnClickListener(new onClick());
            playPause.setOnClickListener(new onClick());
            close.setOnClickListener(new onClick());
            floatView.setOnTouchListener(new onTouchListener());
            parent.setOnTouchListener(new onTouchListener());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        try {
            parent = floatView.findViewById(R.id.mini_payer_parent);
            pv = floatView.findViewById(R.id.mini_player_view);
            maximize = floatView.findViewById(R.id.exite_fullscreen);
            close = floatView.findViewById(R.id.close_mini_player);
            playPause = floatView.findViewById(R.id.mini_play_pause);
            player = PlayerSaver.getInstance().Player();
            pv.setKeepScreenOn(true);
            pv.setPlayer(player);
            if (player.getPlayWhenReady()) playPause.setImageResource(R.drawable.ic_pause);
            if (runnable == null) {
                runnable = () -> {
                    upDateVisibility(false);
                    isShow = false;
                };
            }
            try {
                handler.removeCallbacks(runnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
            handler.postDelayed(runnable, 2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private int ParamX() {
        try {
            if (!sp.getString("x", "").isEmpty()) {
                return Integer.parseInt(sp.getString("x", ""));
            }
        } catch (Exception ignored) {

        }
        return 0;
    }

    private int ParamY() {
        try {
            if (!sp.getString("y", "").isEmpty()) {
                return Integer.parseInt(sp.getString("y", ""));
            }
        } catch (Exception ignored) {

        }
        return 0;
    }

    private void upDateVisibility(boolean show) {
        playPause.setVisibility(!show ? View.GONE : View.VISIBLE);
        maximize.setVisibility(!show ? View.GONE : View.VISIBLE);
        close.setVisibility(!show ? View.GONE : View.VISIBLE);
    }

    private class onTouchListener implements View.OnTouchListener {
        final WindowManager.LayoutParams floatWindowLayoutUpdateParam = floatWindowLayoutParam;
        double x;
        double y;
        double px;
        double py;

        @Override
        public boolean onTouch(View v, @NonNull MotionEvent event) {
            pv.hideController();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = floatWindowLayoutUpdateParam.x;
                    y = floatWindowLayoutUpdateParam.y;
                    px = event.getRawX();
                    py = event.getRawY();
                    if (isShow) {
                        upDateVisibility(false);
                        isShow = false;
                    } else {
                        upDateVisibility(true);
                        isShow = true;
                        try {
                            if (runnable != null)
                                handler.removeCallbacks(runnable);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (runnable != null) handler.postDelayed(runnable, 2000);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    floatWindowLayoutUpdateParam.x = (int) ((x + event.getRawX()) - px);
                    floatWindowLayoutUpdateParam.y = (int) ((y + event.getRawY()) - py);
                    sp.edit().putString("x", "" + floatWindowLayoutUpdateParam.x).apply();
                    sp.edit().putString("y", "" + floatWindowLayoutUpdateParam.y).apply();
                    windowManager.updateViewLayout(floatView, floatWindowLayoutUpdateParam);
                    break;
            }
            return false;
        }
    }

    private class onClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view == maximize) {
                try {
                    windowManager.removeView(floatView);
                    PlayerSaver.getInstance().setPlayer(player);
                    Intent backToHome = new Intent(act, Player.class);
                    backToHome.setData(PlayerSaver.getInstance().getUri());
                    backToHome.putExtra("fromFloater", true);
                    act.startActivity(backToHome);
                    if (PlayerSaver.getInstance().Window() != null && PlayerSaver.getInstance().View() != null) {
                        PlayerSaver.getInstance().Window().removeView(PlayerSaver.getInstance().View());
                        PlayerSaver.getInstance().setWindow(null);
                        PlayerSaver.getInstance().setView(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view == close) {
                try {
                    releasePlayer();
                    PlayerSaver.getInstance().setPlayer(player);
                    windowManager.removeView(floatView);
                    if (PlayerSaver.getInstance().Window() != null && PlayerSaver.getInstance().View() != null) {
                        PlayerSaver.getInstance().Window().removeView(PlayerSaver.getInstance().View());
                        PlayerSaver.getInstance().setWindow(null);
                        PlayerSaver.getInstance().setView(null);
                    }
                    PlayerSaver.getInstance().clear();
                } catch (Exception ignored) {
                }
            } else if (view == playPause) {
                if (player != null) {
                    if (player.getPlayWhenReady()) {
                        player.setPlayWhenReady(false);
                        player.getPlayWhenReady();
                        playPause.setImageResource(R.drawable.ic_play);
                    } else {
                        player.setPlayWhenReady(true);
                        player.getPlayWhenReady();
                        playPause.setImageResource(R.drawable.ic_pause);
                    }
                }
            }
        }
    }
}