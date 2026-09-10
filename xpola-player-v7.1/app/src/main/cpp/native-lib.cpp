#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include <vector>

#define LOG_TAG "NativeSec"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// VALID SIGNATURES (SHA-256)
const std::vector<std::string> EXPECTED_SIGNATURE_HASHES = {
    "E546216FE6E4CA5C18FDD95D5E37B2273D38BB865C1E8682EEC7E6C64D47B618", // Play Store
    "0AEE6366E3E415F9A48299ADECE8D0D210111D2961A7188EE8B9846D27CEF386"  // Direct APK
};

const char* EXPECTED_PACKAGE = "com.xpola.player";

bool is_compromised = false;

// Helper to calculate SHA-256 of a byte array using Java's MessageDigest
std::string calculateSHA256(JNIEnv *env, jbyteArray byteArray) {
    jclass digestClass = env->FindClass("java/security/MessageDigest");
    if (!digestClass) return "";

    jmethodID getInstance = env->GetStaticMethodID(digestClass, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jmethodID update = env->GetMethodID(digestClass, "update", "([B)V");
    jmethodID digest = env->GetMethodID(digestClass, "digest", "()[B");

    jstring sha256 = env->NewStringUTF("SHA-256");
    jobject md = env->CallStaticObjectMethod(digestClass, getInstance, sha256);
    env->DeleteLocalRef(sha256);

    env->CallVoidMethod(md, update, byteArray);
    jbyteArray hash = (jbyteArray) env->CallObjectMethod(md, digest);

    jbyte* hashBytes = env->GetByteArrayElements(hash, NULL);
    jsize len = env->GetArrayLength(hash);

    char hexChars[] = "0123456789ABCDEF";
    std::string hexString;
    for (int i = 0; i < len; ++i) {
        hexString += hexChars[(hashBytes[i] >> 4) & 0xF];
        hexString += hexChars[hashBytes[i] & 0xF];
    }

    env->ReleaseByteArrayElements(hash, hashBytes, JNI_ABORT);
    return hexString;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_xpola_player_Sec_ConfigLoader_verifyIntegrity(JNIEnv *env, jobject thiz, jobject context) {

    // 1. Check Package Name
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPackageNameMethod = env->GetMethodID(contextClass, "getPackageName", "()Ljava/lang/String;");
    jstring packageName = (jstring) env->CallObjectMethod(context, getPackageNameMethod);
    const char *pkgNameChars = env->GetStringUTFChars(packageName, 0);

    if (strcmp(pkgNameChars, EXPECTED_PACKAGE) != 0) {
        is_compromised = true;
    }
    env->ReleaseStringUTFChars(packageName, pkgNameChars);

    if (is_compromised) return JNI_FALSE;

    // 2. Check Signature
    jclass pmClass = env->FindClass("android/content/pm/PackageManager");
    jmethodID getPmMethod = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject pm = env->CallObjectMethod(context, getPmMethod);

    jmethodID getPackageInfoMethod = env->GetMethodID(pmClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");

    // PackageManager.GET_SIGNATURES = 64
    jobject packageInfo = env->CallObjectMethod(pm, getPackageInfoMethod, packageName, 64);

    jclass packageInfoClass = env->GetObjectClass(packageInfo);
    jfieldID signaturesField = env->GetFieldID(packageInfoClass, "signatures", "[Landroid/content/pm/Signature;");
    jobjectArray signatures = (jobjectArray) env->GetObjectField(packageInfo, signaturesField);

    jobject signature = env->GetObjectArrayElement(signatures, 0);
    jclass signatureClass = env->GetObjectClass(signature);
    jmethodID toByteArrayMethod = env->GetMethodID(signatureClass, "toByteArray", "()[B");
    jbyteArray signatureBytes = (jbyteArray) env->CallObjectMethod(signature, toByteArrayMethod);

    std::string currentHash = calculateSHA256(env, signatureBytes);

    bool signatureMatch = false;
    for (const auto& expected : EXPECTED_SIGNATURE_HASHES) {
        if (currentHash == expected) {
            signatureMatch = true;
            break;
        }
    }

    if (!signatureMatch) {
        // Log mismatch for debugging if needed, but in prod we just set flag
        // LOGI("Signature Mismatch! Current: %s", currentHash.c_str());
        is_compromised = true;
    }

    // 3. Simple Anti-Debug (PTRACE)
    // Disabled by default to prevent issues on some devices, but good to have ready.
    /*
    if (ptrace(PTRACE_TRACEME, 0, 1, 0) < 0) {
        is_compromised = true;
    }
    */

    return is_compromised ? JNI_FALSE : JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_xpola_player_Sec_ConfigLoader_getSecureString(JNIEnv *env, jobject thiz, jint id) {
    if (is_compromised) {
        // Return corrupted data
        if (id == 1) return env->NewStringUTF("http://broken-url.com");
        if (id == 2) return env->NewStringUTF("Bad-Agent/1.0");
    } else {
        // Return real data (examples)
        if (id == 1) return env->NewStringUTF("https://real-api-endpoint.com");
        if (id == 2) return env->NewStringUTF("Mozilla/5.0 (Linux; Android 10; Mobile)");
    }
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_xpola_player_Sec_ConfigLoader_shouldTriggerChaos(JNIEnv *env, jobject thiz) {
    if (!is_compromised) return JNI_FALSE;

    // Return true randomly
    int random = rand() % 100;
    return (random > 50) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_xpola_player_Sec_ConfigLoader_setCompromised(JNIEnv *env, jobject thiz, jboolean state) {
    // Allow Java to signal compromise too
    if (state) is_compromised = true;
}
