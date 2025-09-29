package Server.TCP;

import Server.TCP.TCPMessage.Command;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TCPMiddleware {
    private static final int DEFAULT_CLIENT_PORT = 17000;
    private static final int FLIGHT_RM_PORT = 18081;
    private static final int CAR_RM_PORT = 18082;
    private static final int ROOM_RM_PORT = 18083;

    private String flightRMHost;
    private String carRMHost;
    private String roomRMHost;

    private ServerSocket clientServerSocket;
    private ExecutorService clientThreadPool;
    private boolean running = false;
    private int clientPort;

    public TCPMiddleware(int clientPort) {
        this(clientPort, "localhost", "localhost", "localhost");
    }

    public TCPMiddleware(int clientPort, String flightHost, String carHost, String roomHost) {
        this.clientPort = clientPort;
        this.flightRMHost = flightHost;
        this.carRMHost = carHost;
        this.roomRMHost = roomHost;
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    public void startServer() {
        try {
            clientServerSocket = new ServerSocket(clientPort);
            running = true;
            System.out.println("TCP Middleware started on port " + clientPort);
            System.out.println("Connecting to ResourceManagers:");
            System.out.println("  Flight RM: " + flightRMHost + ":" + FLIGHT_RM_PORT);
            System.out.println("  Car RM: " + carRMHost + ":" + CAR_RM_PORT);
            System.out.println("  Room RM: " + roomRMHost + ":" + ROOM_RM_PORT);

            while (running) {
                try {
                    Socket clientSocket = clientServerSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
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

    private Socket getRMConnection(String rmType) throws IOException {
        int port = getPortForRM(rmType);
        String host = getHostForRM(rmType);
        return new Socket(host, port);
    }

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
                return "customer"; 

            default:
                return "flight"; 
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Socket flightSocket;
        private Socket carSocket;
        private Socket roomSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            // Create persistent connections to all RMs
            try {
                flightSocket = getRMConnection("flight");
                carSocket = getRMConnection("car");
                roomSocket = getRMConnection("room");
            } catch (IOException e) {
                System.err.println("Failed to establish RM connections: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                while (!clientSocket.isClosed() && running) {
                    try {
                        
                        TCPMessage request = TCPCommunicator.receiveMessage(clientSocket);
                        System.out.println("Received from client: " + request);
                        processRequest(request);

                    } catch (EOFException | SocketException e) {
                        System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid message format");
                        break;
                    } catch (IOException e) {
                        System.err.println("Client communication error: " + e.getMessage());
                        break;
                    }
                }
            } finally {
                TCPCommunicator.closeSocket(flightSocket);
                TCPCommunicator.closeSocket(carSocket);
                TCPCommunicator.closeSocket(roomSocket);
                TCPCommunicator.closeSocket(clientSocket);
            }
        }

        private void processRequest(TCPMessage request) {
            String rmType = determineRMType(request.getCommand());

            try {
                TCPMessage response;

                if ("customer".equals(rmType)) {
                    response = handleDistributedCustomerOperationPersistent(request);
                } else {
                    response = forwardToResourceManagerPersistent(request, rmType);
                }
                sendResponseToClient(response, clientSocket);

            } catch (Exception e) {
                System.err.println("Error processing request " + request.getCommand() + ": " + e.getMessage());
                TCPMessage errorResponse = new TCPMessage(request.getMessageId(), "Error processing request: " + e.getMessage());
                sendResponseToClient(errorResponse, clientSocket);
            }
        }

        private TCPMessage forwardToResourceManagerPersistent(TCPMessage request, String rmType) {
            Socket rmSocket;
            switch (rmType) {
                case "flight": rmSocket = flightSocket; break;
                case "car": rmSocket = carSocket; break;
                case "room": rmSocket = roomSocket; break;
                default: throw new IllegalArgumentException("Unknown RM type: " + rmType);
            }

            try {
                TCPCommunicator.sendMessage(rmSocket, request);
                TCPMessage response = TCPCommunicator.receiveMessage(rmSocket);
                return response;

            } catch (Exception e) {
                System.err.println("Error communicating with " + rmType + " RM: " + e.getMessage());
                return new TCPMessage(request.getMessageId(), "RM communication failed: " + e.getMessage());
            }
        }

        private TCPMessage handleDistributedCustomerOperationPersistent(TCPMessage request) {
            try {
                switch (request.getCommand()) {
                    case NEW_CUSTOMER:
                        int cid = Integer.parseInt(String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.MILLISECOND)) +
                                                String.valueOf(Math.round(Math.random() * 100 + 1)));
                        TCPMessage newCustomerRequest = new TCPMessage(request.getMessageId(), TCPMessage.Command.NEW_CUSTOMER_ID, cid);

                        TCPMessage flightResponse = forwardToResourceManagerPersistent(newCustomerRequest, "flight");
                        TCPMessage carResponse = forwardToResourceManagerPersistent(newCustomerRequest, "car");
                        TCPMessage roomResponse = forwardToResourceManagerPersistent(newCustomerRequest, "room");

                        boolean allSuccess = (Boolean) flightResponse.getResult() &&
                                           (Boolean) carResponse.getResult() &&
                                           (Boolean) roomResponse.getResult();
                        return new TCPMessage(request.getMessageId(), allSuccess ? cid : -1);

                    case NEW_CUSTOMER_ID:
                    case DELETE_CUSTOMER:
                        TCPMessage flightResp = forwardToResourceManagerPersistent(request, "flight");
                        TCPMessage carResp = forwardToResourceManagerPersistent(request, "car");
                        TCPMessage roomResp = forwardToResourceManagerPersistent(request, "room");

                        boolean success = (Boolean) flightResp.getResult() &&
                                        (Boolean) carResp.getResult() &&
                                        (Boolean) roomResp.getResult();
                        return new TCPMessage(request.getMessageId(), success);

                    case QUERY_CUSTOMER:
                        TCPMessage flightBill = forwardToResourceManagerPersistent(request, "flight");
                        TCPMessage carBill = forwardToResourceManagerPersistent(request, "car");
                        TCPMessage roomBill = forwardToResourceManagerPersistent(request, "room");

                        String overall_bill = "Flight" + flightBill.getResult() +
                                            "Car" + carBill.getResult() +
                                            "Room" + roomBill.getResult();
                        return new TCPMessage(request.getMessageId(), (Object) overall_bill);

                    case BUNDLE:
                        Object[] args = request.getArguments();
                        int customerID = (Integer) args[0];
                        @SuppressWarnings("unchecked")
                        Vector<String> flightNumbers = (Vector<String>) args[1];
                        String location = (String) args[2];
                        boolean car = (Boolean) args[3];
                        boolean room = (Boolean) args[4];

                        TCPMessage custCheck = new TCPMessage(request.getMessageId(), TCPMessage.Command.QUERY_CUSTOMER, customerID);
                        TCPMessage custResponse = forwardToResourceManagerPersistent(custCheck, "flight");
                        String customerInfo = (String) custResponse.getResult();
                        if (customerInfo == null || customerInfo.trim().isEmpty()) {
                            return new TCPMessage(request.getMessageId(), false);
                        }

                        java.util.Map<String, Integer> flightCounts = new java.util.HashMap<>();
                        for (String flightNum : flightNumbers) {
                            flightCounts.put(flightNum, flightCounts.getOrDefault(flightNum, 0) + 1);
                        }

                        for (java.util.Map.Entry<String, Integer> entry : flightCounts.entrySet()) {
                            String flightNum = entry.getKey();
                            int needed = entry.getValue();

                            TCPMessage flightCheck = new TCPMessage(request.getMessageId(), TCPMessage.Command.QUERY_FLIGHT, Integer.parseInt(flightNum));
                            TCPMessage flightAvailCheck = forwardToResourceManagerPersistent(flightCheck, "flight");
                            int availableSeats = (Integer) flightAvailCheck.getResult();

                            if (availableSeats < needed) {
                                return new TCPMessage(request.getMessageId(), false);
                            }
                        }

                        if (car) {
                            TCPMessage carCheck = new TCPMessage(request.getMessageId(), TCPMessage.Command.QUERY_CARS, location);
                            TCPMessage carAvailCheck = forwardToResourceManagerPersistent(carCheck, "car");
                            int availableCars = (Integer) carAvailCheck.getResult();
                            if (availableCars <= 0) {
                                return new TCPMessage(request.getMessageId(), false);
                            }
                        }

                        if (room) {
                            TCPMessage roomCheck = new TCPMessage(request.getMessageId(), TCPMessage.Command.QUERY_ROOMS, location);
                            TCPMessage roomAvailCheck = forwardToResourceManagerPersistent(roomCheck, "room");
                            int availableRooms = (Integer) roomAvailCheck.getResult();
                            if (availableRooms <= 0) {
                                return new TCPMessage(request.getMessageId(), false);
                            }
                        }

                        // Reserve flights
                        for (String flightNum : flightNumbers) {
                            TCPMessage flightReq = new TCPMessage(request.getMessageId(),
                                TCPMessage.Command.RESERVE_FLIGHT, customerID, Integer.parseInt(flightNum));
                            TCPMessage flightResult = forwardToResourceManagerPersistent(flightReq, "flight");
                            if (!(Boolean) flightResult.getResult()) {
                                return new TCPMessage(request.getMessageId(), false);
                            }
                        }

                        // Reserve car
                        if (car) {
                            TCPMessage carReq = new TCPMessage(request.getMessageId(),
                                TCPMessage.Command.RESERVE_CAR, customerID, location);
                            TCPMessage carResult = forwardToResourceManagerPersistent(carReq, "car");
                            if (!(Boolean) carResult.getResult()) {
                                return new TCPMessage(request.getMessageId(), false);
                            }
                        }

                        // Reserve room if requested
                        if (room) {
                            TCPMessage roomReq = new TCPMessage(request.getMessageId(),
                                TCPMessage.Command.RESERVE_ROOM, customerID, location);
                            TCPMessage roomResult = forwardToResourceManagerPersistent(roomReq, "room");
                            if (!(Boolean) roomResult.getResult()) {
                                return new TCPMessage(request.getMessageId(), false);
                            }
                        }

                        return new TCPMessage(request.getMessageId(), true);

                    default:
                        throw new UnsupportedOperationException("Unsupported customer operation: " + request.getCommand());
                }
            } catch (Exception e) {
                return new TCPMessage(request.getMessageId(), "Customer operation error: " + e.getMessage());
            }
        }

    }

    private void sendResponseToClient(TCPMessage response, Socket clientSocket) {
        try {
            TCPCommunicator.sendMessage(clientSocket, response);
        } catch (IOException e) {
            System.err.println("Failed to send response to client: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (clientServerSocket != null && !clientServerSocket.isClosed()) {
                clientServerSocket.close();
            }
            clientThreadPool.shutdown();
        } catch (IOException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }


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
        Runtime.getRuntime().addShutdownHook(new Thread(middleware::shutdown));
        middleware.startServer();
    }
}