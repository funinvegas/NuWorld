/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import NuWorld.CubeTerrain.CubeAssets;
import com.cubes.CubesSettings;
import com.jme3.app.Application;
import strongdk.jme.appstate.console.ConsoleAppState;

/**
 *
 * @author funin_000
 */
public class GameSingletons {
    private CubesSettings cubeSettings;
    private NuWorldMain app; 
    private ConsoleAppState console;
    GameSingletons(Application ap, ConsoleAppState console) {
        this.app = (NuWorldMain) ap;
        this.cubeSettings = CubeAssets.getSettings(this.app);
        this.console = console;
    }
    
    public CubesSettings getCubesSettings() {
        return cubeSettings;
    }
    public void logToConsole(String text) {
        console.appendConsole(text);
        System.out.println("ConsoleText: " + text);
    }
    public void errorToConsole(String text) {
        console.appendConsoleError(text);
        System.err.println("ConsoleError: " + text);
    }
    
}
