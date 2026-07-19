#!/bin/bash
# Script facilitant la compilation des bibliothèques natives
# Nécessite Android NDK 27+

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
THIRD_PARTY="$SCRIPT_DIR/../third_party"
JNILIBS="$SCRIPT_DIR/../app/src/main/jniLibs/arm64-v8a"

mkdir -p "$THIRD_PARTY" "$JNILIBS"

build_whisper() {
    echo "=== Build whisper.cpp ==="
    cd "$THIRD_PARTY"
    [ -d whisper.cpp ] || git clone --depth 1 https://github.com/ggerganov/whisper.cpp
    cd whisper.cpp
    mkdir -p build && cd build
    cmake .. \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-29 \
        -DWHISPER_SUPPORT_VULKAN=ON \
        -DCMAKE_BUILD_TYPE=Release
    make -j"$(nproc)" whisper
    cp src/libwhisper.so "$JNILIBS/"
    echo "✓ whisper.cpp -> $JNILIBS/libwhisper.so"
}

build_llama() {
    echo "=== Build llama.cpp ==="
    cd "$THIRD_PARTY"
    [ -d llama.cpp ] || git clone --depth 1 https://github.com/ggerganov/llama.cpp
    cd llama.cpp
    mkdir -p build && cd build
    cmake .. \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-29 \
        -DLLAMA_VULKAN=ON \
        -DCMAKE_BUILD_TYPE=Release
    make -j"$(nproc)" llama
    cp src/libllama.so "$JNILIBS/"
    echo "✓ llama.cpp -> $JNILIBS/libllama.so"
}

case "${1:-all}" in
    whisper) build_whisper ;;
    llama)   build_llama ;;
    all)     build_whisper; build_llama ;;
    *)       echo "Usage: $0 {whisper|llama|all}"; exit 1 ;;
esac

echo "=== Terminé ==="
