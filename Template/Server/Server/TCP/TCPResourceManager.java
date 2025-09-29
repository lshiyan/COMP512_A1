package Server.TCP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Vector;

import Client.Command;
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
        
        try(ServerSocket serverSocket = new ServerSocket(s_serverPort)){

            while (true) {
                Socket middlewareSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + middlewareSocket);

                new Thread(() -> {
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(middlewareSocket.getOutputStream());
                        out.flush();
                        ObjectInputStream in = new ObjectInputStream(middlewareSocket.getInputStream());

                        while (true) {
                            TCPCommandMessage message = (TCPCommandMessage) in.readObject();
                            TCPCommandMessageResponse response = server.processRequest(message);

                            out.writeObject(response);
                            out.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private TCPCommandMessageResponse processRequest(TCPCommandMessage message) throws RemoteException {
        Command command = message.getCommand();
        Vector<String> args = message.getCommandArgs();

        TCPCommandMessageResponse resp = null;
        switch(command){

            case Command.AddFlight: {
                int flightNum = Integer.parseInt(args.get(1));
                int flightSeats = Integer.parseInt(args.get(2));
                int flightPrice = Integer.parseInt(args.get(3));
                boolean result = addFlight(flightNum, flightSeats, flightPrice);

                resp = new TCPCommandMessageResponse(Command.AddFlight, String.valueOf(result));
                break;
            }

            case Command.AddCars: {
                String location = args.get(1);
                int count = Integer.parseInt(args.get(2));
                int price = Integer.parseInt(args.get(3));
                boolean result = addCars(location, count, price);

                resp = new TCPCommandMessageResponse(Command.AddCars, String.valueOf(result));
                break;
            }

            case Command.AddRooms: {
                String location = args.get(1);
                int count = Integer.parseInt(args.get(2));
                int price = Integer.parseInt(args.get(3));

                boolean result = addRooms(location, count, price);

                resp = new TCPCommandMessageResponse(Command.AddRooms, String.valueOf(result));
                break;
            }

            case Command.AddCustomer:{
                int cid = newCustomer();

                resp = new TCPCommandMessageResponse(Command.AddCustomer, String.valueOf(cid));
                break;
            }

            case Command.AddCustomerID:{
                int cid = Integer.parseInt(args.get(1));
                boolean result = newCustomer(cid);

                resp = new TCPCommandMessageResponse(Command.AddCustomerID, String.valueOf(result));
                break;
            }

            case Command.DeleteFlight:{
                int flightNum = Integer.parseInt(args.get(1));
                boolean result = deleteFlight(flightNum);

                resp = new TCPCommandMessageResponse(Command.DeleteFlight, String.valueOf(result));
                break;
            }

            case Command.DeleteCars:{
                String location = args.get(1);
                boolean result = deleteCars(location);

                resp = new TCPCommandMessageResponse(Command.DeleteCars, String.valueOf(result));
                break;
            }

            case Command.DeleteRooms:{
                String location = args.get(1);  
                boolean result = deleteRooms(location);

                resp = new TCPCommandMessageResponse(Command.DeleteRooms, String.valueOf(result));
                break;
            } 

            case Command.DeleteCustomer:{
                int cid = Integer.parseInt(args.get(1));
                boolean result = deleteCustomer(cid);

                resp = new TCPCommandMessageResponse(Command.DeleteCustomer, String.valueOf(result));
                break;
            }

            case Command.QueryFlight:{
                int flightNum = Integer.parseInt(args.get(1));
                int seats = queryFlight(flightNum);

                resp = new TCPCommandMessageResponse(Command.QueryFlight, String.valueOf(seats));
                break;
            }

            case Command.QueryCars:{
                String location = args.get(1);
                int numCars = queryCars(location); 

                resp = new TCPCommandMessageResponse(Command.QueryCars, String.valueOf(numCars));
                break;
            }

            case Command.QueryRooms:{
                String location = args.get(1);
                int numRooms = queryRooms(location); 

                resp = new TCPCommandMessageResponse(Command.QueryRooms, String.valueOf(numRooms));
                break;
            }

            case Command.QueryCustomer:{
                int cid = Integer.parseInt(args.get(1));
                String bill = queryCustomerInfo(cid);

                resp = new TCPCommandMessageResponse(Command.QueryCustomer, bill);
                break;
            }

            case Command.QueryFlightPrice:{
                int flightNum = Integer.parseInt(args.get(1));
                int price = queryFlightPrice(flightNum);

                resp = new TCPCommandMessageResponse(Command.QueryFlightPrice, String.valueOf(price));
                break;
            }   

            case Command.QueryCarsPrice:{
                String location = args.get(1);
                int price = queryCarsPrice(location); 

                resp = new TCPCommandMessageResponse(Command.QueryCarsPrice, String.valueOf(price));
                break;
            }   

            case Command.QueryRoomsPrice:{
                String location = args.get(1);
                int price = queryRoomsPrice(location);  

                resp = new TCPCommandMessageResponse(Command.QueryRoomsPrice, String.valueOf(price));
                break;
            }

            case Command.ReserveFlight:{
                int cid = Integer.parseInt(args.get(1));
                int flightNum = Integer.parseInt(args.get(2));
                boolean result = reserveFlight(cid, flightNum);

                resp = new TCPCommandMessageResponse(Command.ReserveFlight, String.valueOf(result));
                break;
            }

            case Command.ReserveCar:{
                int cid = Integer.parseInt(args.get(1));
                String location = args.get(2);
                boolean result = reserveCar(cid, location);

                resp = new TCPCommandMessageResponse(Command.ReserveCar, String.valueOf(result));
                break;
            }

            case Command.ReserveRoom:{
                int cid = Integer.parseInt(args.get(1));
                String location = args.get(2);
                boolean result = reserveRoom(cid, location);  
                
                resp = new TCPCommandMessageResponse(Command.ReserveRoom, String.valueOf(result));
                break;
            }

            default: {
                resp = null;
                break;
            }
        }

        System.out.println("Response message return:" + resp.getReturn());
        return resp;
    }
}

