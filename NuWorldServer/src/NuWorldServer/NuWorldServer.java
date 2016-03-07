/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer;

import NuWorldServer.Messages.ClearBlock;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetBlock;
import NuWorldServer.Messages.SetPlayerLocation;
import NuWorldServer.Messages.SetupMessages;
import NuWorldServer.Messages.UpdatePlayerEntities;
import com.cubes.BlockManager;
import com.cubes.BlockNavigator;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import strongdk.jme.appstate.console.ConsoleAppState;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author funin_000
 */
public class NuWorldServer implements ConnectionListener, MessageListener<HostedConnection> {
    
    private ConsoleAppState console;
    public NuWorldServer(ConsoleAppState outputConsole) {
        console = outputConsole;
        allConnections = new ArrayList<HostedConnection>();
    }

    // JME Spider server listening for connections
    private Server server;
    
    // All client connections
    ArrayList<HostedConnection> allConnections;

    private void logError(String text) {
        console.appendConsoleError(text);
        System.err.printf(text);
    }
    
    private void logOutput(String text) {
        console.appendConsole(text);
        System.out.printf(text);
    }
    private final int SERVER_PORT = 6143;
    
    public void Start() {
        try {
            server = Network.createServer(SERVER_PORT);
        } catch (IOException ex) {
            logError("Failed to start Network Server");
            logError(ex.toString());
        }
        if (server != null) {
            logOutput("Created Server");
            SetupMessages.RegisterAllMessageTypes();
            server.addConnectionListener(this);
            server.addMessageListener(this);
            initBlockTerrain();
            server.start();
            logOutput("Server Started on port " + SERVER_PORT);
        } else {
            logError("Server failed");
        }
    }

    public void connectionAdded(Server server, HostedConnection conn) {
        logOutput("Connection Received");
        allConnections.add(conn);
        Vector3f defaultStartingLocation = new Vector3f(5, TERRAIN_SIZE.getY() + 5, 5).mult(cubesSettings.getBlockSize());

        // Send chunks in 5 chunks all around
        Vector3Int blockAtPlayer = BlockNavigator.getPointedBlockLocation(blockTerrain, defaultStartingLocation, false);
        Vector3Int chunkAtPlayer = blockTerrain.getChunkLocation(blockAtPlayer);
        int range = 5;
        for(int x = chunkAtPlayer.getX() - range; x <= chunkAtPlayer.getX() + range; ++x) {
            for(int y = chunkAtPlayer.getY() - range; y <= chunkAtPlayer.getY() + range; ++y) {
                for(int z = chunkAtPlayer.getZ() - range; z <= chunkAtPlayer.getZ() + range; ++z) {
                    Vector3Int chunkToSend = new Vector3Int(x,y,z);
                    // TODO generate missing chunks on demand
                    //Vector3Int chunkToSend = chunkAtPlayer;
                    if (blockTerrain.isValidChunkLocation(chunkToSend)) {
                        ArrayList<byte[]> slices = blockTerrain.writeChunkPartials(chunkToSend);

                        // The terrain renderer doesn't like building from 0->256
                        logOutput("Sending Chunk Slices" + chunkToSend.getX() + ", " + chunkToSend.getY() + ", " + chunkToSend.getZ());
                        for (int i = slices.size() - 1; i >= 0 ; --i) {
                            ResetChunk resetMessage = new ResetChunk(slices.get(i));
                            conn.send(resetMessage);
                        }
                    }
                }
            }
        }
        
        SetPlayerLocation playerLoc = new SetPlayerLocation("" + conn.getId(), defaultStartingLocation);
        logOutput("Sending Player Loc Message");
        conn.send(playerLoc);


        
        
        //blockTerrain.getChunks()

    
/*        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BitOutputStream bitOutputStream = new BitOutputStream(byteArrayOutputStream);
        bitSerializable.write(bitOutputStream);
        bitOutputStream.close();
        return byteArrayOutputStream.toByteArray();
 */   
    
    }

    public void connectionRemoved(Server server, HostedConnection conn) {
        logOutput("Connection Lost");
        entityManager.cleanConnection(conn.getId());
        allConnections.remove(conn);
    }

    public void messageReceived(HostedConnection source, Message message) {

        logOutput("Message Received");
        //logOutput(m.toString());
            //public Object call() throws Exception {
                if (message instanceof SetPlayerLocation) {
                  SetPlayerLocation playerLocMessage = (SetPlayerLocation) message;
                  System.out.println("Server received '" +playerLocMessage.getPlayerLoc().toString() +"' from client #"+source.getId() );
                  entityManager.setPlayerLocation(source.getId(), playerLocMessage.getPlayerLoc());
                  // TODO put this on a timer
                  server.broadcast(entityManager.buildUpdatePlayersMessage());
                } else if (message instanceof ResetChunk) {
                }
                else if (message instanceof SetBlock) {
                    SetBlock setMessage = (SetBlock)message;                    
                    blockTerrain.setBlock(setMessage.getBlock(), BlockManager.getBlock((byte)setMessage.getBlockID()));
                    server.broadcast(setMessage);                    
                } 
                else if (message instanceof ClearBlock) {
                    ClearBlock clearMessage = (ClearBlock)message;
                    blockTerrain.removeBlock(clearMessage.getBlock());
                    server.broadcast(clearMessage);                    
                }
                else if (message instanceof UpdatePlayerEntities) {
                    
                }
//                return null;
//            }
        
    }
    private CubesSettings cubesSettings;
    private BlockTerrainControl blockTerrain;
    private ServerEntities entityManager;
    private final Vector3Int TERRAIN_SIZE = new Vector3Int(100, 30, 100);
    
      private void initBlockTerrain(){
        CubeAssets.registerBlocks();
        CubeAssets.initializeEnvironment(null);
        
        cubesSettings = CubeAssets.getSettings(null);
        blockTerrain = new BlockTerrainControl(cubesSettings, new Vector3Int(2, 1, 2));
        blockTerrain.setBlocksFromNoise(new Vector3Int(),  TERRAIN_SIZE, 0.8f, CubeAssets.BLOCK_GRASS);
        
        entityManager = new ServerEntities();
        /*blockTerrain.addChunkListener(new BlockChunkListener(){

            @Override
            public void onSpatialUpdated(BlockChunkControl blockChunk){
                Geometry optimizedGeometry = blockChunk.getOptimizedGeometry_Opaque();
                RigidBodyControl rigidBodyControl = optimizedGeometry.getControl(RigidBodyControl.class);
                if(rigidBodyControl == null){
                    rigidBodyControl = new RigidBodyControl(0);
                    optimizedGeometry.addControl(rigidBodyControl);
                    bulletAppState.getPhysicsSpace().add(rigidBodyControl);
                }
                rigidBodyControl.setCollisionShape(new MeshCollisionShape(optimizedGeometry.getMesh()));
            }
        });*/
        //terrainNode.addControl(blockTerrain);
        //terrainNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //this.app.getRootNode().attachChild(terrainNode);
    }
}



