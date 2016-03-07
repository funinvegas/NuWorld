package NuWorld;

import NuWorldServer.Messages.SetupMessages;
import com.cubes.BlockChunkControl;
import com.cubes.BlockChunkListener;
import com.cubes.BlockNavigator;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.MessageListener;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.awt.Font;
import strongdk.jme.appstate.console.CommandEvent;
import strongdk.jme.appstate.console.CommandListener;
import strongdk.jme.appstate.console.CommandParser;
import strongdk.jme.appstate.console.ConsoleAppState;
import strongdk.jme.appstate.console.ConsoleDefaultCommandsAppState;

/**
 * Main
 * @author BigScorch
 * implements ActionListener so it can listen to input events
 */
public class NuWorldMain extends SimpleApplication implements ScreenController, CommandListener  {
    
    // State manager to handle transition from start screen to in-game and others in the future
    //private AppStateManager stateManager;

    // GUI management
    NiftyJmeDisplay niftyDisplay;
    
    // GUI managment core
    Nifty nifty;
    
    boolean invertYAxis = false;
        
    GameClient gameClient;
    
    // Node for player
    private Node playerNode;
    
    // Global starting point
    public static void main(String[] args) {
        NuWorldMain app = new NuWorldMain();
        app.start();
    }
    
    /*public boolean getInvertYAxis() {
        return this.invertYAxis;
    }*/
    
    // "The" world
    // can be reset between game instances
    private WorldManager worldManager;
    private GameSettings gameSettings;
    
    public WorldManager getWorldManager() {
        return worldManager;
    }
    
    public void bind(Nifty nifty, Screen screen)
    {
        System.out.print("Main bind");
    }

    
    public void onStartScreen()
    {
        System.out.print("Main OnStartScreen");
    }

    public void onEndScreen()
    {
        System.out.print("Main OnEndScreen");
    }
    
    // Constructor
    NuWorldMain() {
        // Set some defaults for the jMonkeyEngine settings dialog
        settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("NuWorld");
        settings.setFrameRate(60);
        this.invertYAxis = false;
        SetupMessages.RegisterAllMessageTypes();

    }
    
    public Node getRootNode() {
        return rootNode;
    }
    public AppSettings getSettings() {
        return settings;
    }
    
    // Console control for entering commands
    private ConsoleAppState console;

    //private CommandListener commandListener = new CommandListener() {
            
     //   };
    
    @Override
    public void execute(CommandEvent evt) {
       final CommandParser parser = evt.getParser();
       if (evt.getCommand().equals("/invertY")) {
            Integer value = parser.getInt(0);
            if (value != null) {
                if (value != 0) {
                    chaseCam.setInvertVerticalAxis(true);
                } else {
                    chaseCam.setInvertVerticalAxis(false);
                }
                chaseCam.setMaxDistance(0.3f * 3f);
                chaseCam.setMinDistance(0.3f * 3f);
                chaseCam.setLookAtOffset(new Vector3f(0, 1.5f * 3f, 0));
                chaseCam.setDragToRotate(false);

            } else {
                console.appendConsoleError("Who are you? " + parser.getString(0));
            }
             /*String value = parser.getString(0);
             if(value != null){
                   console.appendConsole("Hi "+value);
             }else{
                   console.appendConsoleError("Who are you? " + parser.getString(0));
             }*/
             //this.invertYAxis = !this.invertYAxis;
       }
    }
    private ChaseCamera chaseCam;
    private void initGUI(){
        
        niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
        nifty = niftyDisplay.getNifty();
        nifty.fromXml("Interface/startMenu.xml", "start", this);
        nifty.addXml("Interface/inGameMenu.xml");
        nifty.addXml("Interface/connectMenu.xml");
        nifty.gotoScreen("StartScreen");
        //Element niftyElement = nifty.getCurrentScreen().findElementByName("quitGame");
        //nifty.addXml("Interface/inventory.xml");
        guiViewPort.addProcessor(niftyDisplay);  //    assetManager, inputManager, audioRenderer, guiViewPort);
        flyCam.setDragToRotate(true);
        nifty.subscribeAnnotations(this);

        flyCam.setEnabled(false);
        chaseCam = new ChaseCamera(cam, inputManager);
        
    }
    public BitmapFont getGuiFont() {
        return guiFont;
    }
    private void initConsole() {
        console = new ConsoleAppState();
        stateManager.attach(console);
        stateManager.attach(new ConsoleDefaultCommandsAppState());
        console.registerCommand("/invertY", this);
        console.appendConsole("You can change speed by using the command 'rotation [1-10]'");
        console.appendConsole("Example: rotation 5");

        gameClient = new GameClient(console);

        // TODO how to draw text on the screen!
        //BitmapText bt = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        //bt.setColor(ColorRGBA.Black);
        //bt.setText("This is an example of a game that uses ConsleAppState\nUse the grave key (button next to 1) to toggle the console on and off");
        //bt.setLocalTranslation(0, bt.getLineHeight() *4,0);
        //bt.setBox(new Rectangle(0, 0, guiViewPort.getCamera().getWidth(), bt.getLineHeight()));
        //bt.setAlignment(BitmapFont.Align.Center);
        //guiNode.attachChild(bt);
    }


    @Override
    public void simpleInitApp() {
        initConsole();
        initGUI();
        StateStartMenu startScreenState = new StateStartMenu();
        stateManager.attach(startScreenState);
        gameSettings = new GameSettings(this);
        worldManager = new WorldManager(stateManager, this);

        /*Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        rootNode.attachChild(geom);
        */
    }

    public ChaseCamera getChaseCamera() {
        return chaseCam;
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    // Allow the cursor to move around a menu
    public void enableCursor() {
        inputManager.setCursorVisible(true);
    }

    // Allow the cursor to move around a menu
    public void hideCursor() {
        inputManager.setCursorVisible(false);
        flyCam.setDragToRotate(false);
    }
    public NiftyJmeDisplay getNiftyDisplay() {
        return this.niftyDisplay;
    }
    
    public void startNewGame(AbstractAppState leavingState ) {
        StateRunningGame stateRunning = new StateRunningGame();
        stateManager.detach(leavingState);
        stateManager.attach(stateRunning);
    }
    
    public void joinGame(AbstractAppState leavingState) {
        StateConnecting stateConnecting = new StateConnecting();
        stateManager.detach(leavingState);
        stateManager.attach(stateConnecting);
        
    }
    String serverIP;
    public void setServerConnection(String serverConnectionString) {
        serverIP = serverConnectionString;
        gameClient.connect(serverIP);
    }
    public void connectToServer() {
        gameClient.start();
    }
    public void startRunning(AbstractAppState leavingState) {
        StateRunningGame stateRunning = new StateRunningGame();
        stateManager.detach(leavingState);
        stateManager.attach(stateRunning);
    }
    
    public void removeMessageListener(MessageListener<Client> clientListener) {
        gameClient.removeMessageListener(clientListener);        
    }
    
    public void addMessageListener(MessageListener<Client> clientListener) {
        gameClient.addMessageListener(clientListener);
    }

    GameClient getGameClient() {
        return this.gameClient;
    }

    GameSettings getGameSettings() {
        return gameSettings;
    }
}
