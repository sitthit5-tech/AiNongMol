# Kotlin-LlamaCpp

### Implementing GGUF Local Inference into Android Devices with EASE

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

**Native AI inference for Android devices**

Run GGUF models directly on your Android device with optimized performance and zero cloud dependency!

This is an Android binding for [llama.cpp](https://github.com/ggerganov/llama.cpp) written in Kotlin, designed specifically for native Android applications. Built to leverage modern hardware capabilities, this library brings efficient large language model inference to mobile devices. The project is inspired by [cui-llama.rn](https://github.com/Vali-98/cui-llama.rn) and [llama.cpp](https://github.com/ggerganov/llama.cpp): Inference of [LLaMA](https://arxiv.org/abs/2302.13971) and multimodal models in pure C/C++, specifically tailored for native Android development in Kotlin.

[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/P5P6149YRQ)

## Changelog
### v0.4.0 (Latest)
- **Modernized Core**: Native codebase synchronized with the latest `llama.cpp` upstream (via `cui-llama.rn`).
- **Multimodal Support**: Full support for vision/multimodal models (e.g., LLaVA) using `mmproj` files.
- **Improved File Handling**: Migrated to a robust File Descriptor (FD) passing mechanism, bypassing Android's scoped storage restrictions and `/proc/self/fd` limitations.
- **Architecture Support**: Expanded support beyond ARM-only; optimized for 64-bit platforms (`arm64-v8a` and `x86_64`).
- **Real-time Streaming**: Enhanced JNI streaming logic with robust UTF-8 buffering to prevent crashes during token generation.
- **UI State Feedback**: Improved `LlamaHelper` to provide immediate feedback during long-running image analysis/projection phases.

## Why On-Device AI?

Modern Android devices (Snapdragon, MediaTek, Exynos, Tensor) now possess the power to run sophisticated AI models locally. Kotlin-LlamaCpp enables:

- **True On-Device AI**: Run large language models entirely on your phone or tablet—no internet required, complete privacy.
- **Hardware-Accelerated Inference**: Automatic detection and utilization of CPU features (i8mm, dotprod) for hardware-accelerated inference on ARM and x86.
- **Mobile-First Design**: Context management and batch interruption designed for the constraints of mobile processors.
- **Multimodal Capabilities**: Analyze images locally using multimodal projectors (`mmproj`).

## Features

- **Multi-Architecture Support**: Built for `arm64-v8a` and `x86_64` with automatic CPU feature detection.
- **Multimodal Inference**: Support for LLaVA and other vision models with per-prompt image injection.
- **Efficient Mobile Inference**: Context Shift support from [kobold.cpp](https://github.com/LostRuins/koboldcpp) enables longer conversations without memory overflow.
- **Kotlin-First Design**: Helper class to handle initialization and context management seamlessly.
- **Seamless Android Integration**: Uses `ContentResolver` and File Descriptors to work naturally with Android 11+ scoped storage.

## Demo App
You can find a complete, ready-to-build demo application in the [`/app`](https://github.com/ljcamargo/kotlinllamacpp/tree/master/app) directory.
The demo showcases model loading, multimodal image selection, handling inference in a ViewModel, and displaying generated text in a Jetpack Compose UI.

## Installation

Add the following to your project's `build.gradle`:
```gradle
dependencies {
    implementation 'io.github.ljcamargo:llamacpp-kotlin:0.4.0'
}
```

## Usage

### Basic Text Completion
```kotlin
// Initialize and load
llamaHelper.load(
    path = modelUriString, // e.g. from a file picker
    contextLength = 2048,
) {
    // Model loaded!
}

// Generate
llamaHelper.predict("Why is the sky blue?")
```

### Multimodal (Image Processing)
To use multimodal features, you need a base GGUF model and its corresponding `mmproj` file.
```kotlin
// Load with multimodal projector
llamaHelper.load(
    path = baseModelUri,
    contextLength = 4096,
    mmprojPath = mmprojUri // Pass the projector file here
) {
    // Multimodal context ready!
}

// Generate with an image
llamaHelper.predict(
    prompt = "Describe this image in detail:",
    imagePath = selectedImageUri // Pass the image URI here
)
```

## Native Code Maintenance
The native C++ core is synchronized with the latest `llama.cpp` developments. For instructions on how to update or rebuild the native components, see the [llamaCpp README](llamaCpp/README.md).

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
MIT

## Acknowledgments
This project builds upon the work of:
- [llama.cpp](https://github.com/ggerganov/llama.cpp) by Georgi Gerganov
- [cui-llama.rn](https://github.com/Vali-98/cui-llama.rn) by Vali-98
- [llama.rn](https://github.com/mybigday/llama.rn)
