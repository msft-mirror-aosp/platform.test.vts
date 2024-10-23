/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <dlfcn.h>
#include <jni.h>
#include <utils/Log.h>

#define ARRAY_SIZE(x) (sizeof((x)) / (sizeof(((x)[0]))))
#define TAG "VtsAngleLocationTest"

#if defined(__LP64__) || defined(_LP64)
#define SYSTEM_LIB_PATH "/system/lib64"
#else
#define SYSTEM_LIB_PATH "/system/lib"
#endif

jboolean native_testAngleLocation(JNIEnv*, jobject) {
  if (access(SYSTEM_LIB_PATH "/libEGL_angle.so", R_OK) != 0) {
    __android_log_print(ANDROID_LOG_ERROR, TAG,
                        SYSTEM_LIB_PATH "/libEGL_angle.so not found");
    return JNI_FALSE;
  }
  if (access(SYSTEM_LIB_PATH "/libGLESv1_CM_angle.so", R_OK) != 0) {
    __android_log_print(ANDROID_LOG_ERROR, TAG,
                        SYSTEM_LIB_PATH "/libGLESv1_CM_angle.so not found");
    return JNI_FALSE;
  }
  if (access(SYSTEM_LIB_PATH "/libGLESv2_angle.so", R_OK) != 0) {
    __android_log_print(ANDROID_LOG_ERROR, TAG,
                        SYSTEM_LIB_PATH "/libGLESv2_angle.so not found");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

static const JNINativeMethod gVtsAngleLocationTestMethods[] = {
    {"native_testAngleLocation", "()Z", (void*)native_testAngleLocation},
};

int register_com_google_android_vts_angle_VtsAngleLocationTest(JNIEnv* env) {
  jclass clazz =
      env->FindClass("com/google/android/vts/angle/testapp/VtsAngleTestCase");
  int ret = env->RegisterNatives(clazz, gVtsAngleLocationTestMethods,
                                 ARRAY_SIZE(gVtsAngleLocationTestMethods));
  env->DeleteLocalRef(clazz);
  return ret;
}
