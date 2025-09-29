package Server.TCP;
import java.io.Serializable;

public class TCPMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        REQUEST, RESPONSE, ERROR
    }
    
    public enum Command {
        ADD_FLIGHT, ADD_CARS, ADD_ROOMS,
        NEW_CUSTOMER, NEW_CUSTOMER_ID,
        DELETE_FLIGHT, DELETE_CARS, DELETE_ROOMS, DELETE_CUSTOMER,
        QUERY_FLIGHT, QUERY_CARS, QUERY_ROOMS, QUERY_CUSTOMER,
        QUERY_FLIGHT_PRICE, QUERY_CARS_PRICE, QUERY_ROOMS_PRICE,
        RESERVE_FLIGHT, RESERVE_CAR, RESERVE_ROOM,
        BUNDLE, GET_NAME
    }

    private MessageType messageType;
    private Command command;
    private Object[] arguments;
    private Object result;
    private String errorMessage;
    private int messageId;

    // requests
    public TCPMessage(int messageId, Command command, Object... arguments) {
        this.messageId = messageId;
        this.messageType = MessageType.REQUEST;
        this.command = command;
        this.arguments = arguments;
    }

    // successful responses
    public TCPMessage(int messageId, Object result) {
        this.messageId = messageId;
        this.messageType = MessageType.RESPONSE;
        this.result = result;
    }

    // error responses
    public TCPMessage(int messageId, String errorMessage) {
        this.messageId = messageId;
        this.messageType = MessageType.ERROR;
        this.errorMessage = errorMessage;
    }

    // getters and setters
    public MessageType getMessageType() { return messageType; }
    public Command getCommand() { return command; }
    public Object[] getArguments() { return arguments; }
    public Object getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public int getMessageId() { return messageId; }

    public void setResult(Object result) {
        this.result = result;
        this.messageType = MessageType.RESPONSE;
    }

    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.messageType = MessageType.ERROR;
    }

    @Override
    public String toString() {
        return String.format("TCPMessage[id=%d, type=%s, command=%s, args=%d]",
                           messageId, messageType, command, arguments != null ? arguments.length : 0);
    }
}