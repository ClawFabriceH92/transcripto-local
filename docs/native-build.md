# transcripto-local

## Build des bibliothèques natives

### whisper.cpp

```bash
git clone https://github.com/ggerganov/whisper.cpp third_party/whisper.cpp
cd third_party/whisper.cpp
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
         -DANDROID_ABI=arm64-v8a \
         -DANDROID_PLATFORM=android-29 \
         -DWHISPER_SUPPORT_VULKAN=ON \
         -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
```

### llama.cpp

```bash
git clone https://github.com/ggerganov/llama.cpp third_party/llama.cpp
cd third_party/llama.cpp
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
         -DANDROID_ABI=arm64-v8a \
         -DANDROID_PLATFORM=android-29 \
         -DLLAMA_VULKAN=ON \
         -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
```

Les `.so` générées sont placées dans `app/src/main/jniLibs/arm64-v8a/`.

### Structure JNI

```
app/src/main/
├── jniLibs/
│   └── arm64-v8a/
│       ├── libwhisper.so
│       └── libllama.so
├── cpp/
│   ├── whisper_jni.cpp
│   └── llama_jni.cpp
```

## Ajout d'un nouveau modèle

1. Ajouter un profil dans `ModelProfile.kt` (tableau `ALL_PROFILES`)
2. Ajouter l'URL de téléchargement et le hash SHA-256 dans `ModelManager.kt`
3. Le modèle devient disponible dans les paramètres

## Tests

```bash
./gradlew test                    # Tests unitaires
./gradlew connectedAndroidTest    # Tests instrumentalisés
```
