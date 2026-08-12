#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"

echo "Starting API temporarily..."

node src/server.js &
PID=$!

cleanup() {
    kill "$PID" 2>/dev/null || true
}

trap cleanup EXIT

sleep 2

echo
echo "Health:"
curl -fsS http://127.0.0.1:8080/health

echo
echo
echo "Stats:"
curl -fsS http://127.0.0.1:8080/api/stats

echo
echo
echo "✅ API test passed."
