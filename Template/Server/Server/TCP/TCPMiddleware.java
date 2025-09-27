package Server.TCP;

import Server.Common.Trace;
import Server.TCP.TCPMessage.Command;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * TCP Middleware server - non-blocking architecture
 * Sits between clients and ResourceManagers
 * Handles request routing and manages customer data
 */
public class TCPMiddleware {
    private static final int DEFAULT_CLIENT_PORT = 17000;

    // ResourceManager connection
    private static final int FLIGHT_RM_PORT = 18081;
    private static final int CAR_RM_PORT = 18082;
    private static final int ROOM_RM_PORT = 18083;

    // Configurable RM hosts for distributed deployment
    private String flightRMHost;
    private String carRMHost;
    private String roomRMHost;

    private ServerSocket clientServerSocket;
    private ExecutorService clientThreadPool;
    private ExecutorService rmThreadPool;
    private boolean running = false;
    private int clientPort;

    // Connection pools to ResourceManagers
    private Map<String, BlockingQueue<Socket>> rmConnectionPools;
    private Map<Integer, CompletableFuture<TCPMessage>> pendingRequests;
    private Map<Integer, Socket> clientSockets;

    // Customer management (handled at middleware level)
    private Map<Integer, Map<String, Object>> customerData;
    private int nextCustomerId = 1000;

    public TCPMiddleware(int clientPort) {
        this(clientPort, "localhost", "localhost", "localhost");
    }

    public TCPMiddleware(int clientPort, String flightHost, String carHost, String roomHost) {
        this.clientPort = clientPort;
        this.flightRMHost = flightHost;
        this.carRMHost = carHost;
        this.roomRMHost = roomHost;
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.rmThreadPool = Executors.newCachedThreadPool();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.clientSockets = new ConcurrentHashMap<>();
        this.customerData = new ConcurrentHashMap<>();
        this.rmConnectionPools = new ConcurrentHashMap<>();

        // Initialize connection pools
        initializeConnectionPools();
    }

    /**
     * Initialize connection pools to ResourceManagers
     */
    private void initializeConnectionPools() {
        rmConnectionPools.put("flight", new ArrayBlockingQueue<>(10));
        rmConnectionPools.put("car", new ArrayBlockingQueue<>(10));
        rmConnectionPools.put("room", new ArrayBlockingQueue<>(10));
    }

    /**
     * Start the middleware server
     */
    public void startServer() {
        try {
            clientServerSocket = new ServerSocket(clientPort);
            running = true;
            System.out.println("TCP Middleware started on port " + clientPort);
            System.out.println("Connecting to ResourceManagers:");
            System.out.println("  Flight RM: " + flightRMHost + ":" + FLIGHT_RM_PORT);
            System.out.println("  Car RM: " + carRMHost + ":" + CAR_RM_PORT);
            System.out.println("  Room RM: " + roomRMHost + ":" + ROOM_RM_PORT);

            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = clientServerSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                    // Handle each client in separate thread (non-blocking)
                    clientThreadPool.submit(new ClientHandler(clientSocket));

                } catch (SocketException e) {
                    if (running) {
                        System.err.println("Socket error accepting connections: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start middleware on port " + clientPort + ": " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Get connection to appropriate ResourceManager
     */
    private Socket getRMConnection(String rmType) throws IOException {
        BlockingQueue<Socket> pool = rmConnectionPools.get(rmType);
        Socket connection = pool.poll(); // Non-blocking poll

        if (connection == null || connection.isClosed()) {
            int port = getPortForRM(rmType);
            String host = getHostForRM(rmType);
            connection = new Socket(host, port);
            System.out.println("Created new connection to " + rmType + " RM");
        }

        return connection;
    }

    /**
     * Return connection to pool
     */
    private void returnRMConnection(String rmType, Socket connection) {
        if (connection != null && !connection.isClosed()) {
            rmConnectionPools.get(rmType).offer(connection);
        }
    }

    /**
     * Get port number for ResourceManager type
     */
    private int getPortForRM(String rmType) {
        switch (rmType) {
            case "flight": return FLIGHT_RM_PORT;
            case "car": return CAR_RM_PORT;
            case "room": return ROOM_RM_PORT;
            default: throw new IllegalArgumentException("Unknown RM type: " + rmType);
        }
    }

    private String getHostForRM(String rmType) {
        switch (rmType) {
            case "flight": return flightRMHost;
            case "car": return carRMHost;
            case "room": return roomRMHost;
            default: throw new IllegalArgumentException("Unknown RM type: " + rmType);
        }
    }

    /**
     * Determine which ResourceManager to route request to
     */
    private String determineRMType(Command command) {
        switch (command) {
            case ADD_FLIGHT:
            case DELETE_FLIGHT:
            case QUERY_FLIGHT:
            case QUERY_FLIGHT_PRICE:
            case RESERVE_FLIGHT:
                return "flight";

            case ADD_CARS:
            case DELETE_CARS:
            case QUERY_CARS:
            case QUERY_CARS_PRICE:
            case RESERVE_CAR:
                return "car";

            case ADD_ROOMS:
            case DELETE_ROOMS:
            case QUERY_ROOMS:
            case QUERY_ROOMS_PRICE:
            case RESERVE_ROOM:
                return "room";

            case NEW_CUSTOMER:
            case NEW_CUSTOMER_ID:
            case DELETE_CUSTOMER:
            case QUERY_CUSTOMER:
            case BUNDLE:
                return "middleware"; // Handle at middleware level

            default:
                return "flight"; // Default to flight RM
        }
    }

    /**
     * Inner class to handle client connections
     */
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientId = clientSocket.getPort(); // Use port as simple client ID
            clientSockets.put(clientId, clientSocket);
        }

        @Override
        public void run() {
            try {
                while (!clientSocket.isClosed() && running) {
                    try {
                        // Receive request from client (this can block per client)
                        TCPMessage request = TCPCommunicator.receiveMessage(clientSocket);
                        System.out.println("Received from client: " + request);

                        // Process request asynchronously (non-blocking middleware)
                        processRequestAsync(request, clientSocket);

                    } catch (EOFException | SocketException e) {
                        System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid message format: " + e.getMessage());
                        break;
                    } catch (IOException e) {
                        System.err.println("Client communication error: " + e.getMessage());
                        break;
                    }
                }
            } finally {
                clientSockets.remove(clientId);
                TCPCommunicator.closeSocket(clientSocket);
            }
        }
    }

    /**
     * Process request asynchronously - this is where non-blocking happens
     */
    private void processRequestAsync(TCPMessage request, Socket clientSocket) {
        String rmType = determineRMType(request.getCommand());

        if ("middleware".equals(rmType)) {
            // Handle customer operations at middleware level
            CompletableFuture.supplyAsync(() -> {
                return handleCustomerOperation(request);
            }).thenAccept(response -> {
                sendResponseToClient(response, clientSocket);
            }).exceptionally(throwable -> {
                TCPMessage errorResponse = new TCPMessage(request.getMessageId(),
                                                         "Middleware error: " + throwable.getMessage());
                sendResponseToClient(errorResponse, clientSocket);
                return null;
            });
        } else {
            // Forward to appropriate ResourceManager (non-blocking)
            CompletableFuture.supplyAsync(() -> {
                return forwardToResourceManager(request, rmType);
            }, rmThreadPool).thenAccept(response -> {
                sendResponseToClient(response, clientSocket);
            }).exceptionally(throwable -> {
                TCPMessage errorResponse = new TCPMessage(request.getMessageId(),
                                                         "RM communication error: " + throwable.getMessage());
                sendResponseToClient(errorResponse, clientSocket);
                return null;
            });
        }
    }

    /**
     * Handle customer operations at middleware level
     */
    private TCPMessage handleCustomerOperation(TCPMessage request) {
        try {
            Object result;
            Object[] args = request.getArguments();

            switch (request.getCommand()) {
                case NEW_CUSTOMER:
                    if (args.length == 0) {
                        synchronized (this) {
                            int customerId = nextCustomerId++;
                            customerData.put(customerId, new ConcurrentHashMap<>());
                            result = customerId;
                        }
                    } else {
                        int customerId = (Integer) args[0];
                        if (!customerData.containsKey(customerId)) {
                            customerData.put(customerId, new ConcurrentHashMap<>());
                            result = true;
                        } else {
                            result = false; // Customer already exists
                        }
                    }
                    break;

                case DELETE_CUSTOMER:
                    int customerId = (Integer) args[0];
                    Map<String, Object> customerInfo = customerData.remove(customerId);
                    result = customerInfo != null;
                    break;

                case QUERY_CUSTOMER:
                    customerId = (Integer) args[0];
                    customerInfo = customerData.get(customerId);
                    if (customerInfo != null) {
                        result = "Customer " + customerId + " data: " + customerInfo.toString();
                    } else {
                        result = "";
                    }
                    break;

                case BUNDLE:
                    result = handleBundle(request);
                    break;

                default:
                    throw new UnsupportedOperationException("Unsupported customer operation: " + request.getCommand());
            }

            return new TCPMessage(request.getMessageId(), result);

        } catch (Exception e) {
            return new TCPMessage(request.getMessageId(), "Customer operation error: " + e.getMessage());
        }
    }

    /**
     * Handle bundle operation - coordinate across multiple RMs
     */
    private boolean handleBundle(TCPMessage request) {
        // For now, return false as bundle is not fully implemented
        // In full implementation, this would coordinate reservations across multiple RMs
        return false;
    }

    /**
     * Forward request to ResourceManager
     */
    private TCPMessage forwardToResourceManager(TCPMessage request, String rmType) {
        Socket rmSocket = null;
        try {
            rmSocket = getRMConnection(rmType);

            // Send request to RM
            TCPCommunicator.sendMessage(rmSocket, request);

            // Receive response from RM
            TCPMessage response = TCPCommunicator.receiveMessage(rmSocket);

            // Return connection to pool
            returnRMConnection(rmType, rmSocket);

            return response;

        } catch (Exception e) {
            System.err.println("Error communicating with " + rmType + " RM: " + e.getMessage());
            if (rmSocket != null) {
                TCPCommunicator.closeSocket(rmSocket);
            }
            return new TCPMessage(request.getMessageId(), "RM communication failed: " + e.getMessage());
        }
    }

    /**
     * Send response back to client
     */
    private void sendResponseToClient(TCPMessage response, Socket clientSocket) {
        try {
            TCPCommunicator.sendMessage(clientSocket, response);
            System.out.println("Sent response: " + response);
        } catch (IOException e) {
            System.err.println("Failed to send response to client: " + e.getMessage());
        }
    }

    /**
     * Shutdown middleware
     */
    public void shutdown() {
        running = false;
        try {
            if (clientServerSocket != null && !clientServerSocket.isClosed()) {
                clientServerSocket.close();
            }
            clientThreadPool.shutdown();
            rmThreadPool.shutdown();

            // Close all RM connections
            for (BlockingQueue<Socket> pool : rmConnectionPools.values()) {
                Socket conn;
                while ((conn = pool.poll()) != null) {
                    TCPCommunicator.closeSocket(conn);
                }
            }

            System.out.println("Middleware shutdown complete");
        } catch (IOException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        int port = DEFAULT_CLIENT_PORT;
        String flightHost = "localhost";
        String carHost = "localhost";
        String roomHost = "localhost";

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }

        if (args.length > 1) {
            flightHost = args[1];
        }

        if (args.length > 2) {
            carHost = args[2];
        }

        if (args.length > 3) {
            roomHost = args[3];
        }

        System.out.println("Starting TCP Middleware with configuration:");
        System.out.println("  Client port: " + port);
        System.out.println("  Flight RM: " + flightHost + ":18081");
        System.out.println("  Car RM: " + carHost + ":18082");
        System.out.println("  Room RM: " + roomHost + ":18083");

        TCPMiddleware middleware = new TCPMiddleware(port, flightHost, carHost, roomHost);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(middleware::shutdown));

        // Start middleware
        middleware.startServer();
    }
}