#!/bin/bash

# Script to start all TCP ResourceManager servers and Middleware
# Usage: ./run_tcp_servers.sh

echo "Starting TCP-based distributed system..."

# Compile the Java classes first
echo "Compiling Java classes..."
javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Starting ResourceManager servers..."

# Start Flight ResourceManager on port 18081
echo "Starting Flight ResourceManager on port 18081..."
java -cp . Server.TCP.TCPResourceManager FlightRM 18081 &
FLIGHT_PID=$!

# Start Car ResourceManager on port 18082
echo "Starting Car ResourceManager on port 18082..."
java -cp . Server.TCP.TCPResourceManager CarRM 18082 &
CAR_PID=$!

# Start Room ResourceManager on port 18083
echo "Starting Room ResourceManager on port 18083..."
java -cp . Server.TCP.TCPResourceManager RoomRM 18083 &
ROOM_PID=$!

# Wait a bit for RMs to start
sleep 2

# Start Middleware on port 17000
echo "Starting Middleware on port 17000..."
java -cp . Server.TCP.TCPMiddleware 17000 &
MIDDLEWARE_PID=$!

echo "All servers started!"
echo "Flight RM PID: $FLIGHT_PID (port 18081)"
echo "Car RM PID: $CAR_PID (port 18082)"
echo "Room RM PID: $ROOM_PID (port 18083)"
echo "Middleware PID: $MIDDLEWARE_PID (port 17000)"
echo ""
echo "To connect client: cd ../Client && ./run_tcp_client.sh"
echo "To stop servers: kill $FLIGHT_PID $CAR_PID $ROOM_PID $MIDDLEWARE_PID"

# Keep script running to maintain server processes
wait