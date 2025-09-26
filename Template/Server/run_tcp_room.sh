#!/bin/bash

# Script to start TCP Room ResourceManager
# Usage: ./run_tcp_room.sh [port]

PORT=${1:-18083}

echo "Starting TCP Room ResourceManager on port $PORT..."

# Compile if needed
javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java

# Start Room ResourceManager
java -cp . Server.TCP.TCPResourceManager RoomRM $PORT