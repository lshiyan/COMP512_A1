package Server.TCP;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/* Handles serialization, framing, and socket */
public class TCPCommunicator {
    public static void sendMessage(Socket socket, TCPMessage message) throws IOException {
        try {
            // Serialize message
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(message);
            objectStream.flush();
            byte[] messageBytes = byteStream.toByteArray();

            // Send message
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(messageBytes.length);
            out.write(messageBytes);
            out.flush();

            objectStream.close();
            byteStream.close();

        } catch (SocketException e) {
            throw new IOException("Socket closed during message send", e);
        }
    }


    public static TCPMessage receiveMessage(Socket socket) throws IOException, ClassNotFoundException {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());


            int messageLength = in.readInt();
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);

            // Deserialize message
            ByteArrayInputStream byteStream = new ByteArrayInputStream(messageBytes);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);
            TCPMessage message = (TCPMessage) objectStream.readObject();
            objectStream.close();
            byteStream.close();

            return message;

        } catch (SocketException e) {
            throw new IOException("Socket closed during message receive", e);
        }
    }

    public static void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}