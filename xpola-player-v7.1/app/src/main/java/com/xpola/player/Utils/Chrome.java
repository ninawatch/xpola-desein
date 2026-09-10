package com.xpola.player.Utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;


public class Chrome extends WebChromeClient {
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;

    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;
    private final Activity context;

    public Chrome(Activity context) {
        this.context = context;
    }

    public Bitmap getDefaultVideoPoster() {
        if (mCustomView == null) {
            return null;
        }
        return BitmapFactory.decodeResource(context.getResources(), 2130837573);
    }

    public void onHideCustomView() {
        ((FrameLayout) context.getWindow().getDecorView()).removeView(this.mCustomView);
        this.mCustomView = null;
        context.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
        context.setRequestedOrientation(this.mOriginalOrientation);
        this.mCustomViewCallback.onCustomViewHidden();
        this.mCustomViewCallback = null;
    }

    public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
        if (this.mCustomView != null) {
            onHideCustomView();
            return;
        }
        this.mCustomView = paramView;
        this.mOriginalSystemUiVisibility = context.getWindow().getDecorView().getSystemUiVisibility();
        this.mOriginalOrientation = context.getRequestedOrientation();
        this.mCustomViewCallback = paramCustomViewCallback;
        ((FrameLayout) context.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
        context.getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
