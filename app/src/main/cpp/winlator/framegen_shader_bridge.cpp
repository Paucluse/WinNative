#include <jni.h>
#include <android/hardware_buffer.h>
#include <android/native_window_jni.h>

#include "framegen_native_bridge.hpp"

extern "C" JNIEXPORT jint JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeExtractLosslessShaders(
    JNIEnv *env, jclass clazz, jstring dll_path, jstring cache_dir) {
  (void)clazz;
  if (dll_path == nullptr || cache_dir == nullptr) {
    return -1;
  }

  const char *utf_dll_path = env->GetStringUTFChars(dll_path, nullptr);
  const char *utf_cache_dir = env->GetStringUTFChars(cache_dir, nullptr);
  if (utf_dll_path == nullptr || utf_cache_dir == nullptr) {
    if (utf_dll_path != nullptr) {
      env->ReleaseStringUTFChars(dll_path, utf_dll_path);
    }
    if (utf_cache_dir != nullptr) {
      env->ReleaseStringUTFChars(cache_dir, utf_cache_dir);
    }
    return -1;
  }

  const int result = fg_extract_shaders(utf_dll_path, utf_cache_dir);
  env->ReleaseStringUTFChars(dll_path, utf_dll_path);
  env->ReleaseStringUTFChars(cache_dir, utf_cache_dir);
  return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeProbeLosslessShaders(
    JNIEnv *env, jclass clazz, jstring cache_dir) {
  (void)clazz;
  if (cache_dir == nullptr) {
    return -11;
  }

  const char *utf_cache_dir = env->GetStringUTFChars(cache_dir, nullptr);
  if (utf_cache_dir == nullptr) {
    return -11;
  }

  const int result = fg_probe_shaders(utf_cache_dir);
  env->ReleaseStringUTFChars(cache_dir, utf_cache_dir);
  return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeIsFramegenFp16Supported(
    JNIEnv *env, jclass clazz, jstring cache_dir) {
  (void)clazz;
  if (cache_dir == nullptr) {
    return JNI_FALSE;
  }

  const char *utf_cache_dir = env->GetStringUTFChars(cache_dir, nullptr);
  if (utf_cache_dir == nullptr) {
    return JNI_FALSE;
  }

  const int result = fg_is_framegen_fp16_supported(utf_cache_dir);
  env->ReleaseStringUTFChars(cache_dir, utf_cache_dir);
  return result != 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeInitLosslessRenderLoop(
    JNIEnv *env, jclass clazz, jstring cache_dir, jint width, jint height,
    jint multiplier, jfloat flow_scale, jboolean performance, jboolean hdr,
    jboolean anti_artifacts, jboolean framegen_fp16, jint target_fps_cap,
    jfloat ema_alpha, jfloat outlier_ratio, jfloat vsync_slack_ms, jint queue_depth) {
  (void)clazz;
  if (cache_dir == nullptr) {
    return -1;
  }

  const char *utf_cache_dir = env->GetStringUTFChars(cache_dir, nullptr);
  if (utf_cache_dir == nullptr) {
    return -1;
  }

  const int result = fg_init_render_loop(
      utf_cache_dir, width, height, multiplier, static_cast<float>(flow_scale),
      performance == JNI_TRUE, hdr == JNI_TRUE, anti_artifacts == JNI_TRUE,
      framegen_fp16 == JNI_TRUE, target_fps_cap, static_cast<float>(ema_alpha),
      static_cast<float>(outlier_ratio), static_cast<float>(vsync_slack_ms), queue_depth);
  env->ReleaseStringUTFChars(cache_dir, utf_cache_dir);
  return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeShutdownLosslessRenderLoop(
    JNIEnv *env, jclass clazz) {
  (void)env;
  (void)clazz;
  fg_shutdown_render_loop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeSetLosslessOutputSurface(
    JNIEnv *env, jclass clazz, jobject surface, jint width, jint height) {
  (void)clazz;
  ANativeWindow *window =
      surface != nullptr ? ANativeWindow_fromSurface(env, surface) : nullptr;
  fg_set_output_surface(window, width, height);
  if (window != nullptr) {
    ANativeWindow_release(window);
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeSetLosslessVsyncPeriodNs(
    JNIEnv *env, jclass clazz, jlong period_ns) {
  (void)env;
  (void)clazz;
  fg_set_vsync_period_ns(static_cast<long long>(period_ns));
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeSetLosslessPacingParams(
    JNIEnv *env, jclass clazz, jint target_fps_cap, jfloat ema_alpha,
    jfloat outlier_ratio, jfloat vsync_slack_ms, jint queue_depth) {
  (void)env;
  (void)clazz;
  fg_set_pacing_params(
      target_fps_cap, static_cast<float>(ema_alpha),
      static_cast<float>(outlier_ratio), static_cast<float>(vsync_slack_ms),
      queue_depth);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativePushLosslessFrame(
    JNIEnv *env, jclass clazz, jlong hardware_buffer_ptr, jlong timestamp_ns) {
  (void)env;
  (void)clazz;
  fg_push_frame(reinterpret_cast<AHardwareBuffer *>(hardware_buffer_ptr),
                static_cast<long long>(timestamp_ns));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeGetLosslessGeneratedFrameCount(
    JNIEnv *env, jclass clazz) {
  (void)env;
  (void)clazz;
  return static_cast<jlong>(fg_get_generated_frame_count());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeGetLosslessPostedFrameCount(
    JNIEnv *env, jclass clazz) {
  (void)env;
  (void)clazz;
  return static_cast<jlong>(fg_get_posted_frame_count());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_runtime_display_framegen_FrameGenerationBridge_nativeGetLosslessUniqueCaptureCount(
    JNIEnv *env, jclass clazz) {
  (void)env;
  (void)clazz;
  return static_cast<jlong>(fg_get_unique_capture_count());
}
