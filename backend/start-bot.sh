#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

npm install

echo
echo "Starting Wasalni Telegram Bot..."
echo

node src/bot.js
