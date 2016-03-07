/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer;

import com.jme3.math.Vector3f;

/**
 *
 * @author funin_000
 */
public class ServerEntity {
    public int clientID;
    public String name;
    public Vector3f location;

    ServerEntity(String name, int id, Vector3f playerLoc) {
        this.clientID = id;
        this.name = name;
        this.location = playerLoc;
    }

    public String getName() {
        return name;
    }

    public Vector3f getLocation() {
        return location;
    }

    void setLocation(Vector3f playerLoc) {
        location = playerLoc;
    }
}
