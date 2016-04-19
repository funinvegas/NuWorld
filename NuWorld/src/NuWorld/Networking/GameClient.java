/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld.Networking;

import NuWorld.PlayerEntity;
import NuWorldServer.Messages.RequestChunk;
import NuWorldServer.Messages.SetBlock;
import com.cubes.BlockChunkControl;
import com.cubes.BlockTerrainControl;
import com.cubes.Vector3Int;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    public void sendMessage(Message message) {
        if (client != null && client.isConnected()) {
            client.send(message);
        } else {
            System.err.println("Attempting to send message " + message.toString() + " to closed connection");
        }
    }
    private boolean checkBlock(BlockTerrainControl blockTerrain, Vector3Int currentChunk, int iX, int iY, int iZ) {
        Vector3Int locationToCheck = Vector3Int.create(currentChunk.getX() + iX, currentChunk.getY() + iY, currentChunk.getZ() + iZ);
        //System.out.println("LW checking " + locationToCheck.toString());
        if (!blockTerrain.isValidChunkLocation(locationToCheck) && !blockTerrain.isPendingChunkLocation(locationToCheck)) {
            RequestChunk requestChunk = new RequestChunk(locationToCheck);
            System.out.println("Requesting " + BlockTerrainControl.keyify(locationToCheck));
            //System.out.println("Requesting chunk " + locationToCheck.toString());
            sendMessage(requestChunk);
            blockTerrain.readUnlock("requestNextChunk");
            blockTerrain.setChunkStarted(locationToCheck);
            return true;
        }
        Vector3Int.dispose(locationToCheck);
        return false;
    }
    public void requestNextChunk(BlockTerrainControl blockTerrain, PlayerEntity primaryEntity) {
            if (blockTerrain != null && primaryEntity != null && !blockTerrain.getChunksInProgress()) {
                Vector3Int currentChunk = blockTerrain.worldLocationToChunkLocation(primaryEntity.getLocation());
                blockTerrain.cullChunks(currentChunk, 10);
                //System.out.println("player loc is " + currentChunk.toString());
                // TODO: Path find reachable chunks ahead of chunks that would require digging thru chunks
                long startTime = System.currentTimeMillis();
                long endTime;
                boolean hasLock = blockTerrain.readLockTry("requestNextChunk", 5); 
                if (hasLock) {
                    try {
                        if (blockTerrain.pendingChunkCount() == 0) {
                            int blockDistance = 6; 
                            for (int i = 0; i < blockDistance; ++i) {
                                for (int iX = -i; iX <= i; ++iX) {
                                  for (int iY = -i; iY <= i; ++iY) {
                                    for (int iZ = -i; iZ <= i; ++iZ) {
                                        if ((iX == -i || iX == i || 
                                             iY == -i || iY == i || 
                                             iZ == -i || iZ == i) && checkBlock(blockTerrain, currentChunk, iX, iY, iZ)) {
                                            endTime = System.currentTimeMillis();
                                            if (endTime - startTime> 2) {
                                                System.out.println("RequestNextCunk found a block after " + (endTime - startTime));
                                            }
                                            hasLock = false;
                                            return;
                                        }
                                    }
                                  }
                                }
                            }
                        }
                    }
                    finally {
                        if (hasLock) {
                            blockTerrain.readUnlock("requestNextChunk"); 
                        }
                    }
                }

                endTime = System.currentTimeMillis();
                if (endTime - startTime> 2) {
                    System.out.println("RequestNextCunk found nothing after " + (endTime - startTime));
                }
                /*for (int iX = 0; iX < 5; ++iX) {
                  for (int iY = -5; iY < 5; ++iY) {
                    for (int iZ = 0; iZ < 5; ++iZ) {
                        // left wall
                        for( int i = 0; i < iZ * 2; ++i) {
                            Vector3Int locationToCheck = Vector3Int.create(currentChunk.getX() - iX, currentChunk.getY() + iY, currentChunk.getZ() - iZ + i);
                            //System.out.println("LW checking " + locationToCheck.toString());
                            if (!blockTerrain.isValidChunkLocation(locationToCheck)) {
                                RequestChunk requestChunk = new RequestChunk(locationToCheck);
                                //System.out.println("Requesting chunk " + locationToCheck.toString());
                                sendMessage(requestChunk);
                                return;
                            }
                        } 
                        // right wall
                        for( int i = 0; i < iZ * 2; ++i) {
                            Vector3Int locationToCheck = Vector3Int.create(currentChunk.getX() + iX, currentChunk.getY(), currentChunk.getZ() - iZ + i);
                            //System.out.println("RW checking " + locationToCheck.toString());
                            if (!blockTerrain.isValidChunkLocation(locationToCheck)) {
                                RequestChunk requestChunk = new RequestChunk(locationToCheck);
                                //System.out.println("Requesting chunk " + locationToCheck.toString());
                                sendMessage(requestChunk);
                                return;
                            }
                        } 
                        // top wall
                        for( int i = 0; i < iZ * 2; ++i) {
                            Vector3Int locationToCheck = Vector3Int.create(currentChunk.getX() - iX + i, currentChunk.getY(), currentChunk.getZ() - iZ);
                            //System.out.println("TW checking " + locationToCheck.toString());
                            if (!blockTerrain.isValidChunkLocation(locationToCheck)) {
                                RequestChunk requestChunk = new RequestChunk(locationToCheck);
                                //System.out.println("Requesting chunk " + locationToCheck.toString());
                                sendMessage(requestChunk);
                                return;
                            }
                        } 
                        // bottom wall
                        for( int i = 0; i < iZ * 2; ++i) {
                            Vector3Int locationToCheck = Vector3Int.create(currentChunk.getX() - iX + i, currentChunk.getY(), currentChunk.getZ() + iZ);
                            //System.out.println("BW checking " + locationToCheck.toString());
                            if (!blockTerrain.isValidChunkLocation(locationToCheck)) {
                                RequestChunk requestChunk = new RequestChunk(locationToCheck);
                                //System.out.println("Requesting chunk " + locationToCheck.toString());
                                sendMessage(requestChunk);
                                return;
                            }
                        } 
                    }
                  }
                }*/
            }
    }

    public void setBlockFinished() {
        System.out.println("Finished Chunk");
    }

}
