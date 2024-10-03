#include "org_example_demofx_ZhoWrapper.h"
#include "opencc_fmmseg_capi.h"

JNIEXPORT jlong JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1new(JNIEnv *env, jobject obj)
{
    return (jlong)opencc_new();
}

JNIEXPORT jstring JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1convert(JNIEnv *env, jobject obj, jlong instance, jstring input, jstring config, jboolean punctuation)
{
    const char *inputStr = env->GetStringUTFChars(input, NULL);
    const char *configStr = env->GetStringUTFChars(config, NULL);

    char *output = opencc_convert((void *)instance, inputStr, configStr, punctuation);

    env->ReleaseStringUTFChars(config, configStr);
    env->ReleaseStringUTFChars(input, inputStr);

    jstring result = NULL;
    if (output != NULL) {
        result = env->NewStringUTF(output);
        opencc_string_free(output);
    }
    return result;
}

JNIEXPORT void JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1free(JNIEnv *env, jobject obj, jlong instance) {
    opencc_free((void *)instance);
}

JNIEXPORT jint JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1zho_1check(JNIEnv *env, jobject obj, jlong instance, jstring input) {
    const char *inputStr = env->GetStringUTFChars(input, NULL);
    int c_code = opencc_zho_check((void *)instance, inputStr);
    env->ReleaseStringUTFChars(input, inputStr); // Release the string after using it
    return c_code;
}

JNIEXPORT jboolean JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1get_1parallel(JNIEnv *env, jobject obj, jlong instance) {
    return opencc_get_parallel((void *)instance) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1set_1parallel(JNIEnv *env, jobject obj, jlong instance, jboolean is_parallel) {
    opencc_set_parallel((void *)instance, (bool)is_parallel);
}

JNIEXPORT void JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1string_1free(JNIEnv *env, jobject obj, jstring ptr) {
    const char *str = env->GetStringUTFChars(ptr, NULL);
    env->ReleaseStringUTFChars(ptr, str); // Release the string
}

JNIEXPORT jstring JNICALL Java_org_example_demofx_ZhoWrapper_opencc_1last_1error(JNIEnv *env, jobject obj) {
    char *last_error = opencc_last_error();
    jstring result = env->NewStringUTF(last_error);
    if (last_error != NULL) {
            opencc_string_free(last_error);
    }
    return result;
}