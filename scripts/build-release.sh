#!/bin/bash
set -e

echo "=========================================="
echo "📦 Building Wassalni Production Release APK"
echo "=========================================="

gradle assembleRelease

echo "✅ Release APK generated at app/build/outputs/apk/release/app-release.apk"
