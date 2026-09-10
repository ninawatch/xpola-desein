package com.xpola.player.Sec;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Base64;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Fahis {

    // التوقيع المشفر (AES-256-CBC + Base64)
    // For Play Store
// التوقيع الأصلي للتطبيق مشفر باستخدام SHA-256
    // for playStore
     private static final String ENC_SIGNATURE =  "pFky4Uui8F4J614DUgMfsEZosPJW/od0TpczxRWTm/zGjgPGLFjm61rC8VYdW9VDDmwZSu7OzfqrprPIylk92YFLsa2w6m97H028hILJW+s=";

    // For Pc / TV
    //  private static final String ENC_SIGNATURE =  "D69Hwv+hqeVkpOjBu7oG5GW7jj6oW3y/kzbcGkhed863YfjfIqBMEyxhqhM+gXOq6xuyCvdq8pxUCP+kOJOsOKcv+oNxHRSyb3sVmyb+3eA=";

    // نفس القيم المستخدمة في PHP (يفضل تشفيرها لاحقًا أو تقسيمها)
    private static final String AES_KEY =
            "0123456789abcdef0123456789abcdef"; // 32 chars

    private static final String AES_IV =
            "1234567890123456"; // 16 chars

    private Fahis() {
    }

    // ====== الدالة التي ستستخدمها في if ======
    public static boolean isValid(Context context) {
        try {
            String originalSha256 = decryptSignature();

            PackageManager pm = context.getPackageManager();
            PackageInfo info;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pm.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );

                Signature[] signatures =
                        info.signingInfo.getApkContentsSigners();

                return check(signatures, originalSha256);

            } else {
                info = pm.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNATURES
                );

                return check(info.signatures, originalSha256);
            }

        } catch (Exception e) {
            return false;
        }
    }

    // ====== مقارنة التوقيع ======
    private static boolean check(Signature[] signatures, String original) {
        if (signatures == null) return false;

        for (Signature s : signatures) {
            String sha = sha256(s.toByteArray());
            if (original.equalsIgnoreCase(sha)) {
                return true;
            }
        }
        return false;
    }

    // ====== SHA-256 ======
    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);

            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();

        } catch (Exception e) {
            return "";
        }
    }

    // ====== فك التشفير (AES-256-CBC) ======
    private static String decryptSignature() throws Exception {
        IvParameterSpec iv = new IvParameterSpec(AES_IV.getBytes());
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] decoded = Base64.decode(ENC_SIGNATURE, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(decoded);

        return new String(decrypted);
    }
}
