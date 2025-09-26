package Client;

import Server.Interface.*;
import Server.TCP.TCPMessage;
import Server.TCP.TCPMessage.Command;
import Server.TCP.TCPCommunicator;

import java.io.*;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.rmi.RemoteException;

public class TCPClient extends Client {
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 17000; // Middleware port

    private Socket socket;
    private AtomicInteger messageIdCounter = new AtomicInteger(1);

    public static void main(String args[]) {
        if (args.length > 0) {
            s_serverHost = args[0];
        }
        if (args.length > 1) {
            try {
                s_serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }
        if (args.length > 2) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java Client.TCPClient [server_hostname [server_port]]");
            System.exit(1);
        }

        try {
            TCPClient client = new TCPClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public TCPClient() {
        super();
        // Create a TCP-based ResourceManager proxy
        m_resourceManager = new TCPResourceManagerProxy();
    }

    @Override
    public void connectServer() {
        connectServer(s_serverHost, s_serverPort);
    }

    public void connectServer(String server, int port) {
        try {
            boolean first = true;
            while (true) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    socket = new Socket(server, port);
                    System.out.println("Connected to Middleware server [" + server + ":" + port + "]");
                    break;
                } catch (IOException e) {
                    if (first) {
                        System.out.println("Waiting for Middleware server [" + server + ":" + port + "]");
                        first = false;
                    }
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TCP-based ResourceManager proxy
     * Converts method calls to TCP messages
     */
    private class TCPResourceManagerProxy implements IResourceManager {

        private TCPMessage sendRequest(Command command, Object... args) throws RemoteException {
            try {
                int messageId = messageIdCounter.getAndIncrement();
                TCPMessage request = new TCPMessage(messageId, command, args);

                // Send request
                TCPCommunicator.sendMessage(socket, request);

                // Receive response
                TCPMessage response = TCPCommunicator.receiveMessage(socket);

                // Check for errors
                if (response.getMessageType() == TCPMessage.MessageType.ERROR) {
                    throw new RemoteException("Server error: " + response.getErrorMessage());
                }

                return response;

            } catch (IOException | ClassNotFoundException e) {
                throw new RemoteException("Communication error: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean addFlight(int flightNum, int flightSeats, int flightPrice) throws RemoteException {
            TCPMessage response = sendRequest(Command.ADD_FLIGHT, flightNum, flightSeats, flightPrice);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean addCars(String location, int numCars, int price) throws RemoteException {
            TCPMessage response = sendRequest(Command.ADD_CARS, location, numCars, price);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean addRooms(String location, int numRooms, int price) throws RemoteException {
            TCPMessage response = sendRequest(Command.ADD_ROOMS, location, numRooms, price);
            return (Boolean) response.getResult();
        }

        @Override
        public int newCustomer() throws RemoteException {
            TCPMessage response = sendRequest(Command.NEW_CUSTOMER);
            return (Integer) response.getResult();
        }

        @Override
        public boolean newCustomer(int cid) throws RemoteException {
            TCPMessage response = sendRequest(Command.NEW_CUSTOMER_ID, cid);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean deleteFlight(int flightNum) throws RemoteException {
            TCPMessage response = sendRequest(Command.DELETE_FLIGHT, flightNum);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean deleteCars(String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.DELETE_CARS, location);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean deleteRooms(String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.DELETE_ROOMS, location);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean deleteCustomer(int customerID) throws RemoteException {
            TCPMessage response = sendRequest(Command.DELETE_CUSTOMER, customerID);
            return (Boolean) response.getResult();
        }

        @Override
        public int queryFlight(int flightNumber) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_FLIGHT, flightNumber);
            return (Integer) response.getResult();
        }

        @Override
        public int queryCars(String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_CARS, location);
            return (Integer) response.getResult();
        }

        @Override
        public int queryRooms(String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_ROOMS, location);
            return (Integer) response.getResult();
        }

        @Override
        public String queryCustomerInfo(int customerID) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_CUSTOMER, customerID);
            return (String) response.getResult();
        }

        @Override
        public int queryFlightPrice(int flightNumber) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_FLIGHT_PRICE, flightNumber);
            return (Integer) response.getResult();
        }

        @Override
        public int queryCarsPrice(String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_CARS_PRICE, location);
            return (Integer) response.getResult();
        }

        @Override
        public int queryRoomsPrice(String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.QUERY_ROOMS_PRICE, location);
            return (Integer) response.getResult();
        }

        @Override
        public boolean reserveFlight(int customerID, int flightNumber) throws RemoteException {
            TCPMessage response = sendRequest(Command.RESERVE_FLIGHT, customerID, flightNumber);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean reserveCar(int customerID, String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.RESERVE_CAR, customerID, location);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean reserveRoom(int customerID, String location) throws RemoteException {
            TCPMessage response = sendRequest(Command.RESERVE_ROOM, customerID, location);
            return (Boolean) response.getResult();
        }

        @Override
        public boolean bundle(int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
            TCPMessage response = sendRequest(Command.BUNDLE, customerID, flightNumbers, location, car, room);
            return (Boolean) response.getResult();
        }

        @Override
        public String getName() throws RemoteException {
            TCPMessage response = sendRequest(Command.GET_NAME);
            return (String) response.getResult();
        }
    }
}