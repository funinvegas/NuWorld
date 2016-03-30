/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import NuWorldServer.Messages.ClearBlock;
import NuWorldServer.Messages.RequestChunk;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetBlock;
import com.cubes.BlockChunkControl;
import com.cubes.BlockChunkListener;
import com.cubes.BlockManager;
import com.cubes.BlockNavigator;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;
import com.cubes.network.BitInputStream;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.AbstractPhysicsControl;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.input.ChaseCamera;
import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;


/**
 *
 * @author funin_000
 */
public class WorldManager {

    // Starting? Terrain size?
    // TODO this doesn't control the terrain size anymore,
    // its now an assumption for setting the player in a good starting spot
    public final Vector3Int TERRAIN_SIZE = new Vector3Int(100, 30, 100);

    // Physics Engine
    private BulletAppState bulletAppState;
        
    // THE cube terrain
    private BlockTerrainControl blockTerrain;
    
    // JMonkey node to hold the terrain aka root
    private Node terrainNode = new Node("Cube Terrain");

    private GameSettings gameSettings;
    
    private final AppStateManager stateManager;
    private final NuWorldMain app;
    
    private EntityManager entityManager;
    private PlayerEntity primaryEntity;
    
    public WorldManager(AppStateManager stateManager, Application ap) {
        this.stateManager = stateManager;
        this.app = (NuWorldMain)ap;
        this.gameSettings = app.getGameSettings(); 
        reset();
    }
    
    public void reset() {
        if (bulletAppState != null) {
            stateManager.detach(bulletAppState);
        }
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,-19.8f * gameSettings.getCubesSettings().getBlockSize(),0));
        //bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,-2f * gameSettings.getCubesSettings().getBlockSize(),0));
        initBlockTerrain();
        
        if (entityManager != null) {
            entityManager.cleanup();
        }
        entityManager = new EntityManager(this);
        //cam.lookAtDirection(new Vector3f(1, 0, 1), Vector3f.UNIT_Y);
    }
    private HashMap<String, BlockChunkControl> chunksToRender = new HashMap<String, BlockChunkControl>();
    private boolean readyToHandleChunks = false;
    private void updateChunk(BlockChunkControl blockChunk) {
        long startTime = Calendar.getInstance().getTimeInMillis();
        long endTime;
        Geometry optimizedGeometry = blockChunk.getOptimizedGeometry_Opaque();
        RigidBodyControl rigidBodyControl = optimizedGeometry.getControl(RigidBodyControl.class);
        if (rigidBodyControl != null) {
            optimizedGeometry.removeControl(rigidBodyControl);
            bulletAppState.getPhysicsSpace().remove(rigidBodyControl);
        }
        //if(rigidBodyControl == null){
        if (optimizedGeometry.getMesh().getVertexCount() > 0) {
            rigidBodyControl = new RigidBodyControl(0);
            optimizedGeometry.addControl(rigidBodyControl);
            bulletAppState.getPhysicsSpace().add(rigidBodyControl);
            rigidBodyControl.setCollisionShape(new MeshCollisionShape(optimizedGeometry.getMesh()));
        }
        //}
        //System.err.println("SpatialUpdated terrain is at " + terrainNode.getWorldTranslation().toString());
        //System.err.println("SpatialUpdated player is at " + playerNode.getWorldTranslation().toString());
        //playerControl.warp(new Vector3f(0,0,0));
        endTime = Calendar.getInstance().getTimeInMillis();
        if (endTime - startTime > 16) {
            System.out.println("updateChunk took " + (endTime - startTime));
        }
    }
    public void enableChunks() {
        readyToHandleChunks = true;
        for (String i : chunksToRender.keySet()) {
            updateChunk (chunksToRender.get(i));
        }
        chunksToRender = new HashMap<String, BlockChunkControl>();
    }
    private void initBlockTerrain(){
        CubeAssets.registerBlocks();
        CubeAssets.initializeEnvironment(this.app);
        
        CubesSettings cubesSettings = gameSettings.getCubesSettings();
        blockTerrain = new BlockTerrainControl(cubesSettings, new Vector3Int(7, 1, 7));

        
        //To set a block, just specify the location and the block object
        //(Existing blocks will be replaced)
        //blockTerrain.setBlock(new Vector3Int(0, 0, 0), CubeAssets.BLOCK_WOOD);
        //blockTerrain.setBlock(new Vector3Int(0, 0, 1), CubeAssets.BLOCK_WOOD);
        //blockTerrain.setBlock(new Vector3Int(1, 0, 0), CubeAssets.BLOCK_WOOD);
        //blockTerrain.setBlock(new Vector3Int(1, 0, 1), CubeAssets.BLOCK_STONE);
        //blockTerrain.setBlock(0, 0, 0, CubeAssets.BLOCK_GRASS); //For the lazy users :P

        
        //blockTerrain.setBlocksFromNoise(new Vector3Int(), TERRAIN_SIZE, 0.8f, CubeAssets.BLOCK_GRASS);
        blockTerrain.addChunkListener(new BlockChunkListener(){
            @Override
            public void onSpatialUpdated(BlockChunkControl blockChunk){
                if (readyToHandleChunks) {
                    updateChunk(blockChunk);
                } else {
                    chunksToRender.put(blockTerrain.keyify(blockChunk.getBlockLocation()), blockChunk);
                }

            }
        });
        terrainNode.addControl(blockTerrain);
        terrainNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        this.app.getRootNode().attachChild(terrainNode);
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

 
       
    public Vector3Int getCurrentPointedBlockLocation(boolean getNeighborLocation){
        CollisionResults results = getRayCastingResults(terrainNode);
        for (int i = 0; i < results.size(); i++) {
            // For each hit, we know distance, impact point, name of geometry.
            float     dist = results.getCollision(i).getDistance();
            Vector3f    pt = results.getCollision(i).getContactPoint();
            String   party = results.getCollision(i).getGeometry().getName();
            int        tri = results.getCollision(i).getTriangleIndex();
            Vector3f  norm = results.getCollision(i).getTriangle(new Triangle()).getNormal();
            //System.out.println("Details of Collision #" + i + ":");
            //System.out.println("  Party " + party + " was hit at " + pt + ", " + dist + " wu away.");
            //System.out.println("  The hit triangle #" + tri + " has a normal vector of " + norm);
            if(party == "Cube optimized_opaque") {
                Vector3f collisionContactPoint = results.getClosestCollision().getContactPoint();
                return BlockNavigator.getPointedBlockLocation(blockTerrain, pt, getNeighborLocation, norm);
            }
        }

        return null;
    }
    
    private CollisionResults getRayCastingResults(Node node){
        Vector3f origin = app.getCamera().getWorldCoordinates(new Vector2f((app.getSettings().getWidth() / 2), (app.getSettings().getHeight() / 2)), 0.0f);
        Vector3f direction = app.getCamera().getWorldCoordinates(new Vector2f((app.getSettings().getWidth() / 2), (app.getSettings().getHeight() / 2)), 0.3f);
        direction.subtractLocal(origin).normalizeLocal();
        Ray ray = new Ray(origin, direction);
        CollisionResults results = new CollisionResults();
        node.collideWith(ray, results);
        return results;
    }
    
    // TODO Make thread safe
    public void HandleResetChunk(ResetChunk resetChunk) {
        //System.out.println("Client received '" +resetChunk.getChunkData().length +"' from host" );
        BitInputStream bitInputStream = new BitInputStream(new ByteArrayInputStream(resetChunk.getChunkData()));
        boolean blockFinished = false;
        try {
            blockFinished = blockTerrain.readChunkPartial(bitInputStream);
            if (blockFinished && primaryEntity != null) {
                app.getGameClient().setBlockFinished();
                app.getGameClient().requestNextChunk(blockTerrain, primaryEntity);
            }
        } catch(IOException ex){
            ex.printStackTrace();
        }

        if (blockFinished) {
            this.app.enqueue(new Callable() {
                public Object call() throws Exception {
                    long startTime = Calendar.getInstance().getTimeInMillis();
                    long endTime;

                    blockTerrain.finishChunks();

                   // terrainNode.removeControl(blockTerrain);
                    long halfTime = Calendar.getInstance().getTimeInMillis();
                    //terrainNode.addControl(blockTerrain);
                    endTime = Calendar.getInstance().getTimeInMillis();
                    if (endTime - startTime > 2) {
                        System.out.println("Block Finished took " + (endTime - startTime) + "ms half was " + (halfTime - startTime));
                    }
                    return null;
                }
            });
            
        }
    }
    
    public void HandleSetBlock(SetBlock setMessage) {
        blockTerrain.setBlock(setMessage.getBlock(), BlockManager.getBlock((byte)setMessage.getBlockID()));
        // TODO: Move this into terrain
        // and only do it when actually needed (first block? mesh vert count 0?)
        //terrainNode.removeControl(blockTerrain);
        //terrainNode.addControl(blockTerrain);
    }
    
    public void HandleClearBlock(ClearBlock clearMessage) {
        blockTerrain.removeBlock(clearMessage.getBlock());
    }

    void removeNodeFromWorld(Node node) {
        node.removeFromParent();
    }

    void removePhysicsControl(AbstractPhysicsControl control) {
        bulletAppState.getPhysicsSpace().remove(control);
    }
    
    void addPhysicsControl(AbstractPhysicsControl control) {
        bulletAppState.getPhysicsSpace().add(control);
        bulletAppState.setDebugEnabled(true);
    }

    void addNodeToWorld(Node playerNode) {
        terrainNode.attachChild(playerNode);
    }

    void setPrimaryEntity(PlayerEntity player) {
        this.primaryEntity = player;
        app.getGameClient().requestNextChunk(blockTerrain, primaryEntity);
    }
    
    BlockTerrainControl getTerrain() {
        return blockTerrain;
    }

}
