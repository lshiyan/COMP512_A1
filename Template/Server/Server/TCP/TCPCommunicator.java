package Server.TCP;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Utility class for TCP message communication
 * Handles serialization, framing, and socket I/O
 */
public class TCPCommunicator {

    /**
     * Send a TCPMessage over a socket
     */
    public static void sendMessage(Socket socket, TCPMessage message) throws IOException {
        try {
            // Serialize the message
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(message);
            objectStream.flush();
            byte[] messageBytes = byteStream.toByteArray();

            // Send length-prefixed message
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

    /**
     * Receive a TCPMessage from a socket
     */
    public static TCPMessage receiveMessage(Socket socket) throws IOException, ClassNotFoundException {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Read message length first
            int messageLength = in.readInt();

            // Read exact message bytes
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);

            // Deserialize the message
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

    /**
     * Safe close of socket
     */
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