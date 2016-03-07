/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer.Messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import java.util.HashMap;

/**
 *
 * @author funin_000
 */
@Serializable
public class UpdatePlayerEntities extends AbstractMessage {
//    private HashMap<String, Vector3f> playerLoc;
    private String[] names;
    private Vector3f[] locations;
    public UpdatePlayerEntities() {
        
    }
    public UpdatePlayerEntities(HashMap<String, Vector3f> players) {
        names = new String[players.size()];
        locations = new Vector3f[players.size()];
        int counter = 0;
        for (String i : players.keySet()){
            names[counter] = i;
            locations[counter] = players.get(i);
            ++counter;
        }
        this.setReliable(false);
    }
    public HashMap<String, Vector3f> getPlayerLoc() {
        HashMap<String, Vector3f> map = new HashMap<String, Vector3f>();
        for( int i = 0; i < names.length; ++i) {
            map.put(names[i], locations[i]);
        }
        return map;
    }
}
