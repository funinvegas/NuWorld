/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer;

import NuWorldServer.Messages.ClearBlock;
import NuWorldServer.Messages.RequestChunk;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetBlock;
import NuWorldServer.Messages.SetPlayerLocation;
import NuWorldServer.Messages.SetupMessages;
import NuWorldServer.Messages.UpdatePlayerEntities;
import com.cubes.BlockChunkControl;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import terrain.TerrainGenerator;
import terrain.TerrainGeneratorFactory;
import terrain.TerrainGeneratorFactory;

/**
 *
 * @author funin_000
 */
public class NuWorldServer implements ConnectionListener, MessageListener<HostedConnection> {
    
    private ConsoleAppState console;
    private TerrainGenerator terrainGenerator;
    public NuWorldServer(ConsoleAppState outputConsole) {
        console = outputConsole;
        allConnections = new ArrayList<HostedConnection>();
    }

    // JME Spider server listening for connections
    private Server server;
    
    // All client connections
    ArrayList<HostedConnection> allConnections;
    private Map<Integer, PlayerClient> playerClients = new HashMap<Integer, PlayerClient> ();

    public void logError(String text) {
        console.appendConsoleError(text);
        System.err.printf(text);
    }
    
    public void logOutput(String text) {
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
    public void ensurechunk(Vector3Int chunkLoc) {
        if (this.blockTerrain.getChunkByChunkLocation(chunkLoc) == null) {
            blockTerrain.setChunk(chunkLoc, this.terrainGenerator.generateChunk(blockTerrain, chunkLoc, cubesSettings));
        }
    }

    public Vector3f getDefaultPlayerStartLocation() {
        /*Vector3Int zero = new Vector3Int(0,0,0);
        this.ensureChunk(zero);
        BlockChunkControl startingChunk = blockTerrain.getChunkByBlockLocation(zero);
        startingChunk.get
        */
        return new Vector3f(8, 255, 8).mult(cubesSettings.getBlockSize());
    }

    public void connectionAdded(Server server, HostedConnection conn) {
        logOutput("Connection Received");
        allConnections.add(conn);
        playerClients.put(conn.getId(), new PlayerClient(this, conn));



        
        
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
                else if (message instanceof RequestChunk) {
                    PlayerClient client = playerClients.get(source.getId());
                    client.handleRequestChunk((RequestChunk)message);
                }
//                return null;
//            }
        
    }
    private CubesSettings cubesSettings;
    private BlockTerrainControl blockTerrain;
    private ServerEntities entityManager;
    private final Vector3Int TERRAIN_SIZE = new Vector3Int(30, 30, 30);
    
    public Vector3Int getChunkAtLocation(Vector3f defaultStartingLocation) {
        // Send chunks in 5 chunks all around
        Vector3Int blockAtPlayer = BlockNavigator.getPointedBlockLocation(blockTerrain, defaultStartingLocation, false, null);
        Vector3Int chunkAtPlayer = blockTerrain.getChunkLocation(blockAtPlayer);
        return chunkAtPlayer;
    }
    public BlockChunkControl getChunkAt(Vector3Int chunkLoc) {
        if (!blockTerrain.isValidChunkLocation(chunkLoc)) {
            return null;
        }
        return blockTerrain.getChunkByBlockLocation(chunkLoc);
    }
    
    public boolean sendChunkToPlayer(Vector3Int chunk, HostedConnection conn) {
        if (chunk != null) {
            try {
                ArrayList<byte[]> slices = blockTerrain.writeChunkPartials(chunk);

                // The terrain renderer doesn't like building from 0->256
                logOutput("Sending Chunk Slices" + chunk.getX() + ", " + chunk.getY() + ", " + chunk.getZ());
                for (int i = slices.size() - 1; i >= 0 ; --i) {
                    ResetChunk resetMessage = new ResetChunk(slices.get(i));
                    conn.send(resetMessage);
                }
                return true;
            } catch (Exception ex) {
                logError("Failed to send chunk to client " + ex.toString());
            };
        }
        return false;
    }

    private void initBlockTerrain(){
        CubeAssets.registerBlocks();
        CubeAssets.initializeEnvironment(null);
        
        cubesSettings = CubeAssets.getSettings(null);
        blockTerrain = new BlockTerrainControl(cubesSettings, new Vector3Int(2, 1, 2));
        //blockTerrain.setBlocksFromNoise(new Vector3Int(),  TERRAIN_SIZE, 0.8f, CubeAssets.BLOCK_GRASS);
        /*for (int iX = 0; iX < 50; ++iX) {
            for (int iY = 0; iY < 30; ++iY) {
                for (int iZ = 0; iZ < 50; ++iZ) {
                    blockTerrain.setBlock(new Vector3Int(iX, iY, iZ), CubeAssets.BLOCK_GRASS);
                }
            }
        }*/

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
        this.terrainGenerator = TerrainGeneratorFactory.makeTerrainGenerator(TerrainGeneratorFactory.GeneratorType.SIMPLEX_2D, blockTerrain);
    }
}



