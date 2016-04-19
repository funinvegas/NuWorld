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
public class SetBlock extends AbstractMessage {
    // TODO seperate block loc identification to chunk + block for larger world
    //private Vector3Int chunkLocation;
    //private Vector3Int blockLocation;
    private int blockLocX;
    private int blockLocY;
    private int blockLocZ;
    private int blockID; 
    public SetBlock() {}
    public SetBlock(/*Vector3Int chunkLoc, */Vector3Int blockLoc, int block) {
        //this.chunkLocation = chunkLoc;
        //this.blockLocation = blockLoc;
        blockLocX = blockLoc.getX();
        blockLocY = blockLoc.getY();
        blockLocZ = blockLoc.getZ();
        this.blockID = block;
    }
    //public Vector3Int getChunk() {
    //    return chunkLocation;
    //}
    public Vector3Int getBlock() {
        return Vector3Int.create(blockLocX, blockLocY, blockLocZ);
    }
    public int getBlockID() {
        return blockID;
    }
}
