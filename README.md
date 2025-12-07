````markdown
# Objecter 🕵️‍♂️📷

**Objecter** is an Android application for **real-time object detection**. It uses **OpenCV** and **TensorFlow Lite** for native C++ image processing, while the interface and camera handling are implemented in **Kotlin** with **CameraX**.

---

## Project Overview 📝

The app captures live camera frames and processes them entirely in C++ via JNI. TensorFlow Lite performs object detection, returning bounding boxes, class IDs, and confidence scores. The Kotlin activity (`ObjectDetectionActivity`) displays the camera feed and overlays detection results in real-time.

**Key Points:**
- ⚡ Native C++ processing ensures low-latency detection.
- 📸 CameraX integration provides smooth camera preview and lifecycle management.
- 🖍️ Bounding boxes and labels are drawn on a transparent `SurfaceView`.

---

## Technologies Used 🛠️

- **OpenCV**: An open-source computer vision library that provides tools for image processing, manipulation, and analysis. In Objecter, OpenCV handles frame conversion, resizing, and rotation. 🖼️
- **TensorFlow Lite**: A lightweight machine learning framework for mobile and embedded devices. It enables fast inference of deep learning models, such as object detection models, directly on the device. 🤖

---

## Important Note ⚠️

This application **does not support Android devices requiring 16 KB aligned memory** for native processing. Running on such devices may lead to crashes or incorrect behavior.

---

## Setup 🔧

Before building the project, unzip the provided `tools.zip` file and place its contents under the `jni/` directory in the project root:

```text
project-root/
└── jni/
    └── ... (contents of tools.zip)
````

This ensures all native dependencies are available for compilation.

---

## Usage 🚀

1. Build and run the project in Android Studio.
2. Grant camera permissions when prompted. ✅
3. The app will display detected objects live on the camera preview. 🎯

---

## Preview 📸

![Library Preview](ASSETS/libraries_preview.png)

```
```
