package terrain;

import NuWorldServer.CubeAssets;
import com.cubes.Block;
import com.cubes.BlockChunkControl;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;

/**
 *
 * @author Perry
 */
public class FastStart implements TerrainGenerator
{
    private BlockTerrainControl terrainControl;
    private Vector3Int size;
    private Vector3Int offset;
    public FastStart(BlockTerrainControl terrainControl)
    {
        this.terrainControl = terrainControl;
    }

    @Override
    public void generateTerrain(Vector3Int offset, CubesSettings cubeSettings, BlockTerrainControl terrain) {
        this.size = cubeSettings.getChunkViewDistance();
        this.offset = offset;
        for (int x = 0 + offset.getX(); x < offset.getX() + size.getX(); x++)
        {
            for (int z = 0+offset.getZ(); z < offset.getZ() + size.getZ(); z++)
            {
                Vector3Int chunkLocation = new Vector3Int(x, 0, z);
                BlockChunkControl c = generateChunk(terrain, chunkLocation, cubeSettings);
                terrainControl.setChunk(chunkLocation, c);
            }
        }
        //terrainControl.addAllScheduledChunks();
    }

    @Override
    public BlockChunkControl generateChunk(BlockTerrainControl terrain, Vector3Int chunkLocation, CubesSettings settings) {
        /*Block block = Blocks.COAL_BLOCK;
        if(location.z %2 == 0)
        {
            if(location.x % 2 == 0) block = Blocks.BIRCHLOG;
        }
        else
        {
            if(location.x % 2 == 1) block = Blocks.BIRCHLOG;
        }*/
        Block block = CubeAssets.BLOCK_GRASS;

        BlockChunkControl chunk = new BlockChunkControl(terrain, chunkLocation);
        for (int x = 0; x < settings.getChunkSizeX(); x++)
        {
            for (int z = 0; z < settings.getChunkSizeZ(); z++)
            {
                chunk.setBlock(new Vector3Int(x, 0, z), block);
            }
        }
        return chunk;
    }


}
