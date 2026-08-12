#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

npm install

echo
echo "Starting Wasalni API..."
echo

node src/server.js
