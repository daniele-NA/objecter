#include <malloc.h>
#include "Shared.h"


void print_java_class(JNIEnv *env, jobject obj) {
    jclass clazz = (*env).GetObjectClass( obj);
    jmethodID getClassMethod = (*env).GetMethodID(clazz, "getClass", "()Ljava/lang/Class;");
    jobject clazzObj = (*env).CallObjectMethod( obj, getClassMethod);
    jclass classClass = (*env).GetObjectClass(clazzObj);
    jmethodID getNameMethod = (*env).GetMethodID( classClass, "getName",
                                                  "()Ljava/lang/String;");
    auto jniClassName = (jstring) (*env).CallObjectMethod( clazzObj, getNameMethod);
    const char *className = (*env).GetStringUTFChars( jniClassName, JNI_FALSE);
    LOG_E("class_name => %s", className);


    (*env).ReleaseStringUTFChars( jniClassName, className);
    (*env).DeleteLocalRef(clazz);
    (*env).DeleteLocalRef(clazzObj);
    (*env).DeleteLocalRef(classClass);
    (*env).DeleteLocalRef(jniClassName);
}


float *float_array_from_jarray(JNIEnv *env, jfloatArray jarray) {
    jsize length = (*env).GetArrayLength(jarray);

    auto* array = new float[length];

    // cp jarray-values into c-array
    (*env).GetFloatArrayRegion( jarray, 0, length, array);

    return array;
}