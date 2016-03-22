/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package terrain;

import NuWorldServer.CubeAssets;
import com.cubes.Block;
import com.cubes.BlockChunkControl;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;
import java.util.Random;

/**
 *
 * @author Perry
 */
public class Simplex2D implements TerrainGenerator
{
    //private JBlockHelper blockHelper;
    private BlockTerrainControl terrainControl;
    /*private int water_level = (int) (GameProperties.CHUNK_SIZE.y * .2f);
    private int stone_level = (int) (GameProperties.CHUNK_SIZE.y * .4f);
    private int snow_level = (int) (GameProperties.CHUNK_SIZE.y * .6f);
    private int ice_level = (int) (GameProperties.CHUNK_SIZE.y * .75f);
    */
    private int water_level = (int) (10);
    private int stone_level = (int) (20);
    private int snow_level = (int) (30);
    private int ice_level = (int) (40);
    
    private Vector3Int offset;

    public Simplex2D(/*JBlockHelper blockHelper, */BlockTerrainControl terrainControl)
    {
        //this.blockHelper = blockHelper;
        this.terrainControl = terrainControl;
    }

    @Override
    public void generateTerrain(Vector3Int offset, CubesSettings cubeSettings, BlockTerrainControl terrain)
    {
        this.offset = offset;
        System.out.println("**** OFFSET " + offset);
        long seed = cubeSettings.getTerrainSeed();
        if(seed == -1)
        {
            Random rndm = new Random(System.currentTimeMillis());
            rndm.nextLong();
            seed = rndm.nextLong();
        }
        SimplexNoise.setSeed(seed);
        Vector3Int chunkViewDistance = cubeSettings.getChunkViewDistance();
        System.out.print("Generating Terrain");
        for (int x = 0 + offset.getX(); x < offset.getX() + chunkViewDistance.getX(); x++)
        {
            //for (int y = 0 + offset.getY(); y < offset.getY() + chunkViewDistance.getY(); y++)
            //{
                for (int z = 0 + offset.getZ(); z < offset.getZ() + chunkViewDistance.getZ(); z++)
                {
                    Vector3Int chunkLocation = new Vector3Int(x, 0, z);
                    BlockChunkControl c = generateChunk(terrain, chunkLocation, cubeSettings);
                    terrainControl.setChunk(chunkLocation, c);
                }
            //}
        }
        System.out.println();
        System.out.println("Adding All Chunks");
        //terrainControl.addAllScheduledChunks();
    }

    @Override
    public BlockChunkControl generateChunk(BlockTerrainControl terrain, Vector3Int chunkLocation, CubesSettings settings)
    {
        int chunkSizeX = settings.getChunkSizeX();
        int chunkSizeY = settings.getChunkSizeY();
        int chunkSizeZ = settings.getChunkSizeZ();
        Vector3Int offset = chunkLocation.mult(new Vector3Int(chunkSizeX, chunkSizeY, chunkSizeZ));
        BlockChunkControl chunk = new BlockChunkControl(terrain, chunkLocation);
        //chunk.setLocation(chunkLocation);
        for (int x = 0; x < chunkSizeX; x++)
        {
            for (int z = 0; z < chunkSizeZ; z++)
            {
                //chunk.setBlock(new Vector3Int(x, 0, z), CubeAssets.BLOCK_BRICK);
                chunk.setBlock(new Vector3Int(0, 0, 0), CubeAssets.BLOCK_BRICK);
                Double c = sumOctave(TerrainGeneratorFactory.ITERATIONS, x+offset.getX(), z+offset.getZ(), TerrainGeneratorFactory.PERSISTENCE, TerrainGeneratorFactory.SIMPLEX_SCALE);
                c = normalize(c, 1, 45/*chunkSizeY TODO: Understand this*/ );
                c -= chunkSizeY * chunkLocation.getY();
                for (int y = 0; y < c; y++)
                {
                    Block place = CubeAssets.BLOCK_GRASS;
                    /*
                    double rand = Math.random() * 5000 ;
                    if(rand < 1000)
                    {
                        place = Blocks.STONE;
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
                    * */
                    chunk.setBlock(new Vector3Int(x, y, z), place);
                }
/*
                if(c<water_level) // water
                {
                    for(int y=c.intValue()+1; y<=water_level; y++)
                    {
                        chunk.setBlock(x, y, z, Blocks.LAPIS_BLOCK);
                    }
                }

                for(int y = c.intValue()-2; y <= c.intValue(); y++)
                {
                    JBlock place = Blocks.GRASS;
                    if(c>stone_level)
                    {
                        place = Blocks.STONE;
                    }
                    if(c>snow_level)
                    {
                        place = Blocks.SNOWBLOCK;
                    }
                    if(c>ice_level)
                    {
                        place = Blocks.ICE;
                    }
                    chunk.setBlock(x,y,z, place);
                }*/
            }
        }
        return chunk;
    }

    private double sumOctave(int num_iterations, double x, double z, double persistence, double scale)
    {
        double maxAmp = 0;
        double amp = 1;
        double noise = 0;

        //#add successively smaller, higher-frequency terms
        for(int i = 0; i < num_iterations; ++i)
        {
            noise += SimplexNoise.noise2D(x, z, scale) * amp;
            maxAmp += amp;
            amp *= persistence;
            scale *= 2;
        }

        //take the average value of the iterations
        noise /= maxAmp;

        return noise;
    }

    private Double normalize(double value, double low, double high)
    {
        return value * (high - low) / 2 + (high + low) / 2;
    }

}
