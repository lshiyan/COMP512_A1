#!/bin/bash

# Automated TCP Distributed Deployment Script
# Usage: ./run_tcp_distributed.sh

# SOCS MACHINES - Use full hostnames (not SSH aliases)
MACHINES=(
    "10.69.38.159"
    "10.69.38.228"
    "10.69.38.161"
    "10.69.38.160"
)

# SSH aliases for connection
FLIGHT_HOST=${MACHINES[0]}
CAR_HOST=${MACHINES[1]}
ROOM_HOST=${MACHINES[2]}
MIDDLEWARE_HOST=${MACHINES[3]}

# Internal network names for inter-machine communication
# (These are what the middleware will use to connect to RMs)
FLIGHT_INTERNAL="10.69.38.159"   # trot-open-09 internal IP
CAR_INTERNAL="10.69.38.228"      # trot-open-36 internal IP
ROOM_INTERNAL="10.69.38.161"     # trot-open-11 internal IP

echo "=========================================="
echo "TCP Distributed Deployment"
echo "=========================================="
echo "Flight RM:    $FLIGHT_HOST"
echo "Car RM:       $CAR_HOST"
echo "Room RM:      $ROOM_HOST"
echo "Middleware:   $MIDDLEWARE_HOST"
echo "=========================================="

# Create tmux session with split windows
tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "ssh -t $FLIGHT_HOST \"cd $(pwd) > /dev/null; echo 'Connected to Flight RM on'; hostname; ./run_tcp_flight.sh\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t $CAR_HOST \"cd $(pwd) > /dev/null; echo 'Connected to Car RM on'; hostname; ./run_tcp_car.sh\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t $ROOM_HOST \"cd $(pwd) > /dev/null; echo 'Connected to Room RM on'; hostname; ./run_tcp_room.sh\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "ssh -t $MIDDLEWARE_HOST \"cd $(pwd) > /dev/null; echo 'Connected to Middleware on'; hostname; sleep 2; ./run_tcp_middleware.sh 17000 $FLIGHT_INTERNAL $CAR_INTERNAL $ROOM_INTERNAL\"" C-m \;

echo ""
echo "All TCP servers deployed!"
echo ""
echo "To connect client from any machine:"
echo "  ssh <any-socs-machine>"
echo "  cd $(pwd)/../Client"
echo "  ./run_tcp_client.sh $MIDDLEWARE_HOST 17000"
echo ""
echo "To stop all servers: Close tmux session (Ctrl+B, then type 'kill-session')"