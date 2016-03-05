/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import NuWorldServer.Messages.ClearBlock;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetBlock;
import NuWorldServer.Messages.SetPlayerLocation;
import com.cubes.BlockChunkControl;
import com.cubes.BlockChunkListener;
import com.cubes.BlockManager;
import com.cubes.BlockNavigator;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;
import com.cubes.network.BitInputStream;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 *
 * @author funin_000
 */
public class StateRunningGame extends AbstractAppState implements ActionListener, MessageListener<Client> {
    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;
    NuWorldMain app;
    
    // Physics Engine
    private BulletAppState bulletAppState;
    
    // Track the up/down state of the direction inputs
    private boolean[] arrowKeys = new boolean[4];

    // Physics controler for the player
    private BetterCharacterControl playerControl;
    
    // Node for player
    private Node playerNode = new Node("Player Node");
    
    // Configuration for cube terrain
    private CubesSettings cubesSettings;

    // THE cube terrain
    private BlockTerrainControl blockTerrain;
    
    // JMonkey node to hold the terrain aka root
    private Node terrainNode = new Node("Cube Terrain");

    // Starting? Terrain size?
    private final Vector3Int TERRAIN_SIZE = new Vector3Int(100, 30, 100);

    // Track the players current walking direction
    private Vector3f walkDirection = new Vector3f();

    private ChaseCamera cam;
    
    private InputManager inputManager;
    
    @Override
    public void initialize(AppStateManager stateManager, Application ap) {
        this.app = (NuWorldMain)ap;
        this.cam = this.app.getChaseCamera();
        this.inputManager = app.getInputManager();
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        initControls();
        initBlockTerrain();
        initPlayer();
        initGui();
        app.connectToServer();
        app.addMessageListener(this);
        //cam.lookAtDirection(new Vector3f(1, 0, 1), Vector3f.UNIT_Y);
    }
    
    void initGui () {
        //Crosshair
        BitmapFont guiFont = this.app.getGuiFont();
        BitmapText crosshair = new BitmapText(guiFont);
        crosshair.setText("+");
        crosshair.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        AppSettings settings = app.getSettings();
        crosshair.setLocalTranslation(
                (settings.getWidth() / 2) - (guiFont.getCharSet().getRenderedSize() / 3 * 2),
                (settings.getHeight() / 2) + (crosshair.getLineHeight() / 2), 0);
        Node guiNode = app.getGuiNode();
        guiNode.attachChild(crosshair);

        this.niftyDisplay = app.getNiftyDisplay();
        this.nifty = this.niftyDisplay.getNifty();
        nifty.gotoScreen("InGameScreen");
        nifty.setIgnoreKeyboardEvents(true);
        nifty.setIgnoreMouseEvents(true);
        app.hideCursor();

        
    }
    
    @Override
    public void cleanup()
    {
        super.cleanup();
        inputManager.removeListener(this);
        app.removeMessageListener(this);
    }
    
    private void initPlayer(){
        //playerControl = new BetterCharacterControl(new CapsuleCollisionShape((cubesSettings.getBlockSize() / 2), cubesSettings.getBlockSize() * 2), 0.05f);
        playerControl = new BetterCharacterControl(cubesSettings.getBlockSize() / 2, cubesSettings.getBlockSize() * 2, 0.05f);
        playerControl.setJumpForce(new Vector3f(0,0.4f * cubesSettings.getBlockSize(),0));
        //playerControl.setGravity(new Vector3f(0,0.0f * cubesSettings.getBlockSize(),0));
        playerNode.addControl(playerControl);
        terrainNode.attachChild(playerNode);
        bulletAppState.getPhysicsSpace().add(playerControl);
        bulletAppState.setDebugEnabled(false);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,-19.8f * cubesSettings.getBlockSize(),0));
        //bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,0.5f * cubesSettings.getBlockSize(),0));
        playerControl.warp(new Vector3f(5, TERRAIN_SIZE.getY() + 5, 5).mult(cubesSettings.getBlockSize()));

        playerNode.addControl(cam);
        //cam = new ChaseCamera(app.getCamera(), playerNode, inputManager);
        cam.setMaxDistance(3.4f * cubesSettings.getBlockSize());
        cam.setMinDistance(3.4f * cubesSettings.getBlockSize());
        cam.setLookAtOffset(new Vector3f(0, 1.5f * cubesSettings.getBlockSize(), 0));
        cam.setDragToRotate(false);
        cam.setInvertVerticalAxis(true);
    }

    @Override
    public void update(float tpf)
    {
        float playerMoveSpeed = ((cubesSettings.getBlockSize() * 360.5f) * tpf);
        Vector3f camDir = app.getCamera().getDirection().mult(playerMoveSpeed);
        Vector3f camLeft = app.getCamera().getLeft().mult(playerMoveSpeed);
        walkDirection.set(0, 0, 0);
        if(arrowKeys[0]){ walkDirection.addLocal(camDir); }
        if(arrowKeys[1]){ walkDirection.addLocal(camLeft.negate()); }
        if(arrowKeys[2]){ walkDirection.addLocal(camDir.negate()); }
        if(arrowKeys[3]){ walkDirection.addLocal(camLeft); }
        walkDirection.setY(0);
        playerControl.setWalkDirection(walkDirection);
        Vector3f playerLoc = playerNode.getWorldTranslation();
        playerLoc.setY(playerLoc.getY() + cubesSettings.getBlockSize() * 1.5f);
        playerLoc = playerLoc.add(camDir.normalize().mult(cubesSettings.getBlockSize() * -0.5f));
        //cam.setLocation(playerLoc);
    }
    private void initControls(){
        // Setup inputs so player can control their movement
        //inputManager.deleteMapping( SimpleApplication.INPUT_MAPPING_EXIT );
        inputManager.addMapping("move_left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addListener(this, "move_left");
        inputManager.addMapping("move_right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener(this, "move_right");
        inputManager.addMapping("move_up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addListener(this, "move_up");
        inputManager.addMapping("move_down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, "move_down");
        inputManager.addMapping("jump", new KeyTrigger(KeyInput.KEY_SPACE), new KeyTrigger(KeyInput.KEY_J));
        inputManager.addListener(this, "jump");
        inputManager.addMapping("set_block", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(this, "set_block");
        inputManager.addMapping("remove_block", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "remove_block");
    }
    
    @Override
    public void onAction(String actionName, boolean value, float lastTimePerFrame){
        if(actionName.equals("move_up")){
            arrowKeys[0] = value; // TODO Magic number directions
        }
        else if(actionName.equals("move_right")){
            arrowKeys[1] = value;
        }
        else if(actionName.equals("move_left")){
            arrowKeys[3] = value;
        }
        else if(actionName.equals("move_down")){
            arrowKeys[2] = value;
        }
        else if(actionName.equals("jump")){
            if (value) {
                playerControl.jump();
            }
        }
        else if(actionName.equals("set_block") && value){
            Vector3Int blockLocation = getCurrentPointedBlockLocation(true);
            if(blockLocation != null){
                SetBlock message = new SetBlock(blockLocation, BlockManager.getType(CubeAssets.BLOCK_WOOD));
                app.getGameClient().sendMessage(message);
                //blockTerrain.setBlock(blockLocation, CubeAssets.BLOCK_WOOD);
            }
        }
        else if(actionName.equals("remove_block") && value){
            Vector3Int blockLocation = getCurrentPointedBlockLocation(false);
            if((blockLocation != null) && (blockLocation.getY() > 0)){
                ClearBlock message = new ClearBlock(blockLocation);
                app.getGameClient().sendMessage(message);
                //blockTerrain.removeBlock(blockLocation);
            }
        }
    }
    
    private Vector3Int getCurrentPointedBlockLocation(boolean getNeighborLocation){
        CollisionResults results = getRayCastingResults(terrainNode);
        if(results.size() > 0){
            Vector3f collisionContactPoint = results.getClosestCollision().getContactPoint();
            return BlockNavigator.getPointedBlockLocation(blockTerrain, collisionContactPoint, getNeighborLocation);
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
    
    private void initBlockTerrain(){
        CubeAssets.registerBlocks();
        CubeAssets.initializeEnvironment(this.app);
        
        cubesSettings = CubeAssets.getSettings(this.app);
        blockTerrain = new BlockTerrainControl(cubesSettings, new Vector3Int(7, 1, 7));

        
                //To set a block, just specify the location and the block object
        //(Existing blocks will be replaced)
        blockTerrain.setBlock(new Vector3Int(0, 0, 0), CubeAssets.BLOCK_WOOD);
        blockTerrain.setBlock(new Vector3Int(0, 0, 1), CubeAssets.BLOCK_WOOD);
        blockTerrain.setBlock(new Vector3Int(1, 0, 0), CubeAssets.BLOCK_WOOD);
        blockTerrain.setBlock(new Vector3Int(1, 0, 1), CubeAssets.BLOCK_STONE);
        blockTerrain.setBlock(0, 0, 0, CubeAssets.BLOCK_GRASS); //For the lazy users :P

        
        //blockTerrain.setBlocksFromNoise(new Vector3Int(), TERRAIN_SIZE, 0.8f, CubeAssets.BLOCK_GRASS);
        blockTerrain.addChunkListener(new BlockChunkListener(){
            @Override
            public void onSpatialUpdated(BlockChunkControl blockChunk){
                Geometry optimizedGeometry = blockChunk.getOptimizedGeometry_Opaque();
                RigidBodyControl rigidBodyControl = optimizedGeometry.getControl(RigidBodyControl.class);
                if (rigidBodyControl != null) {
                    optimizedGeometry.removeControl(rigidBodyControl);
                    bulletAppState.getPhysicsSpace().remove(rigidBodyControl);
                }
                //if(rigidBodyControl == null){
                rigidBodyControl = new RigidBodyControl(0);
                optimizedGeometry.addControl(rigidBodyControl);
                bulletAppState.getPhysicsSpace().add(rigidBodyControl);
                //}
                rigidBodyControl.setCollisionShape(new MeshCollisionShape(optimizedGeometry.getMesh()));
                //System.err.println("SpatialUpdated terrain is at " + terrainNode.getWorldTranslation().toString());
                //System.err.println("SpatialUpdated player is at " + playerNode.getWorldTranslation().toString());
                //playerControl.warp(new Vector3f(0,0,0));
            }
        });
        terrainNode.addControl(blockTerrain);
        terrainNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        this.app.getRootNode().attachChild(terrainNode);
    }
    
    public void messageReceived(final Client source, final Message message) {
        
        this.app.enqueue(new Callable() {
            public Object call() throws Exception {
                if (message instanceof SetPlayerLocation) {
                  // do something with the message
                  SetPlayerLocation playerLocMessage = (SetPlayerLocation) message;
                  System.out.println("Client received '" +playerLocMessage.getPlayerLoc().toString() +"' from host #"+source.getId() );
                  playerControl.warp(playerLocMessage.getPlayerLoc());
                } else if (message instanceof ResetChunk) {
                    ResetChunk resetChunk = (ResetChunk) message;
                    System.out.println("Client received '" +resetChunk.getChunkData().length +"' from host #"+source.getId() );
                    BitInputStream bitInputStream = new BitInputStream(new ByteArrayInputStream(resetChunk.getChunkData()));
                    try {
                        blockTerrain.readChunkPartial(bitInputStream);
                    } catch(IOException ex){
                        ex.printStackTrace();
                    }
                    terrainNode.removeControl(blockTerrain);
                    terrainNode.addControl(blockTerrain);
                } 
                else if (message instanceof SetBlock) {
                    SetBlock setMessage = (SetBlock)message;                    
                    blockTerrain.setBlock(setMessage.getBlock(), BlockManager.getBlock((byte)setMessage.getBlockID()));
                } 
                else if (message instanceof ClearBlock) {
                    ClearBlock clearMessage = (ClearBlock)message;
                    blockTerrain.removeBlock(clearMessage.getBlock());
                }

                return null;
            }

        });
    }
}
