package Server.TCP;

import Server.Common.*;
import Server.TCP.TCPMessage.Command;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP-based ResourceManager server
 * Handles multiple concurrent client connections using thread pool
 */
public class TCPResourceManager extends ResourceManager {
    private static final int DEFAULT_PORT = 18080;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running = false;
    private int port;

    public TCPResourceManager(String name, int port) {
        super(name);
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool(); // Dynamic thread pool
    }

    /**
     * Start the TCP server
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("'" + m_name + "' TCP ResourceManager server started on port " + port);

            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                    threadPool.submit(new ClientHandler(clientSocket));

                } catch (SocketException e) {
                    if (running) {
                        System.err.println("Socket error accepting connections: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Shutdown the server
     */
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("'" + m_name + "' server shutdown");
        } catch (IOException e) {
            System.err.println("Error during server shutdown: " + e.getMessage());
        }
    }

    /**
     * Inner class to handle individual client connections
     */
    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                System.out.println("Handling client: " + clientSocket.getRemoteSocketAddress());

                while (!clientSocket.isClosed()) {
                    try {
                        // Receive request message
                        TCPMessage request = TCPCommunicator.receiveMessage(clientSocket);
                        System.out.println("Received: " + request);

                        // Process the request and send response bcack
                        TCPMessage response = processRequest(request);
                        TCPCommunicator.sendMessage(clientSocket, response);

                    } catch (EOFException | SocketException e) {
                        // Client disconnected
                        System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid message format from client: " + e.getMessage());
                        break;
                    } catch (IOException e) {
                        System.err.println("Communication error with client: " + e.getMessage());
                        break;
                    }
                }
            } finally {
                TCPCommunicator.closeSocket(clientSocket);
                System.out.println("Client handler finished for: " + clientSocket.getRemoteSocketAddress());
            }
        }
    }

    /**
     * Process a request message and return appropriate response
     */
    private TCPMessage processRequest(TCPMessage request) {
        try {
            Object result = executeCommand(request.getCommand(), request.getArguments());
            return new TCPMessage(request.getMessageId(), result);

        } catch (Exception e) {
            System.err.println("Error processing request " + request.getCommand() + ": " + e.getMessage());
            return new TCPMessage(request.getMessageId(), e.getMessage());
        }
    }

    /**
     * Execute ResourceManager commands based on TCP message
     */
    @SuppressWarnings("unchecked")
    private Object executeCommand(Command command, Object[] args) throws Exception {
        switch (command) {
            case ADD_FLIGHT:
                return addFlight((Integer) args[0], (Integer) args[1], (Integer) args[2]);

            case ADD_CARS:
                return addCars((String) args[0], (Integer) args[1], (Integer) args[2]);

            case ADD_ROOMS:
                return addRooms((String) args[0], (Integer) args[1], (Integer) args[2]);

            case NEW_CUSTOMER:
                if (args.length == 0) {
                    return newCustomer();
                } else {
                    return newCustomer((Integer) args[0]);
                }

            case NEW_CUSTOMER_ID:
                return newCustomer((Integer) args[0]);

            case DELETE_FLIGHT:
                return deleteFlight((Integer) args[0]);

            case DELETE_CARS:
                return deleteCars((String) args[0]);

            case DELETE_ROOMS:
                return deleteRooms((String) args[0]);

            case DELETE_CUSTOMER:
                return deleteCustomer((Integer) args[0]);

            case QUERY_FLIGHT:
                return queryFlight((Integer) args[0]);

            case QUERY_CARS:
                return queryCars((String) args[0]);

            case QUERY_ROOMS:
                return queryRooms((String) args[0]);

            case QUERY_CUSTOMER:
                return queryCustomerInfo((Integer) args[0]);

            case QUERY_FLIGHT_PRICE:
                return queryFlightPrice((Integer) args[0]);

            case QUERY_CARS_PRICE:
                return queryCarsPrice((String) args[0]);

            case QUERY_ROOMS_PRICE:
                return queryRoomsPrice((String) args[0]);

            case RESERVE_FLIGHT:
                return reserveFlight((Integer) args[0], (Integer) args[1]);

            case RESERVE_CAR:
                return reserveCar((Integer) args[0], (String) args[1]);

            case RESERVE_ROOM:
                return reserveRoom((Integer) args[0], (String) args[1]);

            case BUNDLE:
                return bundle((Integer) args[0], (Vector<String>) args[1],
                            (String) args[2], (Boolean) args[3], (Boolean) args[4]);

            case GET_NAME:
                return getName();

            default:
                throw new UnsupportedOperationException("Command not supported: " + command);
        }
    }

    /**
     * Main method to start TCP ResourceManager server
     */
    public static void main(String[] args) {
        String serverName = "Server";
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            serverName = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }

        // Create and start the TCP ResourceManager
        TCPResourceManager server = new TCPResourceManager(serverName, port);

        // Shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        // Start the server (this blocks)
        server.startServer();
    }
}