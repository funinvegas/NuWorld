/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.HashMap;

/**
 *
 * @author funin_000
 */
public class EntityManager {
    private HashMap<String, PlayerEntity> playerEntities = new HashMap<String, PlayerEntity>();
    private WorldManager worldManager;
    public EntityManager(WorldManager world) {
        this.worldManager = world;
    }
    public void addPlayerEntity(PlayerEntity player) {
        if (playerEntities.containsKey(player.getName())) {
            throw new UnsupportedOperationException("Player already exists in entity table");
        }
        playerEntities.put(player.getName(), player);
        Node playerNode = player.getNode();
        worldManager.addNodeToWorld(playerNode);
        worldManager.addPhysicsControl(player.getControl());
    }

    void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
