#!/bin/bash
# Build tun2socks Go binary for Android architectures
# Run this on a machine with Go 1.22+ and Android NDK installed

set -e

export NDK=/opt/android-ndk
export GOMOBILE=$GOPATH/pkg/gomobile

# Build for arm64-v8a
GOOS=android GOARCH=arm64 CGO_ENABLED=1 \
  CC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang \
  go build -ldflags="-s -w" -o ../app/src/main/jniLibs/arm64-v8a/libtun2socks.so

# Build for armeabi-v7a
GOOS=android GOARCH=arm CGO_ENABLED=1 \
  CC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi30-clang \
  go build -ldflags="-s -w" -o ../app/src/main/jniLibs/armeabi-v7a/libtun2socks.so

# Build for x86_64
GOOS=android GOARCH=amd64 CGO_ENABLED=1 \
  CC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android30-clang \
  go build -ldflags="-s -w" -o ../app/src/main/jniLibs/x86_64/libtun2socks.so

echo "Build complete. Binaries placed in app/src/main/jniLibs/"
