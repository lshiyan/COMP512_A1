#!/bin/bash

# Simple integration test for TCP system
echo "TCP System Integration Test"
echo "==========================="

# Check if Java classes are compiled
echo "1. Checking compilation..."
cd Server
if ! ls Server/TCP/*.class &>/dev/null; then
    echo "   Compiling server classes..."
    javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java
fi

cd ../Client
if ! ls Client/*.class &>/dev/null; then
    echo "   Compiling client classes..."
    javac -cp ../Server:. Client/*.java
fi
cd ..

echo "   ✓ Compilation complete"

# Function to cleanup processes
cleanup() {
    echo -e "\nCleaning up processes..."
    if [ ! -z "$FLIGHT_PID" ]; then kill $FLIGHT_PID 2>/dev/null; fi
    if [ ! -z "$CAR_PID" ]; then kill $CAR_PID 2>/dev/null; fi
    if [ ! -z "$ROOM_PID" ]; then kill $ROOM_PID 2>/dev/null; fi
    if [ ! -z "$MIDDLEWARE_PID" ]; then kill $MIDDLEWARE_PID 2>/dev/null; fi
    sleep 1
    echo "   ✓ Cleanup complete"
}

# Set trap to cleanup on exit
trap cleanup EXIT

echo "2. Starting ResourceManager servers..."

cd Server

# Start ResourceManagers in background
java -cp . Server.TCP.TCPResourceManager FlightRM 18081 > flight.log 2>&1 &
FLIGHT_PID=$!

java -cp . Server.TCP.TCPResourceManager CarRM 18082 > car.log 2>&1 &
CAR_PID=$!

java -cp . Server.TCP.TCPResourceManager RoomRM 18083 > room.log 2>&1 &
ROOM_PID=$!

echo "   Flight RM started (PID: $FLIGHT_PID)"
echo "   Car RM started (PID: $CAR_PID)"
echo "   Room RM started (PID: $ROOM_PID)"

# Wait for RMs to start
sleep 2

echo "3. Starting Middleware..."
java -cp . Server.TCP.TCPMiddleware 17000 > middleware.log 2>&1 &
MIDDLEWARE_PID=$!
echo "   Middleware started (PID: $MIDDLEWARE_PID)"

# Wait for middleware to start
sleep 2

echo "4. Testing system status..."

# Check if all processes are running
if ! kill -0 $FLIGHT_PID 2>/dev/null; then
    echo "   ✗ Flight RM failed to start"
    exit 1
fi

if ! kill -0 $CAR_PID 2>/dev/null; then
    echo "   ✗ Car RM failed to start"
    exit 1
fi

if ! kill -0 $ROOM_PID 2>/dev/null; then
    echo "   ✗ Room RM failed to start"
    exit 1
fi

if ! kill -0 $MIDDLEWARE_PID 2>/dev/null; then
    echo "   ✗ Middleware failed to start"
    exit 1
fi

# Check if ports are listening
if ! lsof -i :18081 > /dev/null 2>&1; then
    echo "   ✗ Flight RM not listening on port 18081"
    exit 1
fi

if ! lsof -i :18082 > /dev/null 2>&1; then
    echo "   ✗ Car RM not listening on port 18082"
    exit 1
fi

if ! lsof -i :18083 > /dev/null 2>&1; then
    echo "   ✗ Room RM not listening on port 18083"
    exit 1
fi

if ! lsof -i :17000 > /dev/null 2>&1; then
    echo "   ✗ Middleware not listening on port 17000"
    exit 1
fi

echo "   ✓ All services running and listening"

echo ""
echo "SUCCESS: TCP system is running!"
echo ""
echo "Services:"
echo "  - Flight RM:  localhost:18081 (PID: $FLIGHT_PID)"
echo "  - Car RM:     localhost:18082 (PID: $CAR_PID)"
echo "  - Room RM:    localhost:18083 (PID: $ROOM_PID)"
echo "  - Middleware: localhost:17000 (PID: $MIDDLEWARE_PID)"
echo ""
echo "To test with client:"
echo "  cd Client && ./run_tcp_client.sh"
echo ""
echo "Press Ctrl+C to stop all services..."

# Keep running until interrupted
while true; do
    sleep 1
    # Check if any service died
    if ! kill -0 $FLIGHT_PID 2>/dev/null || ! kill -0 $CAR_PID 2>/dev/null || ! kill -0 $ROOM_PID 2>/dev/null || ! kill -0 $MIDDLEWARE_PID 2>/dev/null; then
        echo "A service has stopped unexpectedly!"
        exit 1
    fi
done