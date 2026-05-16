#ifndef WINLATOR_FRAMEGEN_NATIVE_BRIDGE_HPP
#define WINLATOR_FRAMEGEN_NATIVE_BRIDGE_HPP

#include <android/hardware_buffer.h>

struct ANativeWindow;

#ifdef __cplusplus
extern "C" {
#endif

int fg_extract_shaders(const char *dll_path, const char *cache_dir);
int fg_probe_shaders(const char *cache_dir);
int fg_is_framegen_fp16_supported(const char *cache_dir);
int fg_init_render_loop(const char *cache_dir, int width, int height, int multiplier,
                        float flow_scale, int performance, int hdr, int anti_artifacts,
                        int framegen_fp16, int target_fps_cap, float ema_alpha,
                        float outlier_ratio, float vsync_slack_ms, int queue_depth);
void fg_shutdown_render_loop(void);
void fg_set_output_surface(ANativeWindow *window, int width, int height);
void fg_set_vsync_period_ns(long long period_ns);
void fg_set_pacing_params(int target_fps_cap, float ema_alpha, float outlier_ratio,
                          float vsync_slack_ms, int queue_depth);
void fg_push_frame(AHardwareBuffer *hardware_buffer, long long timestamp_ns);
unsigned long long fg_get_generated_frame_count(void);
unsigned long long fg_get_posted_frame_count(void);
unsigned long long fg_get_unique_capture_count(void);

#ifdef __cplusplus
}
#endif

#endif
