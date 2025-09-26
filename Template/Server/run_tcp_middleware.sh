#!/bin/bash

# Script to start TCP Middleware
# Usage: ./run_tcp_middleware.sh [port]

PORT=${1:-17000}

echo "Starting TCP Middleware on port $PORT..."

# Compile if needed
javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java

# Start Middleware
java -cp . Server.TCP.TCPMiddleware $PORT