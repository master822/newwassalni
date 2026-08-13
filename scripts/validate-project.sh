#!/bin/bash
set -e

echo "=========================================="
echo "🧪 Running Tests & Validation"
echo "=========================================="

gradle :app:testDebugUnitTest

echo "✅ All Robolectric tests and unit checks passed!"
