package Server.RMI;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import Server.Common.Car;
import Server.Common.Flight;
import Server.Common.Room;
import Server.Interface.IResourceManager;

public class RMIMiddleware implements IResourceManager {

    private IResourceManager m_rmCar;
    private IResourceManager m_rmRoom;
    private IResourceManager m_rmFlight;

    private static String s_serverName;
    private static String s_car_hostname;
    private static String s_room_hostname;
    private static String s_flight_hostname;

    private static int s_serverPort = 3026;
	private static String s_rmiPrefix = "group_26_";

    public RMIMiddleware(String p_flight_hostname, String p_car_hostname, String p_room_hostname) throws Exception {
        s_flight_hostname =  p_flight_hostname;
        s_car_hostname = p_car_hostname;
        s_room_hostname = p_room_hostname;
        s_serverName = "RMI_Middleware";
    }

    public boolean addFlight(int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return m_rmFlight.addFlight(flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(String location, int count, int price) throws RemoteException {
        return m_rmCar.addCars(location, count, price);
    }

    public boolean addRooms(String location, int count, int price) throws RemoteException {
        return m_rmRoom.addRooms(location, count, price);
    }

    public int newCustomer() throws RemoteException {
        int cid = Integer.parseInt(String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
        newCustomer(cid);
        return cid;
    }

    public boolean newCustomer(int cid) throws RemoteException {
        boolean flight_success = m_rmFlight.newCustomer(cid);
        boolean car_success = m_rmCar.newCustomer(cid);
        boolean room_success = m_rmRoom.newCustomer(cid);
        return flight_success && car_success && room_success;
    }

    public boolean deleteFlight(int flightNum) throws RemoteException {
        return m_rmFlight.deleteFlight(flightNum);
    }

    public boolean deleteCars(String location) throws RemoteException {
        return m_rmCar.deleteCars(location);
    }

    public boolean deleteRooms(String location) throws RemoteException {
        return m_rmRoom.deleteRooms(location);
    }

    public boolean deleteCustomer(int customerID) throws RemoteException {
        boolean flight_success = m_rmFlight.deleteCustomer(customerID);
        boolean car_success = m_rmCar.deleteCustomer(customerID);
        boolean room_success = m_rmRoom.deleteCustomer(customerID);
        return flight_success && car_success && room_success;
    }

    public int queryFlight(int flightNum) throws RemoteException {
        return m_rmFlight.queryFlight(flightNum);
    }

    public int queryCars(String location) throws RemoteException {
        return m_rmCar.queryCars(location);
    }

    public int queryRooms(String location) throws RemoteException {
        return m_rmRoom.queryRooms(location);
    }

    public String queryCustomerInfo(int customerID) throws RemoteException {
        String flight_info = m_rmFlight.queryCustomerInfo(customerID);
        String car_info = m_rmCar.queryCustomerInfo(customerID);
        String room_info = m_rmRoom.queryCustomerInfo(customerID);
        return flight_info + car_info + room_info;
    }
    public int queryFlightPrice(int flightNum) throws RemoteException {
        return m_rmFlight.queryFlightPrice(flightNum);
    }

    public int queryCarsPrice(String location) throws RemoteException {
        return m_rmCar.queryCarsPrice(location);
    }

    public int queryRoomsPrice(String location) throws RemoteException {
        return m_rmRoom.queryRoomsPrice(location);
    }

    public boolean reserveFlight(int customerID, int flightNum) throws RemoteException {
        return m_rmFlight.reserveFlight(customerID, flightNum);
    }
    
    public boolean reserveCar(int customerID, String location) throws RemoteException {
        return m_rmCar.reserveCar(customerID, location);
    }

    public boolean reserveRoom(int customerID, String location) throws RemoteException {
        return m_rmRoom.reserveRoom(customerID, location);
    }  

    public boolean bundle(int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException{

        String customerInfo = queryCustomerInfo(customerID);

        if (customerInfo.equals("")) {
            System.out.println("Bundle failed, customer with ID" + customerID + "doesn't exist.");
            return false;
        }

        Map<String, Integer> seatMap = new HashMap<>();
            for (String flightNum : flightNumbers) {
                seatMap.put(flightNum, seatMap.getOrDefault(flightNum, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : seatMap.entrySet()) {
            String flightNum = entry.getKey();
            int neededSeats = entry.getValue();

            int availableSeats = queryFlight(Integer.parseInt(flightNum));

            if (availableSeats < neededSeats) {
                System.out.println("Bundle failed, not enough seats for flight with ID:" + flightNum);
                return false;
            }
        }

        if (car){
            int numCars = queryCars(location);
            if (numCars == 0){
                System.out.println("Bundle failed, no available cars at location:" + location);
                return false;
            }
        }

        if (room){
            int numRooms = queryRooms(location);
            if (numRooms == 0){
                System.out.println("Bundle failed, no available rooms at location:" + location);
                return false;
            }
        }

        System.out.println("Bundle succeeded, reserving...");

        for (String flightNum : flightNumbers) {
            m_rmFlight.reserveFlight(customerID, Integer.parseInt(flightNum));
        }

        if (car) {
            m_rmCar.reserveCar(customerID, location);
        }

        if (room) {
            m_rmRoom.reserveRoom(customerID, location);
        }
    
        return true;
    }

    public String getName() throws RemoteException {
        return s_serverName;
    }

    public void connectServer(String server, int port, String name, RMIServerType type)
	{
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
                    if (type == RMIServerType.CAR)
                        m_rmCar = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    else if (type == RMIServerType.ROOM)
                        m_rmRoom = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    else if (type == RMIServerType.FLIGHT)
                        m_rmFlight = (IResourceManager) registry.lookup(s_rmiPrefix + name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
						first = false;
					}
				}
				Thread.sleep(500);
			}
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
            s_flight_hostname = args[0];
        }

        if (args.length > 1)
        {
            s_car_hostname = args[1];
        }

        if (args.length > 2)
        {
            s_room_hostname = args[2];
        }
			
		// Create the RMI server entry
		try {
			// Create a new Middleware object
			RMIMiddleware middleware_server = new RMIMiddleware(s_flight_hostname, s_car_hostname, s_room_hostname);

            middleware_server.connectServer(s_flight_hostname, s_serverPort, "Flights", RMIServerType.FLIGHT);
            middleware_server.connectServer(s_car_hostname, s_serverPort, "Cars", RMIServerType.CAR);
            middleware_server.connectServer(s_room_hostname, s_serverPort, "Rooms", RMIServerType.ROOM);
            
			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(middleware_server, 0);

			// Bind the remote object's stub in the registry; adjust port if appropriate
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(3026);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(3026);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

    private enum RMIServerType {
        CAR,
        ROOM,
        FLIGHT
    }
}