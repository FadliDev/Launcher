/*
 * Jembatan JNI untuk memanggil JLI_Launch dari libjli.so milik JRE.
 * Ditulis dari nol berdasarkan signature JLI_Launch yang umum dipakai launcher Java.
 *
 * Catatan:
 *  - JLI_Launch biasanya tidak kembali; JVM memanggil exit() sendiri saat selesai.
 *    Karena itu fungsi ini dijalankan di proses :game (lihat GameService.kt).
 *  - Dependensi libjvm.so di Android 7+ dibatasi linker namespace. Tanpa hook
 *    tambahan, dlopen bisa gagal walau path benar. Ini perlu diuji di perangkat.
 */
#include <dlfcn.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>

#define TAG "FreshJVM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

typedef jint (*JLI_Launch_t)(int argc, char **argv,
                             int jargc, const char **jargv,
                             int appclassc, const char **appclassv,
                             const char *fullversion,
                             const char *dotversion,
                             const char *pname,
                             const char *lname,
                             jboolean javaargs,
                             jboolean cpwildcard,
                             jboolean javaw,
                             jint ergo);

JNIEXPORT jint JNICALL
Java_com_sally_freshlauncher_core_NativeJvm_launch(JNIEnv *env, jclass clazz,
                                                    jstring jliPath, jobjectArray args) {
    (void)clazz;
    const char *path = (*env)->GetStringUTFChars(env, jliPath, NULL);
    if (path == NULL) return -1;

    void *jli = dlopen(path, RTLD_LAZY | RTLD_GLOBAL);
    if (jli == NULL) {
        LOGE("dlopen gagal: %s", dlerror());
        (*env)->ReleaseStringUTFChars(env, jliPath, path);
        return -2;
    }

    JLI_Launch_t launch = (JLI_Launch_t)dlsym(jli, "JLI_Launch");
    if (launch == NULL) {
        LOGE("JLI_Launch tidak ditemukan: %s", dlerror());
        (*env)->ReleaseStringUTFChars(env, jliPath, path);
        return -3;
    }

    int argc = (*env)->GetArrayLength(env, args);
    char **argv = calloc((size_t)argc, sizeof(char *));
    if (argv == NULL) {
        (*env)->ReleaseStringUTFChars(env, jliPath, path);
        return -4;
    }
    for (int i = 0; i < argc; i++) {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char *c = (*env)->GetStringUTFChars(env, s, NULL);
        argv[i] = strdup(c);
        (*env)->ReleaseStringUTFChars(env, s, c);
        (*env)->DeleteLocalRef(env, s);
    }

    LOGI("Memanggil JLI_Launch dengan %d argumen", argc);
    jint rc = launch(argc, argv,
                     0, NULL,
                     0, NULL,
                     "17.0.0-internal",
                     "17",
                     argv[0],
                     argv[0],
                     JNI_FALSE,
                     JNI_TRUE,
                     JNI_FALSE,
                     0);

    for (int i = 0; i < argc; i++) free(argv[i]);
    free(argv);
    (*env)->ReleaseStringUTFChars(env, jliPath, path);
    LOGI("JLI_Launch kembali dengan kode %d", rc);
    return rc;
}
