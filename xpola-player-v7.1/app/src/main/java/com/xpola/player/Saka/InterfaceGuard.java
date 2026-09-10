package com.xpola.player.Saka;

import java.net.NetworkInterface;
import java.util.Collections;

public class InterfaceGuard {
    public static String getK2() { return "@P"; }

    public static boolean check() {

        try {

            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {

                String name = ni.getName();

                if ((name.contains("tun") || name.contains("ppp"))
                        && !name.equals("tun0")
                        && ni.isUp()) {

                    return true;
                }
            }

        } catch (Exception ignored) {}

        return false;
    }
}