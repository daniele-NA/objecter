cmake_minimum_required(VERSION 3.22.1)
project(objecter LANGUAGES C CXX)

find_library(android-log-lib log)
find_library(system-lib m)
find_library(android-lib android)

# OPEN CV
set(OpenCV_STATIC ON)
set(OpenCV_DIR tools/open-cv/sdk/native/jni)
find_package(OpenCV REQUIRED)

# TENSOR FLOW LITE
add_library(libtensorflowlite_c SHARED IMPORTED)
set_target_properties(libtensorflowlite_c PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/tools/tflite-dist/libs/android/${ANDROID_ABI}/libtensorflowlite_c.so
)

add_library(objecter SHARED
        core/Shared.cpp

        detector/ObjectDetectionActivity.cpp
        detector/ObjectDetector.cpp
)

# IN ORDER TO HAVE DIRECT IMPORTS & NOT   #include "../../tools/tflite-dist/core etc etc"
target_include_directories(objecter PRIVATE
        ${OpenCV_INCLUDE_DIRS}
        tools/tflite-dist/include
)

target_link_libraries(objecter
        ${android-log-lib}
        ${system-lib}
        ${android-lib}

        libtensorflowlite_c
        ${OpenCV_LIBS}
)