#include "framegen_native_bridge.hpp"

#include "crash_reporter.hpp"

#include "android_shader_loader.hpp"
#include "lsfg_render_loop.hpp"
#include "android_vk_probe.hpp"

extern "C" int fg_extract_shaders(const char *dll_path, const char *cache_dir) {
  if (dll_path == nullptr || cache_dir == nullptr || dll_path[0] == '\0'
      || cache_dir[0] == '\0') {
    return lsfg_android::kErrDllUnreadable;
  }
  return lsfg_android::extract_dll_to_spirv(dll_path, cache_dir);
}

extern "C" int fg_probe_shaders(const char *cache_dir) {
  if (cache_dir == nullptr || cache_dir[0] == '\0') {
    return lsfg_android::kProbeMissingSpirv;
  }
  return lsfg_android::probe_shaders_on_device(cache_dir);
}

extern "C" int fg_is_framegen_fp16_supported(const char *cache_dir) {
  if (!lsfg_android::device_supports_float16()) {
    return 0;
  }
  if (cache_dir == nullptr || cache_dir[0] == '\0') {
    return 0;
  }
  return lsfg_android::fp16_shaders_available(cache_dir) ? 1 : 0;
}

extern "C" int fg_init_render_loop(const char *cache_dir, int width, int height,
                                   int multiplier, float flow_scale, int performance,
                                   int hdr, int anti_artifacts, int framegen_fp16,
                                   int target_fps_cap, float ema_alpha,
                                   float outlier_ratio, float vsync_slack_ms,
                                   int queue_depth) {
  if (cache_dir == nullptr || cache_dir[0] == '\0' || width <= 0 || height <= 0) {
    return lsfg_android::kErrDllUnreadable;
  }

  lsfg_android::RenderLoopConfig cfg{
      .width = static_cast<uint32_t>(width),
      .height = static_cast<uint32_t>(height),
      .multiplier = multiplier < 2 ? 2 : multiplier,
      .flowScale = flow_scale,
      .performance = performance != 0,
      .hdr = hdr != 0,
      .antiArtifacts = anti_artifacts != 0,
      .framegenFp16 = framegen_fp16 != 0,
      .npuPostProcessing = false,
      .npuPreset = 0,
      .npuUpscaleFactor = 1,
      .npuAmount = 0.5f,
      .npuRadius = 1.0f,
      .npuThreshold = 0.0f,
      .npuFp16 = false,
      .cpuPostProcessing = false,
      .cpuPreset = 0,
      .cpuStrength = 0.5f,
      .cpuSaturation = 0.5f,
      .cpuVibrance = 0.0f,
      .cpuVignette = 0.0f,
      .gpuPostProcessing = false,
      .gpuStage = 1,
      .gpuMethod = 0,
      .gpuUpscaleFactor = 1.0f,
      .gpuSharpness = 0.5f,
      .gpuStrength = 0.5f,
      .targetFpsCap = target_fps_cap,
      .emaAlpha = ema_alpha,
      .outlierRatio = outlier_ratio,
      .vsyncSlackMs = vsync_slack_ms,
      .queueDepth = queue_depth,
  };
  return lsfg_android::initRenderLoop(cache_dir, cfg);
}

extern "C" void fg_shutdown_render_loop(void) {
  lsfg_android::shutdownRenderLoop();
}

extern "C" void fg_set_output_surface(ANativeWindow *window, int width, int height) {
  lsfg_android::setOutputSurface(window, static_cast<uint32_t>(width),
                                 static_cast<uint32_t>(height));
}

extern "C" void fg_set_vsync_period_ns(long long period_ns) {
  lsfg_android::setVsyncPeriodNs(static_cast<int64_t>(period_ns));
}

extern "C" void fg_set_pacing_params(int target_fps_cap, float ema_alpha,
                                     float outlier_ratio, float vsync_slack_ms,
                                     int queue_depth) {
  lsfg_android::setPacingParams(target_fps_cap, ema_alpha, outlier_ratio,
                                vsync_slack_ms, queue_depth);
}

extern "C" void fg_push_frame(AHardwareBuffer *hardware_buffer, long long timestamp_ns) {
  if (hardware_buffer == nullptr) {
    return;
  }
  lsfg_android::pushFrame(hardware_buffer, static_cast<int64_t>(timestamp_ns));
}

extern "C" unsigned long long fg_get_generated_frame_count(void) {
  return static_cast<unsigned long long>(lsfg_android::getGeneratedFrameCount());
}

extern "C" unsigned long long fg_get_posted_frame_count(void) {
  return static_cast<unsigned long long>(lsfg_android::getPostedFrameCount());
}

extern "C" unsigned long long fg_get_unique_capture_count(void) {
  return static_cast<unsigned long long>(lsfg_android::getUniqueCaptureCount());
}
