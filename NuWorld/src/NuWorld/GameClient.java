/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import NuWorldServer.Messages.SetBlock;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import java.io.IOException;
import strongdk.jme.appstate.console.ConsoleAppState;

/**
 *
 * @author funin_000
 */
public class GameClient implements ClientStateListener, MessageListener<Client>{
    private ConsoleAppState console;
    
    private void logError(String text) {
        console.appendConsoleError(text);
        System.err.printf(text);
    }
    
    private void logOutput(String text) {
        console.appendConsole(text);
        System.out.printf(text);
    }
    private final int SERVER_PORT = 6143;
    private Client client;

    public GameClient(ConsoleAppState gameConsole) {
        console = gameConsole;

    }
    public String connectionIP;
    public void connect(String serverIP) {
        connectionIP = serverIP;
        try {
            client = Network.connectToServer(connectionIP, SERVER_PORT);
        } catch (IOException ex) {
            logError("Failed to connect to server");
            logError(ex.toString());
        }
        if (client != null) {
            logOutput("Connecting to server! " + serverIP);
            client.addClientStateListener(this);
            client.addMessageListener(this);
            logOutput("Client started");
        } else {
            logError("Failed to connect");
        }
    }
    
    public void start() {
        client.start();
    }

    public void clientConnected(Client c) {
        logOutput("Connection Successful");
    }

    public void clientDisconnected(Client c, DisconnectInfo info) {
        logOutput("Connection Lost");
    }
    
    public void removeMessageListener(MessageListener<Client> clientListener) {
        client.removeMessageListener(clientListener);        
    }
    
    public void addMessageListener(MessageListener<Client> clientListener) {
        client.addMessageListener(clientListener);
    }

    public void messageReceived(Client source, Message m) {
        //logOutput("(Generic Handler) Message received " + m.toString());
    }

    void sendMessage(Message message) {
        if (client != null && client.isConnected()) {
            client.send(message);
        } else {
            System.err.println("Attempting to send message " + message.toString() + " to closed connection");
        }
    }
}
