package Server.TCP;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import Client.Command;

public class TCPMiddlewareThread extends Thread{
    
    private Socket m_clientsocket;
    private Socket m_flightSocket;
    private Socket m_carSocket;
    private Socket m_roomSocket;

    private ObjectOutputStream m_clientOutputStream;
    private ObjectOutputStream m_flightOutputStream;
    private ObjectOutputStream m_carOutputStream;
    private ObjectOutputStream m_roomOutputStream;

    private ObjectInputStream m_clientInputStream;
    private ObjectInputStream m_flightInputStream;
    private ObjectInputStream m_carInputStream;
    private ObjectInputStream m_roomInputStream;

    private static int s_serverPort = 3026;

    private static Set<Command> s_flightCommands = Set.of(Command.AddFlight, Command.DeleteFlight, Command.QueryFlight, Command.QueryFlightPrice, Command.ReserveFlight);

    private static Set<Command> s_carCommands = Set.of(Command.AddCars, Command.DeleteCars, Command.QueryCars, Command.QueryCarsPrice, Command.ReserveCar);

    private static Set<Command> s_roomCommands = Set.of( Command.AddRooms, Command.DeleteRooms, Command.QueryRooms, Command.QueryRoomsPrice, Command.ReserveRoom);
    
    private static Set<Command> s_aggregateCommands = Set.of(Command.AddCustomer, Command.AddCustomerID, Command.DeleteCustomer, Command.QueryCustomer, Command.Bundle);

    public TCPMiddlewareThread( Socket p_clientsocket, InetAddress p_flight_ipadd, InetAddress p_car_ipadd, InetAddress p_room_ipadd) throws IOException{
        m_clientsocket = p_clientsocket;
        m_flightSocket = new Socket(p_flight_ipadd, s_serverPort);
        m_carSocket = new Socket(p_car_ipadd, s_serverPort);
        m_roomSocket = new Socket(p_room_ipadd, s_serverPort);

        m_clientOutputStream = new ObjectOutputStream(m_clientsocket.getOutputStream());
        m_clientOutputStream.flush();
        m_flightOutputStream = new ObjectOutputStream(m_flightSocket.getOutputStream());
        m_flightOutputStream.flush();
        m_carOutputStream = new ObjectOutputStream(m_carSocket.getOutputStream());
        m_carOutputStream.flush();
        m_roomOutputStream = new ObjectOutputStream(m_roomSocket.getOutputStream());
        m_roomOutputStream.flush();

        m_clientInputStream = new ObjectInputStream(m_clientsocket.getInputStream());
        m_flightInputStream = new ObjectInputStream(m_flightSocket.getInputStream());
        m_carInputStream = new ObjectInputStream(m_carSocket.getInputStream()); 
        m_roomInputStream = new ObjectInputStream(m_roomSocket.getInputStream());
    }

    public void run() {
        try {
            while (true) {
                TCPCommandMessage message = (TCPCommandMessage) m_clientInputStream.readObject();
                
                Command msg_command = message.getCommand();

                System.out.println("Received command: " + msg_command.name());

                if (s_flightCommands.contains(msg_command)){
                    writeToStream(m_flightOutputStream, message);

                    TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_flightInputStream.readObject();
                    writeToStream(m_clientOutputStream, response);
                }
                else if (s_carCommands.contains(msg_command)){
                    writeToStream(m_carOutputStream, message);

                    TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_carInputStream.readObject();

                    writeToStream(m_clientOutputStream, response);
                }
                else if (s_roomCommands.contains(msg_command)){
                    writeToStream(m_roomOutputStream, message);

                    TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_roomInputStream.readObject();

                    writeToStream(m_clientOutputStream, response);
                }

                else if (s_aggregateCommands.contains(msg_command)){

                    if (msg_command == Command.AddCustomer){
                        int cid = Integer.parseInt(String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) + String.valueOf(Math.round(Math.random() * 100 + 1)));

                        Vector<String> args = new Vector<String>();
                        args.add("AddCustomerID");
                        args.add(String.valueOf(cid));

                        TCPCommandMessage newCustomerMessage = new TCPCommandMessage(Command.AddCustomerID, args);
                        writeToStream(m_flightOutputStream, newCustomerMessage);

                        TCPCommandMessageResponse flight_response = (TCPCommandMessageResponse) m_flightInputStream.readObject();
                        String flight_success = flight_response.getReturn();

                        writeToStream(m_carOutputStream, newCustomerMessage);

                        TCPCommandMessageResponse car_response = (TCPCommandMessageResponse) m_carInputStream.readObject();
                        String car_success = car_response.getReturn();

                        writeToStream(m_roomOutputStream, newCustomerMessage);

                        TCPCommandMessageResponse room_response = (TCPCommandMessageResponse) m_roomInputStream.readObject();
                        String room_success = room_response.getReturn();
                        
                        if (flight_success.equals("true") && car_success.equals("true") && room_success.equals("true")){
                            TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, String.valueOf(cid));
                            writeToStream(m_clientOutputStream, overall_response);
                        }
                        else{
                            TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, "-1");
                            writeToStream(m_clientOutputStream, overall_response);
                        }
                    }
                    else if (msg_command == Command.AddCustomerID || msg_command == Command.DeleteCustomer){
                        writeToStream(m_flightOutputStream, message);

                        TCPCommandMessageResponse flight_response = (TCPCommandMessageResponse) m_flightInputStream.readObject();
                        boolean flight_success = Boolean.parseBoolean(flight_response.getReturn());

                        writeToStream(m_carOutputStream, message);

                        TCPCommandMessageResponse car_response = (TCPCommandMessageResponse) m_carInputStream.readObject();
                        boolean car_success = Boolean.parseBoolean(car_response.getReturn());

                        writeToStream(m_roomOutputStream, message);

                        TCPCommandMessageResponse room_response = (TCPCommandMessageResponse) m_roomInputStream.readObject();
                        boolean room_success = Boolean.parseBoolean(room_response.getReturn());
                        
                        boolean overall_success = flight_success && car_success && room_success;

                        TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, String.valueOf(overall_success));
                        
                        writeToStream(m_clientOutputStream, overall_response);
                    }
                    else if (msg_command == Command.QueryCustomer){
                        writeToStream(m_flightOutputStream, message);

                        TCPCommandMessageResponse flight_response = (TCPCommandMessageResponse) m_flightInputStream.readObject();
                        String flight_bill = "Flight " + flight_response.getReturn();

                        writeToStream(m_carOutputStream, message);

                        TCPCommandMessageResponse car_response = (TCPCommandMessageResponse) m_carInputStream.readObject();
                        String car_bill = "Car " + car_response.getReturn();

                        writeToStream(m_roomOutputStream, message);

                        TCPCommandMessageResponse room_response = (TCPCommandMessageResponse) m_roomInputStream.readObject();
                        String room_bill = "Room " + room_response.getReturn();
                        
                        String overall_bill = flight_bill + car_bill + room_bill;

                        TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, overall_bill);
                        
                        writeToStream(m_clientOutputStream, overall_response);
                    }
                    else if (msg_command == Command.Bundle){
                        Vector<String> args = message.getCommandArgs();
                        int customerID = Integer.parseInt(args.elementAt(1));
                        Vector<String> flightNumbers = new Vector<String>();
                        for (int i = 2; i < args.size() - 3; i++){
                            flightNumbers.add(args.elementAt(i));
                        }
                        String location = args.elementAt(args.size() - 3);
                        boolean car = Boolean.valueOf(args.elementAt(args.size() - 2));
                        boolean room = Boolean.valueOf(args.elementAt(args.size() - 1));

                        if (!canBundle(customerID, flightNumbers, location, car, room)){
                            boolean overall_success = false;

                            TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, String.valueOf(overall_success));
                            
                            writeToStream(m_clientOutputStream, overall_response);
                        }
                        else{

                            for (String flightNum : flightNumbers) {
                                Vector<String> flight_args_vector = new Vector<String>();
                                flight_args_vector.add("ReserveFlight");
                                flight_args_vector.add(String.valueOf(customerID));
                                flight_args_vector.add(flightNum);
                                TCPCommandMessage flight_message = new TCPCommandMessage(Command.ReserveFlight, flight_args_vector);

                                writeToStream(m_flightOutputStream, flight_message);
                                
                                //Have to clear the response.
                                m_flightInputStream.readObject();
                            }

                            if (car) {
                                Vector<String> car_args_vector = new Vector<String>();
                                car_args_vector.add("ReserveCar");
                                car_args_vector.add(String.valueOf(customerID));
                                car_args_vector.add(location);
                                TCPCommandMessage car_message = new TCPCommandMessage(Command.ReserveCar, car_args_vector);

                                writeToStream(m_carOutputStream, car_message);

                                //Have to clear the response.
                                m_carInputStream.readObject();
                            }

                            if (room) {
                                Vector<String> room_args_vector = new Vector<String>();
                                room_args_vector.add("ReserveRoom");
                                room_args_vector.add(String.valueOf(customerID));
                                room_args_vector.add(location);
                                TCPCommandMessage room_message = new TCPCommandMessage(Command.ReserveRoom, room_args_vector);

                                writeToStream(m_roomOutputStream, room_message);
                                
                                //Have to clear the response.
                                m_roomInputStream.readObject();
                            }

                            boolean overall_success = true;

                            TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, String.valueOf(overall_success));
                            
                            writeToStream(m_clientOutputStream, overall_response);
                        }
                    } 
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean canBundle(int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws IOException, ClassNotFoundException{

        Vector<String> cust_args = new Vector<>();
        cust_args.add("QueryCustomer");
        cust_args.add(String.valueOf(customerID));

        TCPCommandMessage query_customer = new TCPCommandMessage(Command.QueryCustomer, cust_args);
        writeToStream(m_flightOutputStream, query_customer);

        TCPCommandMessageResponse cust_response = (TCPCommandMessageResponse) m_flightInputStream.readObject();

        String customerInfo = cust_response.getReturn();
        if (customerInfo.equals("")) {
             System.out.println("Bundle failed, customer with ID" + customerID + "doesn't exist.");
            return false;
        }

        for (String flightNum : flightNumbers) {
            Vector<String> flight_args = new Vector<>();
            flight_args.add("QueryFlight");
            flight_args.add(flightNum);

            TCPCommandMessage query_flight = new TCPCommandMessage(Command.QueryFlight, flight_args);
            writeToStream(m_flightOutputStream, query_flight);

            TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_flightInputStream.readObject();

            int numSeats = Integer.parseInt(response.getReturn());
            if (numSeats <= 0) {
                System.out.println("Bundle failed, no seats for flight with ID:" + flightNum);
                return false;
            }
        }

        if (car) {
            Vector<String> car_args = new Vector<>();
            car_args.add("QueryCars");
            car_args.add(location);

            TCPCommandMessage query_car = new TCPCommandMessage(Command.QueryCars, car_args);
            writeToStream(m_carOutputStream, query_car);

            TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_carInputStream.readObject();

            int numCars = Integer.parseInt(response.getReturn());
            if (numCars <= 0) {
                System.out.println("Bundle failed, no available cars at location:" + location);
                return false;
            }
        }

        if (room) {
            Vector<String> room_args = new Vector<>();
            room_args.add("QueryRooms");
            room_args.add(location);

            TCPCommandMessage query_room = new TCPCommandMessage(Command.QueryRooms, room_args);
            writeToStream(m_roomOutputStream, query_room);

            TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_roomInputStream.readObject();

            int numRooms = Integer.parseInt(response.getReturn());
            if (numRooms <= 0) {
                System.out.println("Bundle failed, no available rooms at location:" + location);
                return false;
            }
        }

        return true;
    } 

    public void writeToStream(ObjectOutputStream out, Object obj) throws IOException {
        out.writeObject(obj);
        out.flush();
    }
}
