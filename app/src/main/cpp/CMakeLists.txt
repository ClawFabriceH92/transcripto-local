cmake_minimum_required(VERSION 3.22)
project("transcripto_native")

# Bibliothèques natives pré-compilées (whisper.cpp + llama.cpp)
# Liées dynamiquement via jniLibs, pas compilées ici.

add_library(whisper_jni SHARED whisper_jni.cpp)
target_include_directories(whisper_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp/include
)
target_link_libraries(whisper_jni
    log
    whisper
)

add_library(llama_jni SHARED llama_jni.cpp)
target_include_directories(llama_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/llama.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/llama.cpp/include
)
target_link_libraries(llama_jni
    log
    llama
)
