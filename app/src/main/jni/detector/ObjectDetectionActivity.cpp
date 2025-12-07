
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <cmath>
#include "tensorflow/lite/c/common.h"
#include "tensorflow/lite/c/c_api.h"
#include "tensorflow/lite/delegates/nnapi/nnapi_delegate_c_api.h"
#include "ObjectDetector.h"

using namespace cv;

void rotateMat(Mat &matImage, int rotation) {
    if (rotation == 90) {
        transpose(matImage, matImage);
        flip(matImage, matImage, 1);
    } else if (rotation == 270) {
        transpose(matImage, matImage);
        flip(matImage, matImage, 0);
    } else if (rotation == 180) {
        flip(matImage, matImage, -1);
    }
}

static AAssetManager *assets_manager;
static ObjectDetector * object_detector_pt;

extern "C"
{
JNIEXPORT void JNICALL
Java_com_crescenzi_objecter_ObjectDetectionActivity_initDetector(JNIEnv *env, jobject thiz,
                                                                 jobject java_assets) {

    char *model_buffer=nullptr;
    long model_size=0;

    assets_manager = AAssetManager_fromJava(env, java_assets);
    AAsset *model = AAssetManager_open(assets_manager, "object.tflite", AASSET_MODE_BUFFER);

    model_size= AAsset_getLength(model);
    model_buffer=(char*) malloc(sizeof (char)* model_size);

    // Fill the Buffer
    AAsset_read(model,model_buffer,model_size);

    // Close the asset
    AAsset_close(model);

    // DO NOT use NNAPI (Deprecated Since Android 15)
    object_detector_pt = new ObjectDetector(model_buffer,model_size, true,false);
    free(model_buffer);
}

// If the pointer exists, we cast it to ObjectDetector* and call the destructor,
// properly freeing the memory associated with the object.
JNIEXPORT void JNICALL
Java_com_crescenzi_objecter_ObjectDetectionActivity_destroyDetector(JNIEnv *env, jobject thiz) {
    if(object_detector_pt) {
        delete (ObjectDetector *) object_detector_pt;
        object_detector_pt = nullptr;
    }
}


JNIEXPORT jfloatArray JNICALL
Java_com_crescenzi_objecter_ObjectDetectionActivity_detect(JNIEnv *env, jobject thiz,
                                                           jbyteArray bytes, jint width,
                                                           jint height) {

    jbyte * _rgba= env->GetByteArrayElements(bytes, JNI_FALSE);

    Mat frame(height, width, CV_8UC4, _rgba);
    cvtColor(frame, frame, COLOR_RGBA2BGRA); // (R, G, B, A)  →  (B, G, R, A)

    env->ReleaseByteArrayElements(bytes, _rgba, 0);


    auto *detector = (ObjectDetector *) object_detector_pt;
    DetectResult *res = detector->detect(frame);

    // +1 because the first element will contain the length
    int arr_len = 6 * detector->DETECT_NUM + 1;
    auto* j_res = new jfloat[arr_len];
    j_res[0] = detector->DETECT_NUM;  // set length


    // the order is important,we'll unpack this array in Kotlin-Side
    for (int i = 0; i < detector->DETECT_NUM; ++i) {
        int pos = i * 6 + 1;
        j_res[pos + 0] = res[i].score;
        j_res[pos + 1] = res[i].label;
        j_res[pos + 2] = res[i].xmin;
        j_res[pos + 3] = res[i].ymin;
        j_res[pos + 4] = res[i].xmax;
        j_res[pos + 5] = res[i].ymax;
    }

    // handled by JVM , do not call free functions
    jfloatArray output = env->NewFloatArray(arr_len);
    env->SetFloatArrayRegion(output, 0, arr_len, j_res);

    return output;
}

}
