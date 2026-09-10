package com.xpola.player.Utils;

import android.app.Activity;
import android.content.Context;

import com.xpola.player.R;

public class CustomIntent {
    public static final String LEFT_TO_RIGHT = "left-to-right";
    public static final String RIGHT_TO_LEFT = "right-to-left";
    public static final String FADEIN_TO_FADEOUT = "fadein-to-fadeout";
    public static void customType(Context context, String animtype) {
        Activity act = (Activity) context;
        switch (animtype) {
            case "left-to-right":
                act.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                break;
            case "right-to-left":
                act.overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                break;
            case "bottom-to-up":
                act.overridePendingTransition(R.anim.bottom_to_up, R.anim.up_to_bottom);
                break;
            case "up-to-bottom":
                act.overridePendingTransition(R.anim.up_to_bottom2, R.anim.bottom_to_up2);
                break;
            case "fadein-to-fadeout":
                act.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case "rotateout-to-rotatein":
                act.overridePendingTransition(R.anim.rotatein_out, R.anim.rotateout_in);
                break;
            default:
                break;

        }
    }
}
