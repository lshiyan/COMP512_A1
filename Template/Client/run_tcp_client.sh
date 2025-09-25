#!/bin/bash

# Script to start TCP Client
# Usage: ./run_tcp_client.sh [middleware_host] [middleware_port]

HOST=${1:-localhost}
PORT=${2:-17000}

echo "Starting TCP Client connecting to Middleware at $HOST:$PORT..."

# Compile client and required TCP classes
javac -cp ../Server:. Client/*.java ../Server/Server/TCP/*.java ../Server/Server/Interface/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Start TCP Client
java -cp ../Server:. Client.TCPClient $HOST $PORT