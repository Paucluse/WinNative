#include "crash_reporter.hpp"

#include <android/log.h>
#include <cstdarg>
#include <cstdio>

namespace lsfg_android {

void ring_log(const char *tag, int level, const char *msg) {
  __android_log_print(level, tag != nullptr ? tag : "lsfg", "%s", msg != nullptr ? msg : "");
}

void ring_logf(const char *tag, int level, const char *fmt, ...) {
  char buffer[1024];
  va_list args;
  va_start(args, fmt);
  vsnprintf(buffer, sizeof(buffer), fmt, args);
  va_end(args);
  ring_log(tag, level, buffer);
}

} // namespace lsfg_android
