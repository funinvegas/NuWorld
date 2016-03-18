/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer.Messages;

import com.cubes.Vector3Int;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author funin_000
 */
@Serializable
public class RequestChunk extends AbstractMessage {
    int x;
    int y;
    int z;
    public RequestChunk() {
        this.setReliable(true);
        x = 0;
        y = 0;
        z = 0;
    }
    public RequestChunk(Vector3Int chunk) {
        this.setReliable(true);
        x = chunk.getX();
        y = chunk.getY();
        z = chunk.getZ();
    }
    public Vector3Int getChunkLoc() {
        return new Vector3Int(x,y,z);
    }
}
