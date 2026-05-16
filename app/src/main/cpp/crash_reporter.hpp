#pragma once

#include <string>

namespace lsfg_android {

void ring_log(const char *tag, int level, const char *msg);
void ring_logf(const char *tag, int level, const char *fmt, ...)
    __attribute__((format(printf, 3, 4)));

} // namespace lsfg_android
