/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorldServer.Messages;

import com.jme3.network.serializing.Serializer;

/**
 *
 * @author funin_000
 */
public class SetupMessages {
    public static void RegisterAllMessageTypes() {
        Serializer.registerClass(ClearBlock.class);
        Serializer.registerClass(ResetChunk.class);
        Serializer.registerClass(SetBlock.class);
        Serializer.registerClass(SetPlayerLocation.class);
        Serializer.registerClass(UpdatePlayerEntities.class);
    }
}
