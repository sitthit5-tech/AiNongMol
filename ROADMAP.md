# Roadmap: Updating Native llama.cpp and Multimodal Support

This roadmap outlines the steps to modernize the native `llama.cpp` implementation in the project, moving away from the outdated `cui-llama.rn` fork and establishing a direct sync mechanism with the upstream `llama.cpp` repository.

## Phase 1: Preparation and Research
- [x] Create `ROADMAP.md`
- [x] Analyze the file structure of the current native implementation.
- [x] Research the sync mechanism used by `cui-llama.rn` (documented in their `cpp/README.md`).
- [x] Explore the latest `llama.cpp` repository structure and identifying core files needed.
- [x] Clone `cui-llama.rn` and inspect its sync script and submodules.

## Phase 2: Modernizing the Native Codebase
- [x] Implement an automated sync script to fetch/sync core `llama.cpp` files.
  - [x] Analyze `cui-llama.rn/scripts/bootstrap.sh`.
  - [x] Create `scripts/sync_llamacpp.sh` in our root, adapting the logic from `bootstrap.sh`.
  - [x] Run the sync script to update `llamaCpp/src/main/cpp/lib`.
- [x] Identify and isolate the "binding" layer.
  - [x] Inspect `temp_cui_llama_rn/cpp/rn-*` files.
  - [x] Inspect `temp_cui_llama_rn/android/` for JNI and binding changes.
- [x] Fix compilation errors in the binding layer caused by API changes in the new `llama.cpp`.
  - [x] Update `CMakeLists.txt` to include all new source files.
  - [x] Rewrite `jni.cpp` to match the new `llama_rn_context` and `common_params`.
  - [x] Fix missing `.hpp` files in sync script.
  - [x] Address API changes (`embeddings` -> `embedding`, `mmproj` path struct, etc.).
  - [ ] Adapt Kotlin classes (`LlamaContext.kt`, `LlamaAndroid.kt`) to the new JNI signatures (if needed).

## Phase 3: Updating Android Bindings
- [x] Compare current JNI implementation (`jni.cpp`) with the latest from `cui-llama.rn` for reference.
- [ ] Adapt `LlamaContext.kt` and `LlamaAndroid.kt` to match the new JNI signatures (Added parallel slot support?).
- [ ] Update `LlamaHelper.kt` and `MainViewModel.kt` for per-prompt multimodal support.
- [ ] Ensure proper support for File Descriptor (FD) passing in the new implementation.

## Phase 4: Multimodal Implementation
- [x] Implement the `llava` (multimodal) support using the latest `llama.cpp` patterns in JNI.
- [ ] Update `LlamaHelper.kt` to handle the new multimodal loading sequence (passing images during prediction).
- [ ] Update UI to allow selecting an image for the current prompt.
- [ ] Verify image processing and projection handling in the native layer.

## Phase 5: Testing and Validation
- [ ] Test with standard GGUF models.
- [ ] Test with multimodal models (LLaVA) using the updated sample app.
- [ ] Optimize performance and memory usage for Android.

## Constraints and Guidelines
- **Minimize Token Usage**: Avoid reading large native files in their entirety. Focus on headers (`.h`) and READMEs.
- **Maintain FD Passing**: Newer Android SDKs require passing raw FDs; do not revert to path-based loading.
- **Modular Bindings**: Keep our JNI/Kotlin layer separate from core `llama.cpp` files to ease future updates.
