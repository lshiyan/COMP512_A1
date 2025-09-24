package Server.TCP;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import Client.Command;
import Client.TCPCommandMessage;
import Client.TCPCommandMessageResponse;

public class TCPConnectionThread extends Thread{
    
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

    public TCPConnectionThread( Socket p_clientsocket, InetAddress p_flight_ipadd, InetAddress p_car_ipadd, InetAddress p_room_ipadd) throws IOException{
        m_clientsocket = p_clientsocket;
        m_flightSocket = new Socket(p_flight_ipadd, s_serverPort);
        m_carSocket = new Socket(p_car_ipadd, s_serverPort);
        m_roomSocket = new Socket(p_room_ipadd, s_serverPort);

        m_clientOutputStream = new ObjectOutputStream(m_clientsocket.getOutputStream());
        m_flightOutputStream = new ObjectOutputStream(m_flightSocket.getOutputStream());
        m_carOutputStream = new ObjectOutputStream(m_carSocket.getOutputStream());
        m_roomOutputStream = new ObjectOutputStream(m_roomSocket.getOutputStream());

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
                        writeToStream(m_flightOutputStream, message);

                        TCPCommandMessageResponse flight_response = (TCPCommandMessageResponse) m_flightInputStream.readObject();
                        String flight_customerID = flight_response.getReturn();

                        writeToStream(m_carOutputStream, message);

                        TCPCommandMessageResponse car_response = (TCPCommandMessageResponse) m_carInputStream.readObject();
                        String car_customerID = car_response.getReturn();

                        writeToStream(m_roomOutputStream, message);

                        TCPCommandMessageResponse room_response = (TCPCommandMessageResponse) m_roomInputStream.readObject();

                        String room_customerID = room_response.getReturn();
                        
                        if (flight_customerID == car_customerID && car_customerID == room_customerID){
                            TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, flight_customerID);
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
                        String flight_bill = flight_response.getReturn();

                        writeToStream(m_carOutputStream, message);

                        TCPCommandMessageResponse car_response = (TCPCommandMessageResponse) m_carInputStream.readObject();
                        String car_bill = car_response.getReturn();

                        writeToStream(m_roomOutputStream, message);

                        TCPCommandMessageResponse room_response = (TCPCommandMessageResponse) m_roomInputStream.readObject();
                        String room_bill = room_response.getReturn();
                        
                        String overall_bill = flight_bill + car_bill + room_bill;

                        TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, overall_bill);
                        
                        writeToStream(m_clientOutputStream, overall_response);
                    }
                    else if (msg_command == Command.Bundle){
                        Vector<String> args = message.getCommandArgs();
                        int customerID = Integer.parseInt(args.elementAt(0));
                        Vector<String> flightNumbers = new Vector<String>();
                        for (int i = 1; i < args.size() - 5; i++){
                            flightNumbers.add(args.elementAt(2+i));
                        }
                        String location = args.elementAt(args.size() - 3);
                        boolean car = Boolean.valueOf(args.elementAt(args.size() - 2));
                        boolean room = Boolean.valueOf(args.elementAt(args.size() - 1));
                        

                        boolean flights_reserved_success = true;
                        boolean cars_reserved_success = true;
                        boolean rooms_reserved_success = true;

                        for (String flightNum : flightNumbers) {
                            Vector<String> flight_args_vector = new Vector<String>();
                            flight_args_vector.add(flightNum);
                            TCPCommandMessage flight_message = new TCPCommandMessage(Command.ReserveFlight, flight_args_vector);

                            writeToStream(m_flightOutputStream, flight_message);

                            TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_flightInputStream.readObject();

                            if (response.getReturn() != "true"){
                                flights_reserved_success = false;
                                break;
                            }
                        }

                        if (car) {
                            Vector<String> car_args_vector = new Vector<String>();
                            car_args_vector.add(String.valueOf(customerID));
                            car_args_vector.add(location);
                            TCPCommandMessage car_message = new TCPCommandMessage(Command.ReserveCar, car_args_vector);

                            writeToStream(m_carOutputStream, car_message);

                            TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_carInputStream.readObject();

                            if (response.getReturn() != "true"){
                                cars_reserved_success = false;
                            }
                        }

                        if (room) {
                            Vector<String> room_args_vector = new Vector<String>();
                            room_args_vector.add(String.valueOf(customerID));
                            room_args_vector.add(location);
                            TCPCommandMessage room_message = new TCPCommandMessage(Command.ReserveCar, room_args_vector);

                            writeToStream(m_roomOutputStream, room_message);

                            TCPCommandMessageResponse response = (TCPCommandMessageResponse) m_roomInputStream.readObject();

                            if (response.getReturn() != "true"){
                                cars_reserved_success = false;
                            }
                        }

                        boolean overall_success = flights_reserved_success && cars_reserved_success && rooms_reserved_success;

                        TCPCommandMessageResponse overall_response = new TCPCommandMessageResponse(msg_command, String.valueOf(overall_success));
                        
                        writeToStream(m_clientOutputStream, overall_response);
                    } 
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToStream(ObjectOutputStream out, Object obj) throws IOException {
        out.writeObject(obj);
        out.flush();
    }
}
