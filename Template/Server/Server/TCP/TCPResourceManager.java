package Server.TCP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Vector;

import Client.Command;
import Client.TCPCommandMessage;
import Client.TCPCommandMessageResponse;
import Server.Common.*;

public class TCPResourceManager extends ResourceManager {
    
    private static int s_serverPort = 3026;

    private ObjectInputStream m_in;
    private ObjectOutputStream m_out;

    public TCPResourceManager(String p_name) {
        super(p_name);
    }

    public static void main(String[] args) throws IOException{

        String serverName = "";
        if (args.length > 0) {
            serverName = args[0];
        }
        
        TCPResourceManager server = new TCPResourceManager(serverName);
        
        ServerSocket serverSocket = new ServerSocket(s_serverPort);

        Socket clientSocket = serverSocket.accept();

        server.m_in = new ObjectInputStream(clientSocket.getInputStream());
        server.m_out = new ObjectOutputStream(clientSocket.getOutputStream());

        while (true) {
            try {
                TCPCommandMessage message = (TCPCommandMessage) server.m_in.readObject();
                TCPCommandMessageResponse response = server.processRequest(message);
                
                server.m_out.writeObject(response);
                server.m_out.flush();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private TCPCommandMessageResponse processRequest(TCPCommandMessage message) throws RemoteException {
        Command command = message.getCommand();
        Vector<String> args = message.getCommandArgs();

        TCPCommandMessageResponse resp = null;
        switch(command){

            case Command.AddFlight: {
                int flightNum = Integer.parseInt(args.get(0));
                int flightSeats = Integer.parseInt(args.get(1));
                int flightPrice = Integer.parseInt(args.get(2));
                boolean result = addFlight(flightNum, flightSeats, flightPrice);

                resp = new TCPCommandMessageResponse(Command.AddFlight, String.valueOf(result));
            }

            case Command.AddCars: {
                String location = args.get(0);
                int count = Integer.parseInt(args.get(1));
                int price = Integer.parseInt(args.get(2));
                boolean result = addCars(location, count, price);

                resp = new TCPCommandMessageResponse(Command.AddCars, String.valueOf(result));
            }

            case Command.AddRooms: {
                String location = args.get(0);
                int count = Integer.parseInt(args.get(1));
                int price = Integer.parseInt(args.get(2));

                boolean result = addRooms(location, count, price);

                resp = new TCPCommandMessageResponse(Command.AddRooms, String.valueOf(result));
            }

            case Command.AddCustomer:{
                int cid = newCustomer();

                resp = new TCPCommandMessageResponse(Command.AddCustomer, String.valueOf(cid));
            }

            case Command.AddCustomerID:{
                int cid = Integer.parseInt(args.get(0));
                boolean result = newCustomer(cid);

                resp = new TCPCommandMessageResponse(Command.AddCustomerID, String.valueOf(result));
            }

            case Command.DeleteFlight:{
                int flightNum = Integer.parseInt(args.get(0));
                boolean result = deleteFlight(flightNum);

                resp = new TCPCommandMessageResponse(Command.DeleteFlight, String.valueOf(result));
            }

            case Command.DeleteCars:{
                String location = args.get(0);
                boolean result = deleteCars(location);

                resp = new TCPCommandMessageResponse(Command.DeleteCars, String.valueOf(result));
            }

            case Command.DeleteRooms:{
                String location = args.get(0);  
                boolean result = deleteRooms(location);

                resp = new TCPCommandMessageResponse(Command.DeleteRooms, String.valueOf(result));
            } 

            case Command.DeleteCustomer:{
                int cid = Integer.parseInt(args.get(0));
                boolean result = deleteCustomer(cid);

                resp = new TCPCommandMessageResponse(Command.DeleteCustomer, String.valueOf(result));
            }

            case Command.QueryFlight:{
                int flightNum = Integer.parseInt(args.get(0));
                int seats = queryFlight(flightNum);

                resp = new TCPCommandMessageResponse(Command.QueryFlight, String.valueOf(seats));
            }

            case Command.QueryCars:{
                String location = args.get(0);
                int numCars = queryCars(location); 

                resp = new TCPCommandMessageResponse(Command.QueryCars, String.valueOf(numCars));
            }

            case Command.QueryRooms:{
                String location = args.get(0);
                int numRooms = queryRooms(location); 

                resp = new TCPCommandMessageResponse(Command.QueryRooms, String.valueOf(numRooms));
            }

            case Command.QueryCustomer:{
                int cid = Integer.parseInt(args.get(0));
                String bill = queryCustomerInfo(cid);

                resp = new TCPCommandMessageResponse(Command.QueryCustomer, bill);
            }

            case Command.QueryFlightPrice:{
                int flightNum = Integer.parseInt(args.get(0));
                int price = queryFlightPrice(flightNum);

                resp = new TCPCommandMessageResponse(Command.QueryFlightPrice, String.valueOf(price));
            }   

            case Command.QueryCarsPrice:{
                String location = args.get(0);
                int price = queryCarsPrice(location); 

                resp = new TCPCommandMessageResponse(Command.QueryCarsPrice, String.valueOf(price));
            }   

            case Command.QueryRoomsPrice:{
                String location = args.get(0);
                int price = queryRoomsPrice(location);  

                resp = new TCPCommandMessageResponse(Command.QueryRoomsPrice, String.valueOf(price));
            }

            case Command.ReserveFlight:{
                int cid = Integer.parseInt(args.get(0));
                int flightNum = Integer.parseInt(args.get(1));
                boolean result = reserveFlight(cid, flightNum);

                resp = new TCPCommandMessageResponse(Command.ReserveFlight, String.valueOf(result));
            }

            case Command.ReserveCar:{
                int cid = Integer.parseInt(args.get(0));
                String location = args.get(1);
                boolean result = reserveCar(cid, location);

                resp = new TCPCommandMessageResponse(Command.ReserveCar, String.valueOf(result));
            }

            case Command.ReserveRoom:{
                int cid = Integer.parseInt(args.get(0));
                String location = args.get(1);
                boolean result = reserveRoom(cid, location);  
                
                resp = new TCPCommandMessageResponse(Command.ReserveRoom, String.valueOf(result));
            }

            default: {
                resp = null;
            }
        }

        return resp;
    }
}

