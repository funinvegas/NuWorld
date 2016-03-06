/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import com.cubes.CubesSettings;
import com.jme3.app.Application;

/**
 *
 * @author funin_000
 */
public class GameSettings {
    private CubesSettings cubeSettings;
    private NuWorldMain app; 
    GameSettings(Application ap) {
        this.app = (NuWorldMain) ap;
        this.cubeSettings = CubeAssets.getSettings(this.app);
    }
    
    public CubesSettings getCubesSettings() {
        return cubeSettings;
    }
}
