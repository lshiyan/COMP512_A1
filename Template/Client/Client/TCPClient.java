package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Vector;

import Server.Interface.IResourceManager;

public class TCPClient extends Client
{

    private static String s_serverHost;
	private static int s_serverPort = 3026;
	private static String s_serverName;
	
	private Socket m_socket;
	private ObjectOutputStream m_out;
	private ObjectInputStream m_in;

    public TCPClient()
    {
        super();
    }

    public void connectServer()
	{
		connectServer(s_serverHost, s_serverPort, s_serverName);
	}

	@Override 
	public void execute(Command cmd, Vector<String> arguments) throws IOException, ClassNotFoundException
	{
		switch (cmd)
		{
			case Help:
			{
				if (arguments.size() == 1) {
					System.out.println(Command.description());
				} else if (arguments.size() == 2) {
					Command l_cmd = Command.fromString((String)arguments.elementAt(1));
					System.out.println(l_cmd.toString());
				} else {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
				}
				break;
			}
			case AddFlight: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Adding a new flight ");
				System.out.println("-Flight Number: " + arguments.elementAt(1));
				System.out.println("-Flight Seats: " + arguments.elementAt(2));
				System.out.println("-Flight Price: " + arguments.elementAt(3));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Flight added");
				} else {
					System.out.println("Flight could not be added");
				}
				
				break;
			}
			case AddCars: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Adding new cars");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				System.out.println("-Number of Cars: " + arguments.elementAt(2));
				System.out.println("-Car Price: " + arguments.elementAt(3));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Cars added");
				} else {
					System.out.println("Cars could not be added");
				}

				break;
			}
			case AddRooms: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Adding new rooms");
				System.out.println("-Room Location: " + arguments.elementAt(1));
				System.out.println("-Number of Rooms: " + arguments.elementAt(2));
				System.out.println("-Room Price: " + arguments.elementAt(3));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);
				
				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Rooms added");
				} else {
					System.out.println("Rooms could not be added");
				}
				break;
			}
			case AddCustomer: {
				checkArgumentsCount(1, arguments.size());

				System.out.println("Adding a new customer:=");

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				System.out.println("New customer id: " + response.getReturn());
				
				break;
			}
			case AddCustomerID: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer");
				System.out.println("-Customer ID: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Customer added with id:" + arguments.elementAt(1));
				} else {
					System.out.println("Customer could not be added");
				}

				break;
			}
			case DeleteFlight: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting a flight");
				System.out.println("-Flight Number: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Flight deleted");
				} else {
					System.out.println("Flight could not be deleted");
				}

				break;
			}
			case DeleteCars: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting all cars at a particular location");
				System.out.println("-Car Location: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Cars deleted");
				} else {
					System.out.println("Cars could not be deleted");
				}

				break;
			}
			case DeleteRooms: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting all rooms at a particular location");
				System.out.println("-Car Location: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){	
					System.out.println("Rooms deleted");
				} else {
					System.out.println("Rooms could not be deleted");
				}

				break;
			}
			case DeleteCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting a customer from the database");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				
				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Customer deleted");
				} else {
					System.out.println("Customer could not be deleted");
				}

				break;
			}
			case QueryFlight: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying a flight");
				System.out.println("-Flight Number: " + arguments.elementAt(1));
				
				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				System.out.println("Number of seats available: " + response.getReturn());
				break;
			}
			case QueryCars: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying cars location");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				
				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				System.out.println("Number of cars available: " + response.getReturn());

				break;
			}
			case QueryRooms: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying rooms location");
				System.out.println("-Room Location: " + arguments.elementAt(1));
				
				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				System.out.println("Number of rooms available: " + response.getReturn());

				break;
			}
			case QueryCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying customer information");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				
				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				String bill = response.getReturn();
				System.out.println(bill);

				break;               
			}
			case QueryFlightPrice: {
				checkArgumentsCount(2, arguments.size());
				
				System.out.println("Querying a flight price");
				System.out.println("-Flight Number: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				int price = Integer.parseInt(response.getReturn());
				System.out.println("Price of a seat: " + price);

				break;
			}
			case QueryCarsPrice: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying cars price");
				System.out.println("-Car Location: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				int price = Integer.parseInt(response.getReturn());
				System.out.println("Price of cars at this location: " + price);

				break;
			}
			case QueryRoomsPrice: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying rooms price");
				System.out.println("-Room Location: " + arguments.elementAt(1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				int price = Integer.parseInt(response.getReturn());
				System.out.println("Price of rooms at this location: " + price);

				break;
			}
			case ReserveFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Reserving seat in a flight");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Flight reserved");
				} else {
					System.out.println("Flight could not be reserved");
				}

				break;
			}
			case ReserveCar: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Reserving a car at a location");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				System.out.println("-Car Location: " + arguments.elementAt(2));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){	
					System.out.println("Car reserved");
				} else {
					System.out.println("Car could not be reserved");
				}

				break;
			}
			case ReserveRoom: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Reserving a room at a location");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				System.out.println("-Room Location: " + arguments.elementAt(2));
				
				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Room reserved");
				} else {
					System.out.println("Room could not be reserved");
				}

				break;
			}
			case Bundle: {
				if (arguments.size() < 6) {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 6 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				for (int i = 0; i < arguments.size() - 5; ++i)
				{
					System.out.println("-Flight Number: " + arguments.elementAt(2+i));
				}
				System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size()-3));
				System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
				System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));

				TCPCommandMessage message = new TCPCommandMessage(cmd, arguments);
				sendMessage(message);

				TCPCommandMessageResponse response = receiveResponse();
				if (response.getReturn() == "true"){
					System.out.println("Bundle reserved");
				} else {
					System.out.println("Bundle could not be reserved");
				}
				break;
			}
			case Quit:
				checkArgumentsCount(1, arguments.size());

				System.out.println("Quitting client");
				System.exit(0);
		}
		
	}
	
	public void sendMessage(TCPCommandMessage message) throws IOException
	{
		m_out.writeObject(message);
	}

	public TCPCommandMessageResponse receiveResponse() throws IOException, ClassNotFoundException
	{
		return (TCPCommandMessageResponse) m_in.readObject();
	}

	public void connectServer(String server, int port, String name)
	{
		try {
			m_socket = new Socket(server, port);
			m_out = new ObjectOutputStream(m_socket.getOutputStream());
			m_in = new ObjectInputStream(m_socket.getInputStream());
			System.out.println("Connected to server " + server + " on port " + port);
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

    public static void main(String args[])
	{	
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}

		if (args.length > 1)
		{
			s_serverName = args[1];
		}

		try {
			TCPClient client = new TCPClient();
			client.connectServer();
			client.start();
		} 
		catch (Exception e) {    
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
