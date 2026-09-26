#!/bin/bash
set -e

echo "======================================================="
echo "    SwiftTrack - Docker CI/CD APK Builder (Linux/Mac)  "
echo "======================================================="
echo ""

echo "[1/3] Building the Android environment inside Docker..."
echo "(This ensures a sterile, error-free build. It might take a few minutes.)"
docker build -f Dockerfile.android -t swifttrack-android-builder .

echo ""
echo "[2/3] Extracting the compiled APK from the container..."
docker create --name temp-builder swifttrack-android-builder

mkdir -p outputs
docker cp temp-builder:/app/app/build/outputs/apk/debug/app-debug.apk ./outputs/SwiftTrack-Debug.apk

docker rm temp-builder

echo ""
echo "[3/3] Success!"
echo "Your fully compiled APK is ready and located in the 'outputs/' folder."
echo "You can now install it on your physical device using: adb install outputs/SwiftTrack-Debug.apk"
echo ""
