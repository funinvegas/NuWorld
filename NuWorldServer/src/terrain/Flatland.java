/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package terrain;

//import Properties.GameProperties;
//import com.chappelle.jcraft.blocks.Blocks;
//import com.chappelle.jcraft.blocks.JBlock;
//import com.cubes.BTControl;
import NuWorldServer.CubeAssets;
import com.cubes.Block;
import com.cubes.BlockTerrainControl;
import com.cubes.BlockChunkControl;
import com.cubes.Vector3Int;
import com.cubes.CubesSettings;
import java.util.Random;

/**
 *
 * @author Perry
 */
public class Flatland implements TerrainGenerator
{
    private BlockTerrainControl terrainControl;
    private Random rndm;
    private Vector3Int offset;

    public Flatland(BlockTerrainControl terrainControl)
    {
        this.terrainControl = terrainControl;
    }

    public void generateTerrain(Vector3Int offset, CubesSettings cubeSettings, BlockTerrainControl terrain)
    {
        this.offset = offset;
        long seed = cubeSettings.getSeed();
        if(seed == -1)
        {
            rndm = new Random(System.currentTimeMillis());
            rndm.nextLong();
            seed = rndm.nextLong();
        }
        System.out.print("Generating Terrain ");
        Vector3Int chunkViewDistance = cubeSettings.getChunkViewDistance();
        System.out.print(chunkViewDistance);
        for (int x = 0 + offset.getX(); x < offset.getX() + chunkViewDistance.getX(); x++)
        {
            for (int z = 0 + offset.getZ(); z < offset.getZ() + chunkViewDistance.getZ(); z++)
            {
                System.out.print(".");
                Vector3Int chunkLocation = new Vector3Int(x, 0, z);
                BlockChunkControl c = generateChunk(terrain, chunkLocation, cubeSettings);
                terrainControl.setChunk(chunkLocation, c);
            }
        }
        System.out.println();
       // terrainControl.addAllScheduledChunks();
    }

    /*
    @Override
    public Vector3Int getPlayerStart()
    {
        Vector3Int result = GameProperties.CHUNK_SIZE.mult(GameProperties.CHUNK_VIEW_DISTANCE).divide(2);
        result.addLocal(offset.mult(GameProperties.CHUNK_SIZE));
        int y = GameProperties.CHUNK_SIZE.y * GameProperties.CHUNK_VIEW_DISTANCE.y;
        while(terrainControl.getBlock(result.x, y, result.z) == null && y > 1)
        {
            y--;
        }
        result.setY(y+2);

        return result;
    }*/

    @Override
    public BlockChunkControl generateChunk(BlockTerrainControl terrain, Vector3Int chunkLocation, CubesSettings settings)
    {
        int yMax = settings.getChunkSizeY()/2;
        BlockChunkControl chunk;
        chunk = new BlockChunkControl(terrain, chunkLocation);
        for (int x = 0; x < settings.getChunkSizeX(); x++)
        {
            for (int z = 0; z < settings.getChunkSizeZ(); z++)
            {
                for (int y = 0; y < yMax; y++)
                {
                    Block place = CubeAssets.BLOCK_GRASS;
                    /*
                    Block place = Blocks.STONE;
                    if( y == yMax - 1)
                    {
                        place = Blocks.GRASS;
                    }
                    else if ( y > yMax - 5)
                    {
                        place = Blocks.DIRT;
                    }
                    else
                    {
                        double rand = Math.random() * 5000 ;
                        if(rand < 1000)
                        {
                            place = Blocks.DIRT;
                        }
                        if(rand < 500)
                        {
                            place = Blocks.GRAVEL;
                        }
                        if(rand < 350)
                        {
                            place = Blocks.COAL_BLOCK;
                        }
                        if(rand < 250)
                        {
                            place = Blocks.IRON_ORE;
                        }
                        if(rand < 50 && y < 30)
                        {
                            place = Blocks.GOLD_ORE;
                        }
                        if(rand < 10 && y < 16)
                        {
                            place = Blocks.DIAMOND_ORE;
                        }
                    }
                    * */
                    chunk.setBlock(new Vector3Int(x, y, z), place);
                }
                if(chunkLocation.getY() == 0)
                {
                    chunk.setBlock(new Vector3Int(x, 0, z), CubeAssets.BLOCK_STONE);
                }

            }
        }
        return chunk;
    }
}
