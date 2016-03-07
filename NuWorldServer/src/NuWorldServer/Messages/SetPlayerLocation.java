/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer.Messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author funin_000
 */
@Serializable
public class SetPlayerLocation extends AbstractMessage {
    private String playerName;
    private Vector3f playerLoc;
    public SetPlayerLocation() {}
    public SetPlayerLocation(String playerName, Vector3f loc) {
        this.playerName = playerName;
        this.playerLoc = loc;
        this.setReliable(true);
    }
    public String getPlayerName() {
        return playerName;
    }
    public Vector3f getPlayerLoc() {
        return playerLoc;
    }
}
