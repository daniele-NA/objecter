#pragma once

#include <android/log.h>
#include <jni.h>


#define TAG "MY-LOG"

#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)


void print_java_class(JNIEnv *env, jobject obj);

float* float_array_from_jarray(JNIEnv *env,jfloatArray jarray);