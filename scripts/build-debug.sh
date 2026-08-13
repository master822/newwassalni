#!/bin/bash
set -e

echo "=========================================="
echo "🚀 Building Wassalni Debug APK"
echo "=========================================="

gradle assembleDebug

echo "✅ Debug APK built successfully at app/build/outputs/apk/debug/app-debug.apk"
