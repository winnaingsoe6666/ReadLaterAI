#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Step 1: Build Backend ==="
bash "$SCRIPT_DIR/build-backend.sh"

echo "=== Step 2: Install Frontend Dependencies ==="
cd "$PROJECT_ROOT/frontend"
npm install

echo "=== Step 3: Build Frontend + Electron ==="
npm run build

echo "=== Build Complete ==="
echo "To package for a platform, run:"
echo "  cd frontend && npm run package:win"
echo "  cd frontend && npm run package:mac"
echo "  cd frontend && npm run package:linux"
