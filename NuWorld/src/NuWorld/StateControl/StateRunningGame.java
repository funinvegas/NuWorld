/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld.StateControl;

import NuWorld.CubeTerrain.CubeAssets;
import NuWorld.CubeTerrain.EntityManager;
import NuWorld.NuWorldMain;
import NuWorld.PlayerController;
import NuWorld.PlayerEntity;
import NuWorldServer.Messages.ClearBlock;
import NuWorldServer.Messages.ResetChunk;
import NuWorldServer.Messages.SetBlock;
import NuWorldServer.Messages.SetPlayerLocation;
import NuWorldServer.Messages.UpdatePlayerEntities;
import com.cubes.BlockManager;
import com.cubes.BlockTerrainControl;
import com.cubes.Vector3Int;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
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
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import strongdk.jme.appstate.console.CommandEvent;
import strongdk.jme.appstate.console.CommandListener;
import strongdk.jme.appstate.console.CommandParser;
import strongdk.jme.appstate.console.ConsoleAppState;

/**
 *
 * @author funin_000
 */
public class StateRunningGame extends AbstractAppState implements ActionListener, MessageListener<Client>, CommandListener {
    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;
    NuWorldMain app;
    
    
    private InputManager inputManager;
    
    // Track the players current walking direction
    private Vector3f walkDirection;

    // Physics controler for the player
    private PlayerController playerControl;
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

        app.addCommandListener(this, "/loc");
        app.addCommandListener(this, "/tp");
        app.addCommandListener(this, "/update");
    }
    
    private PlayerEntity player;
    public static Geometry playerG;
    private String playerName;
    private void initPlayer(String playerName){
        float blockSize = app.getGameSettings().getCubesSettings().getBlockSize();
        //playerControl = new BetterCharacterControl(new CapsuleCollisionShape((cubesSettings.getBlockSize() / 2), cubesSettings.getBlockSize() * 2), 0.05f);
        playerControl = new PlayerController(inputManager, app.getCamera(), blockSize, blockSize / 3, blockSize * 2, 0.05f);
        playerControl.setJumpForce(new Vector3f(0,0.4f * blockSize,0));
        player = new PlayerEntity(playerName); 
        //Node playerNode = player.getNode();
        player.addControl(playerControl);
        //playerNode.addControl(playerControl);
        app.getWorldManager().getEntityManager().addPlayerEntity(player);
        app.getWorldManager().setPrimaryEntity(player);
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
        app.removeCommandListener(this, "/loc");
        app.removeCommandListener(this, "/tp");
        app.removeCommandListener(this, "/update");
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
                app.getGameClient().sendMessage(locationMessage);
            }
        }
        app.getGameClient().requestNextChunk(app.getWorldManager().getTerrain(), player);
        //cam.setLocation(playerLoc);
        Screen screen = nifty.getCurrentScreen();
        Element txt;
        txt = screen.findElementByName("DigPoint");
        if (txt != null) {
            Vector3Int clearBlock = app.getWorldManager().getCurrentPointedBlockLocation(false);
            if (clearBlock != null) {
                txt.getRenderer(TextRenderer.class).setText("clearBlock(" + BlockTerrainControl.keyify(clearBlock) + ")");
            }
            txt = screen.findElementByName("PlacePoint");
            Vector3Int setBlock = app.getWorldManager().getCurrentPointedBlockLocation(true);
            if (setBlock != null) {
                txt.getRenderer(TextRenderer.class).setText("setBlock(" + BlockTerrainControl.keyify(setBlock) + ")");
            }
            //txt.setText("HELLOO..");
        }
    }
    private void initControls(){
        // Setup inputs so player can control their movement
        //inputManager.deleteMapping( SimpleApplication.INPUT_MAPPING_EXIT );
        // TODO: Move this to a keyboard mapping manager so it can be configured in the future
        inputManager.addMapping("move_left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("move_right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("move_up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("move_down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("jump", new KeyTrigger(KeyInput.KEY_SPACE), new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("crouch", new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_C));

        inputManager.addMapping("set_block", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("remove_block", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "set_block");
        inputManager.addListener(this, "remove_block");
    }
    
    @Override
    public void onAction(String actionName, boolean value, float lastTimePerFrame){
        // TODO: figure out how set_block should be organized once we have inventory /tools
        if(actionName.equals("set_block") && value){
            Vector3Int blockLocation = app.getWorldManager().getCurrentPointedBlockLocation(true);
            if(blockLocation != null){
                SetBlock message = new SetBlock(blockLocation, BlockManager.getType(CubeAssets.BLOCK_WOOD));
                System.out.println("Sending SetBlock");
                app.getGameClient().sendMessage(message);
                //blockTerrain.setBlock(blockLocation, CubeAssets.BLOCK_WOOD);
            }
        }
        else if(actionName.equals("remove_block") && value){
            Vector3Int blockLocation = app.getWorldManager().getCurrentPointedBlockLocation(false);
            if(blockLocation != null){
                ClearBlock message = new ClearBlock(blockLocation);
                app.getGameClient().sendMessage(message);
                //blockTerrain.removeBlock(blockLocation);
            }
        }
    }
    
    public void messageReceived(final Client source, final Message message) {

        if (message instanceof ResetChunk) {
            app.getWorldManager().HandleResetChunk((ResetChunk)message); 
        } else {

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
                        app.getWorldManager().enableChunks();
                    } else if (message instanceof SetBlock) {
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
                        System.err.println(messageName + " System.currentTimeMillis() " + (endTime - startTime) + "ms");
                    }
                    return null;
                }
            });
        }
    }

   
    @Override
    public void execute(CommandEvent evt) {
       final CommandParser parser = evt.getParser();
      if (evt.getCommand().equals("/tp")) {
           ConsoleAppState console = app.getConsoleAppState();
           Integer x = parser.getInt(0);
           Integer y = parser.getInt(1);
           Integer z = parser.getInt(2);
           console.appendConsole("Attempting to port player to " + x + ", " + y + ", " + z);
           if (x != null && y != null && z != null) {
               Vector3f vf = new Vector3f(x,y,z);
               ((PlayerController)app.getWorldManager().getEntityManager().getPlayerEntity(playerName).getControl()).warp(vf);
           }
         
       } else if (evt.getCommand().equals("/loc")) {
           ConsoleAppState console = app.getConsoleAppState();
           Vector3f playerLoc = app.getWorldManager().getEntityManager().getPlayerEntity(playerName).getLocation();
           console.appendConsole("Player location is " + playerLoc.toString());
       } else if (evt.getCommand().equals("/update")) {
           app.getGameClient().requestNextChunk(app.getWorldManager().getTerrain(), player);
       }
       
      
    }

      
}
