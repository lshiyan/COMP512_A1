echo '  $1 - hostname of Flights'
echo '  $2 - hostname of Cars'
echo '  $3 - hostname of Rooms'

java -cp ../bin -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCPMiddleware $1 $2 $3
