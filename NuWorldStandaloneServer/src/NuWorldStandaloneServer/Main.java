package NuWorldStandaloneServer;

import NuWorldServer.NuWorldServer;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import strongdk.jme.appstate.console.CommandEvent;
import strongdk.jme.appstate.console.CommandListener;
import strongdk.jme.appstate.console.CommandParser;
import strongdk.jme.appstate.console.ConsoleAppState;
import strongdk.jme.appstate.console.ConsoleDefaultCommandsAppState;

/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication implements CommandListener {

    public static void main(String[] args) {
        Main app = new Main();
//       app.start(JmeContext.Type.Headless);
        app.setPauseOnLostFocus(false);
        app.setShowSettings(false);
        app.start();
    }
    private ConsoleAppState console;

    public Main() {
        settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("NuWorld Dedicated Server");
        settings.setFrameRate(10);
    }
    @Override
    public void execute(CommandEvent evt) {
          final CommandParser parser = evt.getParser();
          if (evt.getCommand().equals("rotation")) {
                Integer value = parser.getInt(0);
                if(value != null){
                      console.appendConsole("Rotation speed changed: "+value);
                }else{
                      console.appendConsoleError("Could not change speed, not a valid number: " + parser.getString(0));
                }
          }
    }
    
    NuWorldServer server;
    
    @Override
    public void simpleInitApp() {
        console = new ConsoleAppState();
        stateManager.attach(console);
        stateManager.attach(new ConsoleDefaultCommandsAppState());
        console.registerCommand("rotation", this);
        console.appendConsole("You can change speed by using the command 'rotation [1-10]'");
        console.appendConsole("Example: rotation 5");
        console.setConsoleUsesFullViewPort(true);
        console.setVisible(true);
       /* BitmapText bt = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        bt.setColor(ColorRGBA.Black);
        bt.setText("This is an example of a game that uses ConsleAppState\nUse the grave key (button next to 1) to toggle the console on and off");
        bt.setLocalTranslation(0, bt.getLineHeight() *4,0);
        bt.setBox(new Rectangle(0, 0, guiViewPort.getCamera().getWidth(), bt.getLineHeight()));
        bt.setAlignment(BitmapFont.Align.Center);
        guiNode.attachChild(bt);
        */
        flyCam.setDragToRotate(true);
        
        server = new NuWorldServer(console);
        server.Start();
        console.appendConsole("Ready");
    }
    

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
