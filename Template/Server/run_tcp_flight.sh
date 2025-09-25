#!/bin/bash

# Script to start TCP Flight ResourceManager
# Usage: ./run_tcp_flight.sh [port]

PORT=${1:-18081}

echo "Starting TCP Flight ResourceManager on port $PORT..."

# Compile if needed
javac -cp . Server/TCP/*.java Server/Common/*.java Server/Interface/*.java

# Start Flight ResourceManager
java -cp . Server.TCP.TCPResourceManager FlightRM $PORT