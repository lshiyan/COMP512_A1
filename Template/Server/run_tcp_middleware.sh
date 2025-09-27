#!/bin/bash

# Script to start TCP Middleware with distributed RM support
# Usage: ./run_tcp_middleware.sh [port] [flight_host] [car_host] [room_host]
# Example: ./run_tcp_middleware.sh 17000 flight-server.com car-server.com room-server.com

PORT=${1:-17000}
FLIGHT_HOST=${2:-localhost}
CAR_HOST=${3:-localhost}
ROOM_HOST=${4:-localhost}

echo "Starting TCP Middleware for distributed deployment..."
echo "  Port: $PORT"
echo "  Flight RM: $FLIGHT_HOST:18081"
echo "  Car RM: $CAR_HOST:18082"
echo "  Room RM: $ROOM_HOST:18083"

# Compile if needed
javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java

# Start Middleware with distributed configuration
java -cp . Server.TCP.TCPMiddleware $PORT $FLIGHT_HOST $CAR_HOST $ROOM_HOST