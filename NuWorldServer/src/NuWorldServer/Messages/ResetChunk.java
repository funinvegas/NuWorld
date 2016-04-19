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
    private byte [] chunkData1;
    private byte [] chunkData2;
    private byte [] chunkData3;
    private byte [] chunkData4;
    public ResetChunk() {}
    public ResetChunk( byte[] rchunkData1, byte [] rchunkData2, byte [] rchunkData3, byte [] rchunkData4) {
        chunkData1 = rchunkData1;
        chunkData2 = rchunkData2;
        chunkData3 = rchunkData3;
        chunkData4 = rchunkData4;
        this.setReliable(true);
    }
    public byte [] getChunkData(int n) {
        switch (n) {
            case 0: return chunkData1;
            case 1: return chunkData2;
            case 2: return chunkData3;
            case 3: return chunkData4;
        }
        return null;
    }
}
