package Server.TCP;

import java.io.Serializable;
import java.util.Vector;

import Client.Command;

public class TCPCommandMessage implements Serializable{
	
    private Vector<String> m_commandargs;
    private Command m_command;

    public TCPCommandMessage(Command  p_Command, Vector<String> p_args) {
        m_command = p_Command;
        m_commandargs = p_args;
    }

    public Vector<String> getCommandArgs() {
        return m_commandargs;
    }

    public Command getCommand() {
        return m_command;
    }
}
