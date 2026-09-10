package com.xpola.player.Saka;

import android.content.Context;

public class CoreGuard {
    public static String getK4() { return "!"; }

    public static int state(Context context) {

        boolean a = NetState.check(context);
        boolean b = InterfaceGuard.check();
        boolean c = ConnectionCheck.check(context);

        return (a || b || c) ? 1 : 0;
    }
}