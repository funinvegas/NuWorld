/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer;

import NuWorldServer.Messages.UpdatePlayerEntities;
import com.jme3.math.Vector3f;
import java.util.HashMap;

/**
 *
 * @author funin_000
 */
public class ServerEntities {
    
    HashMap<Integer, ServerEntity> playerEntities;
    ServerEntities() {
        playerEntities = new HashMap<Integer, ServerEntity>();
    }
    void RegisterPlayerEntity(int clientID, ServerEntity entity) {
        playerEntities.put(clientID, entity);
    }
    public UpdatePlayerEntities buildUpdatePlayersMessage() {
        HashMap<String, Vector3f> playerLoc = new HashMap<String, Vector3f>();
        for(Integer i : playerEntities.keySet()) {
            playerLoc.put(playerEntities.get(i).getName(), playerEntities.get(i).getLocation());
        }
        UpdatePlayerEntities message = new UpdatePlayerEntities(playerLoc);
        return message;
    }

    void setPlayerLocation(int id, Vector3f playerLoc) {
        if (playerEntities.containsKey(id)) {
            playerEntities.get(id).setLocation(playerLoc);
        } else {
            playerEntities.put(id, new ServerEntity("" + id, id, playerLoc));
        }
    }

    void cleanConnection(int id) {
        playerEntities.remove(id);
    }
}
