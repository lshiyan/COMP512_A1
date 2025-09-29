package Server.TCP;

import java.io.Serializable;

import Client.Command;

public class TCPCommandMessageResponse implements Serializable{
    
    private Command m_command;
    private String m_return;

    public TCPCommandMessageResponse(Command p_command, String p_return) {
        m_return = p_return;
    }

    public String getReturn() {
        return m_return;
    }

    public Command getCommand() {
        return m_command;
    }
}
