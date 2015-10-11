#include "jni.h"
#include "opencv2/contrib/contrib.hpp"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_net_digitalbebop_http_handlers_indexModules_FisherFaceRecognizer_createFisherFaceRecognizer_10(JNIEnv* env, jclass);
JNIEXPORT jlong JNICALL Java_net_digitalbebop_http_handlers_indexModules_FisherFaceRecognizer_createFisherFaceRecognizer_10(JNIEnv* env, jclass) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createFisherFaceRecognizer();
        pfr.addref(); // this is for the 2.4 branch, 3.0 would need a different treatment here
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_net_digitalbebop_http_handlers_indexModules_FisherFaceRecognizer_createFisherFaceRecognizer_11(JNIEnv* env, jclass, jint num_components);
JNIEXPORT jlong JNICALL Java_net_digitalbebop_http_handlers_indexModules_FisherFaceRecognizer_createFisherFaceRecognizer_11(JNIEnv* env, jclass, jint num_components) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createFisherFaceRecognizer(num_components);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_net_digitalbebop_http_handlers_indexModules_FisherFaceRecognizer_createFisherFaceRecognizer_12(JNIEnv* env, jclass, jint num_components, jdouble threshold);
JNIEXPORT jlong JNICALL Java_net_digitalbebop_http_handlers_indexModules_FisherFaceRecognizer_createFisherFaceRecognizer_12(JNIEnv* env, jclass, jint num_components, jdouble threshold) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createFisherFaceRecognizer(num_components,threshold);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

#ifdef __cplusplus
}
#endif
