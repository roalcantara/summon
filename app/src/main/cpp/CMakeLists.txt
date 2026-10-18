cmake_minimum_required(VERSION 3.22.1)
project(chantwhisper C CXX)

set(CMAKE_CXX_STANDARD 17)
# whisper.cpp and ggml as static libraries inside our one shared library (ADR-0027).
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
add_subdirectory(whisper.cpp)

add_library(chantwhisper SHARED chant_whisper.cpp)
target_link_libraries(chantwhisper PRIVATE whisper log)
# Google Play requires 16 KB page alignment for native code.
target_link_options(chantwhisper PRIVATE "-Wl,-z,max-page-size=16384")
