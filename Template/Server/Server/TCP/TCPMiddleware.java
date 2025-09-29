package Server.TCP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import Server.Interface.IResourceManager;
import Client.Command;

public class TCPMiddleware{
    
    private static InetAddress s_flight_ipadd;
    private static InetAddress s_car_ipadd;
    private static InetAddress s_room_ipadd;

    private static String s_serverName;
    private static int s_serverPort = 3026;

    public TCPMiddleware(String p_flightip, String p_carip, String p_roomip) throws UnknownHostException, IOException{
        s_serverName = "TCP_Middleware";

        s_flight_ipadd = InetAddress.getByName(p_flightip);
        s_car_ipadd = InetAddress.getByName(p_carip);
        s_room_ipadd = InetAddress.getByName(p_roomip);
    }
 
    public static void main(String[] args) throws IOException{ 
        String flightip = "";
        String carip = "";
        String roomip = "";

        if (args.length >0){
            flightip = args[0];
        }
        if (args.length >1){
            carip = args[1];
        }
        if (args.length >2){
            roomip = args[2];
        }

        TCPMiddleware middleware = new TCPMiddleware(flightip, carip, roomip);
        System.out.println(s_serverName + " started");

        middleware.runConnectionThreads();
    }

    public void runConnectionThreads() throws IOException {

        try(ServerSocket serverSocket = new ServerSocket(s_serverPort)){
            System.out.println("Listening on port " + s_serverPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                TCPMiddlewareThread connectionThread = new TCPMiddlewareThread(clientSocket, s_flight_ipadd, s_car_ipadd, s_room_ipadd);
                connectionThread.start();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
