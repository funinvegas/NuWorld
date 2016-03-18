/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package terrain;

import com.cubes.BlockTerrainControl;
import com.cubes.Noise;
import java.util.Random;


/**
 *
 * @author Perry
 */
public class TerrainGeneratorFactory
{
    public  enum GeneratorType { FAST_START, FLATLAND, CUBES_NOISE, SIMPLEX_2D, SIMPLEX_3D, JOSHES_WORLD;}

    final static TerrainGeneratorFactory.GeneratorType GENERATOR_TYPE = TerrainGeneratorFactory.GeneratorType.SIMPLEX_2D;
    final static double SIMPLEX_SCALE = .006; // range from around 0.015 to around 0.001  The higher the number the more rugged and extreme the terain.
    final static int ITERATIONS = 4; // Use a value of 1 to get very smooth rolling hills.  No need to go higher than 4.
    final static float INTERNAL_NOISE = 0.05f;
    final static float PERSISTENCE = 0.33f;
    
    public static TerrainGenerator makeTerrainGenerator(GeneratorType type, /*JBlockHelper blockHelper, */BlockTerrainControl terrainControl)
    {
        switch(type)
        {
            case FAST_START:
                return new FastStart(terrainControl);
            case FLATLAND:
                return new Flatland(terrainControl);
            //case CUBES_NOISE:
            //    Noise noise = new Noise(new Random(), .24f, 256, 256);
                //noise.setTerrainControl(terrainControl);
            //    return noise;
            case SIMPLEX_2D:
                return new Simplex2D(terrainControl);
            case SIMPLEX_3D:
                return new Simplex3D(terrainControl);
            //case JOSHES_WORLD:
             //   return new JoshesWorld(terrainControl);
            default:
                return new Flatland(terrainControl);
        }
    }
}
