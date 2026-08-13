#!/bin/bash
set -e

echo "=========================================="
echo "⚙️ Setting up Wassalni Environment"
echo "=========================================="

if [ ! -f "keystore.properties" ]; then
    echo "Creating keystore.properties from template..."
    cp keystore.properties.example keystore.properties
fi

if [ ! -f ".env" ]; then
    echo "Creating .env from template..."
    cp .env.example .env
fi

echo "✅ Environment configured."
