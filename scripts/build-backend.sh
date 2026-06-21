#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Building backend..."
cd "$PROJECT_ROOT/backend"
mvn clean package -DskipTests -q

echo "Copying JAR to frontend/resources..."
mkdir -p "$PROJECT_ROOT/frontend/resources"
cp target/knowvault-backend-0.0.1-SNAPSHOT.jar "$PROJECT_ROOT/frontend/resources/knowvault-backend.jar"

echo "Backend build complete: frontend/resources/knowvault-backend.jar"
