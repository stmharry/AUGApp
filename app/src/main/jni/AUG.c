#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_example_harry_aug_AccelerometerFragment_getStringNative(JNIEnv *env, jobject instance) {
    return (*env)->NewStringUTF(env, "Hello world!");
}