/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer;

import NuWorldServer.Messages.RequestChunk;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetPlayerLocation;
import com.cubes.BlockNavigator;
import com.cubes.Vector3Int;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author funin_000
 * Class to track a given players resources and assets.
 */
public class PlayerClient {
    private NuWorldServer server;
    private HostedConnection connection;
    private Vector3f playerWorldLocation;
    private Vector3Int lastPlayerChunkLocation;
    private String playerName;
    private long lastReceivedMessage;
    private Set<Vector3Int> loadedChunks = new HashSet<Vector3Int>();
    public PlayerClient (NuWorldServer server, HostedConnection connection) {
        this.server = server;
        this.connection = connection;
        this.onConnect();
    }
    private void updateLastReceivedTime() {
        this.lastReceivedMessage = Calendar.getInstance().getTimeInMillis();
    }
    public void handlePlayerLocation(SetPlayerLocation message) {
        playerWorldLocation = message.getPlayerLoc();
        lastPlayerChunkLocation = server.getChunkAtLocation(playerWorldLocation);
    }
    private void onConnect() {
        Vector3f defaultStartingLocation = server.getDefaultPlayerStartLocation();
        Vector3Int chunkAtPlayer = server.getChunkAtLocation(defaultStartingLocation);
        playerWorldLocation = defaultStartingLocation;
        lastPlayerChunkLocation = chunkAtPlayer;
        /*int range = 3;
        for(int x = Math.max(0, chunkAtPlayer.getX() - range); x <= chunkAtPlayer.getX() + range; ++x) {
            //for(int y = chunkAtPlayer.getY() - range; y <= chunkAtPlayer.getY() + range; ++y) {
                for(int z = Math.max(0, chunkAtPlayer.getZ() - range); z <= chunkAtPlayer.getZ() + range; ++z) {
                    Vector3Int chunkToSend = new Vector3Int(x,0,z);
                    server.ensurechunk(chunkToSend);
                    System.out.println( "attempting to send " + chunkToSend.toString());
                    if (server.sendChunkToPlayer(chunkToSend, connection) ) {
                        System.out.println( "sent " + chunkToSend.toString());
                        loadedChunks.add(chunkToSend);
                    }
                }
            //}
        }*/
        sendPlayerLocation();
    }
     public void teleportPlayer(Vector3f newPlayerLocation) {   
        this.playerWorldLocation = newPlayerLocation;
        Vector3Int chunkAtPlayer = server.getChunkAtLocation(newPlayerLocation);
        lastPlayerChunkLocation = chunkAtPlayer;
        this.sendPlayerLocation();
     }
     private void sendPlayerLocation() {
        SetPlayerLocation playerLoc = new SetPlayerLocation("" + connection.getId(), playerWorldLocation);
        server.logOutput("Sending Player Loc Message");
        connection.send(playerLoc);
    }

    void handleRequestChunk(RequestChunk chunk) {
        Vector3Int v = chunk.getChunkLoc();
        server.ensurechunk(v);
        if (server.sendChunkToPlayer(v, connection) ) {
            loadedChunks.add(v);
        }
    }
    
}
