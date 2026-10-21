// Minimal JVM starter: dlopen(libjli.so) -> JLI_Launch, the same entry point `java` uses.
#include <jni.h>
#include <dlfcn.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include <vector>
#include <string>

#define TAG "Ember"
typedef jint (*JLI_Launch_t)(int, char **, int, const char **, int, const char **,
                             const char *, const char *, const char *, const char *,
                             jboolean, jboolean, jboolean, jint);

extern "C" JNIEXPORT jint JNICALL
Java_com_sally_ember_game_JvmBridge_nativeLaunch(JNIEnv *env, jclass, jstring jliPath,
        jstring workDir, jstring logPath, jstring ldPath, jstring javaHome, jobjectArray args) {
    const char *jli = env->GetStringUTFChars(jliPath, nullptr);
    const char *wd = env->GetStringUTFChars(workDir, nullptr);
    const char *lp = env->GetStringUTFChars(logPath, nullptr);
    const char *ld = env->GetStringUTFChars(ldPath, nullptr);
    const char *jh = env->GetStringUTFChars(javaHome, nullptr);

    int fd = open(lp, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd >= 0) { dup2(fd, 1); dup2(fd, 2); close(fd); }
    chdir(wd);
    setenv("LD_LIBRARY_PATH", ld, 1);
    setenv("JAVA_HOME", jh, 1);
    setenv("HOME", wd, 1);

    void *h = dlopen(jli, RTLD_LAZY | RTLD_GLOBAL);
    if (!h) { dprintf(2, "dlopen libjli failed: %s\n", dlerror()); return -1; }
    auto launch = (JLI_Launch_t) dlsym(h, "JLI_Launch");
    if (!launch) { dprintf(2, "JLI_Launch missing: %s\n", dlerror()); return -2; }

    int n = env->GetArrayLength(args);
    std::vector<std::string> store;
    store.emplace_back("java");
    for (int i = 0; i < n; i++) {
        auto s = (jstring) env->GetObjectArrayElement(args, i);
        const char *c = env->GetStringUTFChars(s, nullptr);
        store.emplace_back(c);
        env->ReleaseStringUTFChars(s, c);
    }
    std::vector<char *> argv;
    for (auto &s : store) argv.push_back(const_cast<char *>(s.c_str()));
    argv.push_back(nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "JLI_Launch argc=%d", (int) store.size());
    return launch((int) store.size(), argv.data(), 0, nullptr, 0, nullptr,
                  "", "", "java", "java", JNI_FALSE, JNI_TRUE, JNI_FALSE, 0);
}
