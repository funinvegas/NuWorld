/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package terrain;

import com.cubes.BlockChunkControl;
import com.cubes.BlockTerrainControl;
import com.cubes.CubesSettings;
import com.cubes.Vector3Int;

/**
 *
 * @author Perry
 */
public interface TerrainGenerator
{
    public void generateTerrain(Vector3Int offset, CubesSettings cubeSettings, BlockTerrainControl terrain);
    public BlockChunkControl generateChunk(BlockTerrainControl terrain, Vector3Int chunkLocation, CubesSettings settings);
}
