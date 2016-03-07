/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import NuWorldServer.Messages.ClearBlock;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetBlock;
import NuWorldServer.Messages.SetPlayerLocation;
import NuWorldServer.Messages.UpdatePlayerEntities;
import com.cubes.BlockManager;
import com.cubes.Vector3Int;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * @author funin_000
 */
public class StateRunningGame extends AbstractAppState implements ActionListener, MessageListener<Client> {
    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;
    NuWorldMain app;
    
    // Track the up/down state of the direction inputs
    private boolean[] arrowKeys = new boolean[4];
    
    private InputManager inputManager;
    
    // Track the players current walking direction
    private Vector3f walkDirection;

    // Physics controler for the player
    private BetterCharacterControl playerControl;
    private ChaseCamera cam;
    PlayerEntity playerEntity;
    
    @Override
    public void initialize(AppStateManager stateManager, Application ap) {
        this.app = (NuWorldMain)ap;
        this.cam = this.app.getChaseCamera();
        this.inputManager = app.getInputManager();
        walkDirection = new Vector3f();
        initControls();
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
    
    private PlayerEntity player;
    public static Geometry playerG;
    private String playerName;
    private void initPlayer(String playerName){
        float blockSize = app.getGameSettings().getCubesSettings().getBlockSize();
        //playerControl = new BetterCharacterControl(new CapsuleCollisionShape((cubesSettings.getBlockSize() / 2), cubesSettings.getBlockSize() * 2), 0.05f);
        playerControl = new BetterCharacterControl(blockSize / 2, blockSize * 2, 0.05f);
        playerControl.setJumpForce(new Vector3f(0,0.4f * blockSize,0));
        player = new PlayerEntity(playerName); 
        //Node playerNode = player.getNode();
        player.addControl(playerControl);
        //playerNode.addControl(playerControl);
        app.getWorldManager().getEntityManager().addPlayerEntity(player);
        playerControl.warp(new Vector3f(5, 35, 5).mult(blockSize));

        player.getNode().addControl(cam);
        
        Cylinder cylinder;
        cylinder = new Cylinder(2, 12, blockSize / 2, blockSize * 2);
        Geometry geom = new Geometry("Playercylinder", cylinder);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        geom.rotate(3.14f/2,0,0);
        geom.move(0,blockSize,0);
        playerG = geom;
        player.getNode().attachChild(geom);

        
        //cam = new ChaseCamera(app.getCamera(), playerNode, inputManager);
        cam.setMaxDistance(3.4f * blockSize);
        cam.setMinDistance(3.4f * blockSize);
        cam.setLookAtOffset(new Vector3f(0, 1.5f * blockSize, 0));
        cam.setDragToRotate(false);
        cam.setInvertVerticalAxis(true);
    }
    
    @Override
    public void cleanup()
    {
        super.cleanup();
        inputManager.removeListener(this);
        app.removeMessageListener(this);
    }
    
    long lastPlayerUpdate = 0;

    @Override
    public void update(float tpf)
    {
        long updateTime = Calendar.getInstance().getTimeInMillis();
        if (updateTime - lastPlayerUpdate > 1000) {
            lastPlayerUpdate = updateTime;
            if (playerName != null) {
                SetPlayerLocation locationMessage = new SetPlayerLocation(playerName, player.getLocation());
                app.gameClient.sendMessage(locationMessage);
            }
        }
        float blockSize = app.getGameSettings().getCubesSettings().getBlockSize();
        float playerMoveSpeed = ((blockSize * 360.5f) * tpf);
        Vector3f camDir = app.getCamera().getDirection().mult(playerMoveSpeed);
        Vector3f camLeft = app.getCamera().getLeft().mult(playerMoveSpeed);
        walkDirection.set(0, 0, 0);
        if(arrowKeys[0]){ walkDirection.addLocal(camDir); }
        if(arrowKeys[1]){ walkDirection.addLocal(camLeft.negate()); }
        if(arrowKeys[2]){ walkDirection.addLocal(camDir.negate()); }
        if(arrowKeys[3]){ walkDirection.addLocal(camLeft); }
        walkDirection.setY(0);
        if (playerControl != null) {
            playerControl.setWalkDirection(walkDirection);
            Vector3f playerLoc = player.getNode().getWorldTranslation();
            playerLoc.setY(playerLoc.getY() + blockSize * 1.5f);
            playerLoc = playerLoc.add(camDir.normalize().mult(blockSize * -0.5f));
        }
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
            Vector3Int blockLocation = app.getWorldManager().getCurrentPointedBlockLocation(true);
            if(blockLocation != null){
                SetBlock message = new SetBlock(blockLocation, BlockManager.getType(CubeAssets.BLOCK_WOOD));
                app.getGameClient().sendMessage(message);
                //blockTerrain.setBlock(blockLocation, CubeAssets.BLOCK_WOOD);
            }
        }
        else if(actionName.equals("remove_block") && value){
            Vector3Int blockLocation = app.getWorldManager().getCurrentPointedBlockLocation(false);
            if((blockLocation != null) && (blockLocation.getY() > 0)){
                ClearBlock message = new ClearBlock(blockLocation);
                app.getGameClient().sendMessage(message);
                //blockTerrain.removeBlock(blockLocation);
            }
        }
    }
    
    public void messageReceived(final Client source, final Message message) {
        
        this.app.enqueue(new Callable() {
            public Object call() throws Exception {
                long startTime = Calendar.getInstance().getTimeInMillis();
                long endTime;
                String messageName = "unset";
                if (message instanceof SetPlayerLocation) {
                    messageName = "SetPlayerLocation";
                     // do something with the message
                    SetPlayerLocation playerLocMessage = (SetPlayerLocation) message;
                    playerName = playerLocMessage.getPlayerName();
                    initPlayer(playerName);
                    System.out.println("Client received '" +playerLocMessage.getPlayerLoc().toString() +"' from host #"+source.getId() );
                    playerControl.warp(playerLocMessage.getPlayerLoc());
                } else if (message instanceof ResetChunk) {
                    messageName = "ResetChunk";
                    app.getWorldManager().HandleResetChunk((ResetChunk)message); 
                } 
                else if (message instanceof SetBlock) {
                    messageName = "SetBlock";
                    app.getWorldManager().HandleSetBlock((SetBlock)message);
                } 
                else if (message instanceof ClearBlock) {
                    messageName = "ClearBlock";
                    app.getWorldManager().HandleClearBlock((ClearBlock)message);
                }
                else if (message instanceof UpdatePlayerEntities) {
                    messageName = "UpdatePlayerEntities";
                    UpdatePlayerEntities updateMessage = (UpdatePlayerEntities)message;
                    HashMap<String, Vector3f> entities = updateMessage.getPlayerLoc();
                    EntityManager entityManager = app.getWorldManager().getEntityManager();
                    Set<String> nameSet = entities.keySet();
                    entityManager.prunePlayers(nameSet);
                    for (String i : nameSet) {
                        if (!i.equals(playerName)) {
                            PlayerEntity otherPlayer = entityManager.getPlayerEntity(i);
                            if (otherPlayer == null) {
                                otherPlayer = new PlayerEntity(i);
                                app.getWorldManager().getEntityManager().addPlayerEntity(otherPlayer);
    
                                Cylinder cylinder;
                                float blockSize = app.getGameSettings().getCubesSettings().getBlockSize();
                                cylinder = new Cylinder(2, 12, blockSize / 2, blockSize * 2);
                                Geometry geom = new Geometry("Playercylinder", cylinder);

                                Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                                mat.setColor("Color", ColorRGBA.Blue);
                                geom.setMaterial(mat);

                                geom.rotate(3.14f/2,0,0);
                                geom.move(0,blockSize,0);
                                otherPlayer.getNode().attachChild(geom);
                            }
                            otherPlayer.getNode().setLocalTranslation(entities.get(i));
                           /*
                            player = new PlayerEntity("Player"); 
                            //Node playerNode = player.getNode();
                            player.addControl(playerControl);
                            //playerNode.addControl(playerControl);
                            app.getWorldManager().getEntityManager().addPlayerEntity(player);
                            playerControl.warp(new Vector3f(5, 35, 5).mult(blockSize));

                            player.getNode().addControl(cam);
*/
    

                                    
                                    
                                    
                       }
                    }
                }
                endTime = Calendar.getInstance().getTimeInMillis();
                if (endTime - startTime > 16) {
                    System.err.println(messageName + " took " + (endTime - startTime) + "ms");
                }
                return null;
            }

        });
    }
}
