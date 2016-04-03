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
public class Simplex3D implements TerrainGenerator
{
    private BlockTerrainControl terrainControl;
    private Vector3Int offset;

    public Simplex3D(BlockTerrainControl terrainControl)
    {
        this.terrainControl = terrainControl;
    }

    @Override
    public void generateTerrain(Vector3Int offset, CubesSettings cubeSettings, BlockTerrainControl terrain)
    {
        this.offset = offset;
        long seed = cubeSettings.getTerrainSeed();
        if(seed == -1)
        {
            Random rndm = new Random(System.currentTimeMillis());
            rndm.nextLong();
            seed = rndm.nextLong();
        }
        SimplexNoise.setSeed(seed);

        System.out.print("Generating Terrain");
        Vector3Int chunkViewDistance = cubeSettings.getChunkViewDistance();
        System.out.print("Generating Terrain");
        for (int x = 0 + offset.getX(); x < offset.getX() + chunkViewDistance.getX(); x++)
        {
            for (int z = 0 + offset.getZ(); z < offset.getZ() + chunkViewDistance.getZ(); z++)
            {
                Vector3Int chunkLocation = new Vector3Int(x, 0, z);
                BlockChunkControl c = generateChunk(terrain, chunkLocation, cubeSettings);
                terrainControl.setChunk(chunkLocation, c);
            }
        }
        //terrainControl.addAllScheduledChunks();
        System.out.println();
    }

    private double sumOctave(int num_iterations, double x, double y, double z, double persistence, double scale)
    {
        double maxAmp = 0;
        double amp = 1;
        double noise = 0;

        //#add successively smaller, higher-frequency terms
        for(int i = 0; i < num_iterations; ++i)
        {
            noise += SimplexNoise.noise3D(x, y, z, scale) * amp;
            maxAmp += amp;
            amp *= persistence;
            scale *= 2;
        }

        //take the average value of the iterations
        noise /= maxAmp;

        return noise;
    }

    public BlockChunkControl generateChunk(BlockTerrainControl terrain, Vector3Int chunkLocation, CubesSettings settings)
    {
        Vector3Int chunkSize = settings.getChunkViewDistance();
        Vector3Int offset = chunkLocation.mult(chunkSize);
        BlockChunkControl chunk = new BlockChunkControl(terrain, chunkLocation);
        for (int x = 0; x < chunkSize.getX(); x++)
        {
            for (int z = 0; z < chunkSize.getY(); z++)
            {
                for (int y = 0; y < chunkSize.getZ(); y++)
                {
                    Double c = sumOctave(TerrainGeneratorFactory.ITERATIONS, x+offset.getX(), y+offset.getY(), z+offset.getZ(), .5, .007);
                    Block place = CubeAssets.BLOCK_WOOD;
                    /*if(c>.05)
                    {
                        int b = y % 15;
                            if(b == 0)
                            {
                                place = Blocks.LOG;
                            }
                            if(b == 1)
                            {
                                place = Blocks.SNOWBLOCK;
                            }
                            if(b == 2)
                            {
                                place = Blocks.LOG;
                            }
                            if(b == 3)
                            {
                                place = Blocks.SPRUCE_LOG;
                            }
                            if(b == 4)
                            {
                                place = Blocks.COAL_BLOCK;
                            }
                            if(b == 5)
                            {
                                place = Blocks.BLUE;
                            }
                            if(b == 6)
                            {
                                place = Blocks.CACTUS;
                            }
                            if(b == 7)
                            {
                                place = Blocks.CHEST;
                            }
                            if(b == 8)
                            {
                                place = Blocks.DIAMOND_ORE;
                            }
                            if(b == 9)
                            {
                                place = Blocks.GOLD_ORE;
                            }
                            if(b == 10)
                            {
                                place = Blocks.LAPIS_ORE;
                            }
                            if(b == 11)
                            {
                                place = Blocks.LAPIS_BLOCK;
                            }
                            if(b == 12)
                            {
                                place = Blocks.FURNACE;
                            }
                            if(b == 13)
                            {
                                place = Blocks.IRON_ORE;
                            }*/
                            chunk.setBlock(new Vector3Int(x, y, z), place);
                    /*}
                    if(c>.4)
                    {
                        chunk.setBlock(x, y, z, Blocks.GOLDBLOCK);
                    }
                    if(c>.7 )
                    {
                        chunk.setBlock(x, y, z, Blocks.LAVA);
                    }*/
                }
                if(chunkLocation.getY() == 0)
                {
                    chunk.setBlock(new Vector3Int(x, 0, z), CubeAssets.BLOCK_BRICK);
                }

            }
        }
        return chunk;
    }
}

