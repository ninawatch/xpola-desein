package com.xpola.player.Sec;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Pack: فك تشفير اسم الحزمة المخزن مشفّراً ومقارنته باسم الحزمة الحالية.
 * إذا اختلف الاسم أو فشل فك التشفير => إغلاق التطبيق صامتاً.
 *
 * ملاحظة: يتطلّب API >= 21 لاستخدام AES/GCM بشكل موثوق.
 */
public class Pack {

    // السلسلة المشفّرة الناتجة من كود PHP (مثال)
    private static final String ENCRYPTED_PKG_BASE64 = "4NXVhwa4+q6YtdBeFhEYc3YuFzFa82kPMVtoXEV28VPKhUgATvz0BCCU9zQ=";

    // نفس المفتاح المستخدم في PHP (Base64، يجب أن يكون 32 بايت بعد فك الـ base64)
    private static final String KEY_BASE64 = "h3rKX7EIf5T0C4b9M7k+0R91QEqEvzQxRt7Q+w8nFv0=";

    // طول IV و TAG كما في PHP (12 و 16 بايت)
    private static final int IV_LEN = 12;
    private static final int TAG_LEN = 16; // بالبايت

    // DECRYPT: يفك التشفير ويعيد سلسلة نصية
    private static String decryptPackageString(String payloadBase64, String keyBase64) throws GeneralSecurityException {
        try {
            byte[] raw = Base64.decode(payloadBase64, Base64.DEFAULT);
            byte[] key = Base64.decode(keyBase64, Base64.DEFAULT);

            if (key == null || key.length != 32) {
                throw new InvalidKeyException("Invalid AES-256 key length");
            }
            if (raw == null || raw.length < IV_LEN + TAG_LEN + 1) {
                throw new GeneralSecurityException("Invalid ciphertext length");
            }

            // بناء الأجزاء طبق ترتيب PHP: raw = IV (12) || TAG (16) || CIPHERTEXT
            byte[] iv = new byte[IV_LEN];
            byte[] tag = new byte[TAG_LEN];
            int ciphertextLen = raw.length - IV_LEN - TAG_LEN;
            byte[] ciphertext = new byte[ciphertextLen];

            System.arraycopy(raw, 0, iv, 0, IV_LEN);
            System.arraycopy(raw, IV_LEN, tag, 0, TAG_LEN);
            System.arraycopy(raw, IV_LEN + TAG_LEN, ciphertext, 0, ciphertextLen);

            // Java يتوقع ciphertext||tag لذلك نعيد تجميعهما بهذا الترتيب
            byte[] cipherPlusTag = new byte[ciphertextLen + TAG_LEN];
            System.arraycopy(ciphertext, 0, cipherPlusTag, 0, ciphertextLen);
            System.arraycopy(tag, 0, cipherPlusTag, ciphertextLen, TAG_LEN);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            // AES/GCM/NoPadding
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            // Spec expects tag length in bits
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LEN * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plain = cipher.doFinal(cipherPlusTag);
            return new String(plain, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException gse) {
            throw gse;
        } catch (Exception e) {
            // أي استثناء آخر اعتبره فشل أمني
            throw new GeneralSecurityException("Decryption error", e);
        }
    }

    // فحص اسم الحزمة بمقارنة النتيجة المفكّكة مع اسم الحزمة الحالي
    private static boolean isPackageNameValid(Context context) {
        try {
            String decrypted = decryptPackageString(ENCRYPTED_PKG_BASE64, KEY_BASE64);
            String current = context.getPackageName();
            // المقارنة البسيطة
            return decrypted != null && decrypted.equals(current);
        } catch (Exception e) {
            // أي خطأ => نعتبر التطبيق معدل/غير صالح
            return false;
        }
    }

    // فحص وإغلاق التطبيق مباشرة بدون أي إخطار (لا تعرض رسائل)
    public static void checkAndCloseIfTampered(Activity activity) {
        if (activity == null) return;
        if (!isPackageNameValid(activity)) {
            try {
                activity.finishAffinity();
            } catch (Exception ignored) {}
            try {
                System.exit(0);
            } catch (Exception ignored) {}
        }
    }

    // نسخة للعمل في أي Context (لـ Service أو استقبال)
    public static void killIfTampered(Context context) {
        if (!isPackageNameValid(context)) {
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception ignored) {}
            try {
                System.exit(0);
            } catch (Exception ignored) {}
        }
    }
}
