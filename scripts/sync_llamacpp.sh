#!/bin/bash -e

# This script syncs the native llama.cpp codebase from the temporary cui-llama.rn clone
# to our local llamaCpp library.

ROOT_DIR=$(pwd)
TEMP_DIR="$ROOT_DIR/temp_cui_llama_rn"
TARGET_DIR="$ROOT_DIR/llamaCpp/src/main/cpp/lib"

if [ ! -d "$TEMP_DIR" ]; then
    echo "Error: temp_cui_llama_rn directory not found. Please clone it first."
    exit 1
fi

echo "Cleaning up target directory: $TARGET_DIR"
rm -rf "$TARGET_DIR"/*

echo "Copying files from cui-llama.rn/cpp..."

# Copy core files (including .hpp)
cp "$TEMP_DIR"/cpp/*.h "$TARGET_DIR"/
cp "$TEMP_DIR"/cpp/*.hpp "$TARGET_DIR"/
cp "$TEMP_DIR"/cpp/*.c "$TARGET_DIR"/
cp "$TEMP_DIR"/cpp/*.cpp "$TARGET_DIR"/

# Copy subdirectories
mkdir -p "$TARGET_DIR/common"
cp -r "$TEMP_DIR"/cpp/common/* "$TARGET_DIR/common/"

mkdir -p "$TARGET_DIR/tools/mtmd"
cp -r "$TEMP_DIR"/cpp/tools/mtmd/* "$TARGET_DIR/tools/mtmd/"

mkdir -p "$TARGET_DIR/ggml-cpu"
cp -r "$TEMP_DIR"/cpp/ggml-cpu/* "$TARGET_DIR/ggml-cpu/"

mkdir -p "$TARGET_DIR/nlohmann"
cp -r "$TEMP_DIR"/cpp/nlohmann/* "$TARGET_DIR/nlohmann/"

# If there are models directory (sometimes used for templates)
if [ -d "$TEMP_DIR/cpp/models" ]; then
    cp -r "$TEMP_DIR"/cpp/models "$TARGET_DIR/"
fi

echo "Sync completed successfully!"
