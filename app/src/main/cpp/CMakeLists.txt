cmake_minimum_required(VERSION 3.22.1)

project("pixelforge_native" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

add_library(
    pixelforge_native
    SHARED
    pixelforge_native.cpp
)

target_include_directories(
    pixelforge_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(pixelforge_native PRIVATE PIXELFORGE_ABI_ARM64=1)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_definitions(pixelforge_native PRIVATE PIXELFORGE_ABI_ARM32=1)
    target_compile_options(pixelforge_native PRIVATE -mfpu=neon)
elseif(ANDROID_ABI STREQUAL "x86_64")
    target_compile_definitions(pixelforge_native PRIVATE PIXELFORGE_ABI_X86_64=1)
elseif(ANDROID_ABI STREQUAL "x86")
    target_compile_definitions(pixelforge_native PRIVATE PIXELFORGE_ABI_X86=1)
endif()

find_library(log-lib log)

target_link_libraries(
    pixelforge_native
    ${log-lib}
)
