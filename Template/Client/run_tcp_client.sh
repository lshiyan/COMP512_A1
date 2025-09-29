#!/bin/bash

# Usage: ./run_tcp_client.sh [server_host] [server_name]

# Run the TCP client
java -cp ../bin:. Client.TCPClient $1 $2