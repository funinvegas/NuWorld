/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer.Messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author funin_000
 */
@Serializable
public class ResetChunk extends AbstractMessage {
    private byte [] chunkData;
    public ResetChunk() {}
    public ResetChunk( byte[] rchunkData ) {
        chunkData = rchunkData;
        this.setReliable(true);
    }
    public byte [] getChunkData() {
        return chunkData;
    }
}
