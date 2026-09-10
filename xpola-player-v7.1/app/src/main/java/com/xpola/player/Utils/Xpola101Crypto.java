package com.xpola.player.Utils;

import android.content.Context;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Xpola101Crypto {

    // =========================
    // Base64Url
    // =========================
    private static byte[] b64uDec(String d) {
        d = d.replace('-', '+').replace('_', '/');
        int pad = d.length() % 4;
        if (pad != 0) {
            d += "====".substring(0, 4 - pad);
        }
        return Base64.decode(d, Base64.DEFAULT);
    }

    // HKDF implementation using HmacSHA256
    private static byte[] hkdfSha256(byte[] ikm, int length, byte[] info, byte[] salt) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        // Extract
        if (salt == null || salt.length == 0) {
            salt = new byte[32]; // HashLen for SHA-256
        }
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);

        // Expand
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] result = new byte[length];
        byte[] t = new byte[0];
        int generated = 0;
        int i = 1;

        while (generated < length) {
            mac.update(t);
            if (info != null) {
                mac.update(info);
            }
            mac.update((byte) i);
            t = mac.doFinal();

            int toCopy = Math.min(t.length, length - generated);
            System.arraycopy(t, 0, result, generated, toCopy);
            generated += toCopy;
            i++;
        }
        return result;
    }

    // =========================
    // AUTO DECRYPT (SECURE)
    // =========================
    public static String decryptAuto(Context context, String text) {
        if (text == null) return "ERROR";

        String masterKey = buildMasterKey(context);

        if (text.startsWith("XPOLA101.v4.")) return decryptV4(text, masterKey);
        if (text.startsWith("XPOLA101.v3.")) return decryptV3(text, masterKey);
        if (text.startsWith("XPOLA101.v2.")) return decryptV2(text, masterKey);
        if (text.startsWith("XPOLA101.v1.")) return decryptV1(text, masterKey);

        return "ERROR";
    }

    // =========================
    // BUILD MASTER KEY (SCATTERED)
    // =========================
    private static String buildMasterKey(Context context) {
        StringBuilder key = new StringBuilder();
        key.append(com.xpola.player.Saka.ConnectionCheck.getK1()); // "3s"
        key.append(com.xpola.player.Saka.InterfaceGuard.getK2()); // "@P"
        key.append(com.xpola.player.Saka.NetState.getK3()); // "9W"
        key.append(com.xpola.player.Saka.CoreGuard.getK4()); // "!"
        key.append(com.xpola.player.Utils.Parser.Data.getK5()); // "S2"
        key.append(com.xpola.player.Utils.Parser.M3uParser.getK6()); // "kL"
        key.append(com.xpola.player.Utils.Parser.YacineLinks.getK7()); // "Z#"
        key.append(com.xpola.player.Utils.NpvChecker.getK8()); // "mN"
        key.append(com.xpola.player.Utils.AdBlocker.getK9()); // "5v"
        key.append(context.getPackageName()); // "com.xpola.player"

        return key.toString();
    }

    // =========================
    // v4 (AES-256-GCM with HKDF)
    // =========================
    private static String decryptV4(String text, String masterKey) {
        try {
            String raw = text.substring("XPOLA101.v4.".length());
            byte[] data = b64uDec(raw);

            if (data == null || data.length < 44) return "ERROR";

            byte[] salt = new byte[16];
            System.arraycopy(data, 0, salt, 0, 8);
            System.arraycopy(data, 30, salt, 8, 8);

            byte[] iv = new byte[12];
            System.arraycopy(data, 8, iv, 0, 6);
            System.arraycopy(data, 38, iv, 6, 6);

            byte[] tag = new byte[16];
            System.arraycopy(data, 14, tag, 0, 16);

            byte[] cipherText = new byte[data.length - 44];
            System.arraycopy(data, 44, cipherText, 0, cipherText.length);

            byte[] keyBytes = hkdfSha256(masterKey.getBytes(StandardCharsets.UTF_8), 32, "XPOLA_CONTEXT".getBytes(StandardCharsets.UTF_8), salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey keySpec = new SecretKeySpec(keyBytes, "AES");
            AlgorithmParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128 bit tag length

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // In Java, the tag is expected to be appended at the end of the ciphertext for GCM decryption
            byte[] cipherTextWithTag = new byte[cipherText.length + tag.length];
            System.arraycopy(cipherText, 0, cipherTextWithTag, 0, cipherText.length);
            System.arraycopy(tag, 0, cipherTextWithTag, cipherText.length, tag.length);

            byte[] decrypted = cipher.doFinal(cipherTextWithTag);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    // =========================
    // v3 (AES-256-GCM)
    // =========================
    private static String decryptV3(String text, String masterKey) {
        try {
            String raw = text.substring("XPOLA101.v3.".length());
            byte[] data = b64uDec(raw);

            if (data == null || data.length < 28) return "ERROR";

            byte[] iv = new byte[12];
            System.arraycopy(data, 0, iv, 0, 12);

            byte[] tag = new byte[16];
            System.arraycopy(data, 12, tag, 0, 16);

            byte[] cipherText = new byte[data.length - 28];
            System.arraycopy(data, 28, cipherText, 0, cipherText.length);

            // Need a 32 byte key for AES-256
            byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey keySpec = new SecretKeySpec(keyBytes, "AES");
            AlgorithmParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherTextWithTag = new byte[cipherText.length + tag.length];
            System.arraycopy(cipherText, 0, cipherTextWithTag, 0, cipherText.length);
            System.arraycopy(tag, 0, cipherTextWithTag, cipherText.length, tag.length);

            byte[] decrypted = cipher.doFinal(cipherTextWithTag);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    // =========================
    // v2 (AES-256-CBC)
    // =========================
    private static String decryptV2(String text, String masterKey) {
        try {
            String raw = text.substring("XPOLA101.v2.".length());
            byte[] data = b64uDec(raw);

            if (data == null || data.length < 16) return "ERROR";

            byte[] iv = new byte[16];
            System.arraycopy(data, 0, iv, 0, 16);

            byte[] cipherText = new byte[data.length - 16];
            System.arraycopy(data, 16, cipherText, 0, cipherText.length);

            byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // PHP openssl defaults to PKCS7/5 padding
            SecretKey keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    // =========================
    // v1 (AES-256-CBC with Null IV)
    // =========================
    private static String decryptV1(String text, String masterKey) {
        try {
            String raw = text.substring("XPOLA101.v1.".length());
            byte[] data = b64uDec(raw);

            if (data == null) return "ERROR";

            byte[] iv = new byte[16]; // All zeros

            byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(data);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}
