cmake_minimum_required(VERSION 3.22.1)

project(Winlator LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2")

set(LSFG_REF_ROOT "D:/Codex/LSFG-Android-ref")
set(LSFG_APP_CPP_DIR "${LSFG_REF_ROOT}/LSFG-Android-Application/app/src/main/cpp")
set(LSFG_VK_ROOT "${LSFG_REF_ROOT}/lsfg-vk-android")
set(PEPARSE_DIR "${LSFG_VK_ROOT}/thirdparty/pe-parse/pe-parser-library")

set(LSFG_REFERENCE_SOURCES_AVAILABLE OFF)
if(EXISTS "${LSFG_APP_CPP_DIR}/android_shader_loader.cpp" AND
   EXISTS "${LSFG_VK_ROOT}/thirdparty/dxbc/CMakeLists.txt")
    set(LSFG_REFERENCE_SOURCES_AVAILABLE ON)
endif()

set(WINLATOR_OPTIONAL_ANDROID_LIBS "")
if(DEFINED ANDROID_PLATFORM_LEVEL AND ANDROID_PLATFORM_LEVEL GREATER_EQUAL 29)
    list(APPEND WINLATOR_OPTIONAL_ANDROID_LIBS neuralnetworks)
endif()

if(LSFG_REFERENCE_SOURCES_AVAILABLE)
    add_library(volk STATIC "${LSFG_VK_ROOT}/thirdparty/volk/volk.c")
    target_include_directories(volk PUBLIC "${LSFG_VK_ROOT}/thirdparty/volk")
    target_compile_definitions(volk PUBLIC VK_USE_PLATFORM_ANDROID_KHR=1)

    add_library(pe-parse STATIC
            "${PEPARSE_DIR}/src/buffer.cpp"
            "${PEPARSE_DIR}/src/parse.cpp"
            unicode_minimal.cpp
    )
    target_include_directories(pe-parse PUBLIC "${PEPARSE_DIR}/include")
    target_compile_features(pe-parse PUBLIC cxx_std_17)

    add_subdirectory("${LSFG_VK_ROOT}/thirdparty/dxbc" "${CMAKE_BINARY_DIR}/dxbc" EXCLUDE_FROM_ALL)
    add_subdirectory("${LSFG_VK_ROOT}/thirdparty/toml11" "${CMAKE_BINARY_DIR}/toml11" EXCLUDE_FROM_ALL)
    add_subdirectory("${LSFG_VK_ROOT}/framegen" "${CMAKE_BINARY_DIR}/framegen" EXCLUDE_FROM_ALL)

    file(GLOB LSFG_VK_SOURCES
            "${LSFG_VK_ROOT}/src/config/*.cpp"
            "${LSFG_VK_ROOT}/src/extract/*.cpp"
            "${LSFG_VK_ROOT}/src/mini/*.cpp"
            "${LSFG_VK_ROOT}/src/utils/*.cpp"
            "${LSFG_VK_ROOT}/src/*.cpp"
    )
    add_library(lsfg-vk SHARED ${LSFG_VK_SOURCES})
    set_target_properties(lsfg-vk PROPERTIES
            OUTPUT_NAME "lsfg-vk-layer"
            CXX_STANDARD 20
            CXX_STANDARD_REQUIRED ON
    )
    target_compile_definitions(lsfg-vk PRIVATE LSFGVK_ANDROID_WINE=1)
    target_include_directories(lsfg-vk PRIVATE "${LSFG_VK_ROOT}/include")
    get_target_property(TOML11_INCLUDE_DIRS toml11 INTERFACE_INCLUDE_DIRECTORIES)
    target_include_directories(lsfg-vk SYSTEM PRIVATE ${TOML11_INCLUDE_DIRS})
    target_link_libraries(lsfg-vk PRIVATE
            android
            log
            pe-parse
            dxbc
            toml11
            lsfg-vk-framegen
    )
endif()

add_subdirectory(patchelf)
add_subdirectory(adrenotools)

# Add Winlator shared library
add_library(winlator SHARED
        winlator/drawable.c
        winlator/framegen_shader_bridge.cpp
        winlator/framegen_native_bridge.cpp
        winlator/gpu_image.c
        winlator/sysvshared_memory.c
        winlator/xconnector_epoll.c
        winlator/alsa_client.c
        winlator/process_lifecycle.c
        winlator/vulkan.c
        crash_reporter.cpp
        xz/native_xz_stream.c
        xz/xz_embedded/xz_crc32.c
        xz/xz_embedded/xz_crc64.c
        xz/xz_embedded/xz_dec_bcj.c
        xz/xz_embedded/xz_dec_lzma2.c
        xz/xz_embedded/xz_dec_stream.c
)

if(LSFG_REFERENCE_SOURCES_AVAILABLE)
    target_sources(winlator PRIVATE
            "${LSFG_APP_CPP_DIR}/ahb_image_bridge.cpp"
            "${LSFG_APP_CPP_DIR}/android_shader_loader.cpp"
            "${LSFG_APP_CPP_DIR}/android_vk_probe.cpp"
            "${LSFG_APP_CPP_DIR}/android_vk_session.cpp"
            "${LSFG_APP_CPP_DIR}/cpu_postprocess.cpp"
            "${LSFG_APP_CPP_DIR}/gpu_postprocess.cpp"
            "${LSFG_APP_CPP_DIR}/lsfg_render_loop.cpp"
            "${LSFG_APP_CPP_DIR}/nnapi_npu.cpp"
            "${LSFG_APP_CPP_DIR}/nnapi_postprocess.cpp"
    )
endif()

target_compile_options(winlator PRIVATE -Wall -Wextra)
target_compile_definitions(winlator PRIVATE
        XZ_USE_CRC64
        XZ_DEC_CONCATENATED
        XZ_DEC_X86
        XZ_DEC_ARM
        XZ_DEC_ARMTHUMB
        XZ_DEC_ARM64
        XZ_DEC_RISCV
        XZ_DEC_POWERPC
        XZ_DEC_IA64
        XZ_DEC_SPARC
)
target_include_directories(winlator PRIVATE
        xz/xz_embedded
)

if(LSFG_REFERENCE_SOURCES_AVAILABLE)
    target_include_directories(winlator PRIVATE
        "${LSFG_APP_CPP_DIR}"
        "${LSFG_VK_ROOT}/framegen/include"
        "${LSFG_VK_ROOT}/framegen/public"
        "${LSFG_VK_ROOT}/framegen/v3.1_include"
        "${LSFG_VK_ROOT}/framegen/v3.1p_include"
        "${LSFG_VK_ROOT}/thirdparty/volk"
        "${PEPARSE_DIR}/include"
        xz/xz_embedded
    )
endif()

target_link_libraries(winlator
        log
        android
        jnigraphics
        aaudio
        EGL
        GLESv2
        GLESv3
        adrenotools
        ${WINLATOR_OPTIONAL_ANDROID_LIBS}
)

if(LSFG_REFERENCE_SOURCES_AVAILABLE)
    target_link_libraries(winlator
        vulkan
        dxbc
        lsfg-vk-framegen
        pe-parse
        volk
    )
endif()

# Fake evdev input shim used by the pb_controller_fix controller path.
add_library(fakeinput SHARED
        winlator/fakeinput.cpp
)
target_compile_options(fakeinput PRIVATE -Wall -Wextra -fvisibility=hidden)
target_link_libraries(fakeinput log dl)

# Evshim - controller support shim (loaded via LD_PRELOAD, uses dlopen for SDL2)
add_library(evshim SHARED
        winlator/evshim.c
)
target_compile_options(evshim PRIVATE -Wall -Wextra -fvisibility=hidden)
target_link_libraries(evshim log dl)
