#!/bin/bash
set -e

echo "=========================================="
echo "🎯 Building Wassalni Google Play App Bundle (AAB)"
echo "=========================================="

gradle bundleRelease

echo "✅ AAB created at app/build/outputs/bundle/release/app-release.aab"
