#!/bin/bash

# Script to start TCP Car ResourceManager
# Usage: ./run_tcp_car.sh [port]

PORT=${1:-18082}

echo "Starting TCP Car ResourceManager on port $PORT..."

# Compile if needed
javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java

# Start Car ResourceManager
java -cp . Server.TCP.TCPResourceManager CarRM $PORT