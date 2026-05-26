#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PID=""

cleanup() {
    echo ""
    echo "Stopping backend..."
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    echo "Done. (Docker DB is still running. Use 'docker-compose down' to stop it.)"
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "=== Personal Account Dashboard ==="
echo ""

# 1. Start database
echo "[1/3] Starting database..."
docker-compose -f "$PROJECT_ROOT/docker-compose.yml" up -d
echo "Database is running on port 3306."
echo ""

# 2. Start backend
echo "[2/3] Starting backend..."
cd "$PROJECT_ROOT/backend"
./gradlew bootRun --console=plain &
BACKEND_PID=$!
sleep 5
echo "Backend is starting on port 8080. (PID: $BACKEND_PID)"
echo ""

# 3. Start frontend
echo "[3/3] Starting frontend..."
cd "$PROJECT_ROOT/frontend"
if [ ! -d "node_modules" ]; then
    echo "Installing npm dependencies..."
    npm install
fi
echo "Frontend dev server starting on port 5173..."
echo ""
echo "=== All services running. Press Ctrl+C to stop. ==="
echo ""
npm run dev
