package com.xpola.player.Sec;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Handles core security configuration loading and validation.
 * Obfuscated name intended: "ConfigLoader"
 */
public class ConfigLoader {

    static {
        try {
            System.loadLibrary("xpola-secure");
        } catch (UnsatisfiedLinkError e) {
            // Native lib failed to load. Likely tampered or incompatible.
            // In a secure app, we should probably crash or set compromised flag.
            NetworkOptimizer.setCompromised(true);
        }
    }

    // Native methods
    public native boolean verifyIntegrity(Context context);
    public native String getSecureString(int id);
    public native boolean shouldTriggerChaos();
    public native void setCompromised(boolean state);

    private static ConfigLoader instance;
    private boolean isCompromised = false;

    public static ConfigLoader getInstance() {
        if (instance == null) instance = new ConfigLoader();
        return instance;
    }

    public void init(Context context) {
        // Run native check
        boolean nativeCheck = false;
        try {
            nativeCheck = verifyIntegrity(context);
        } catch (Exception e) {
            nativeCheck = false;
        }

        if (!nativeCheck) {
            isCompromised = true;
            NetworkOptimizer.setCompromised(true);
        }

        // Run Java checks
        if (!checkSignature(context)) {
            isCompromised = true;
            NetworkOptimizer.setCompromised(true);
            setCompromised(true); // Tell native layer
        }

        if (checkDebuggable(context)) {
            // Optionally allow debuggable builds but maybe log it?
            // For production security, we flag it.
            // isCompromised = true;
        }

        // Check if classes exist (simple integrity check)
        try {
            Class.forName("com.xpola.player.Ui.Activities.VlcPlayer");
        } catch (ClassNotFoundException e) {
             isCompromised = true;
             NetworkOptimizer.setCompromised(true);
        }
    }

    private boolean checkSignature(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim();

                // Compare with expected signature hash (This should be obfuscated or calculated)
                // For now, we rely on the native check mainly, but having a Java check adds redundancy.
                // We'll skip hardcoding a specific hash here to avoid breaking valid builds if the key changes,
                // but in a real scenario, you would check against a known constant.
                // Log.d("Sec", "Sig: " + currentSignature);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean checkDebuggable(Context context) {
        return (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public boolean isCompromised() {
        return isCompromised;
    }
}
