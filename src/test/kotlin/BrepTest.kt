import com.codecad.core.World
import com.codecad.core.export.StlExport
import com.codecad.core.face.entity.*
import com.codecad.core.mesh.MeshGenerator
import com.codecad.core.volume.Volume
import org.junit.jupiter.api.Test
import java.io.FileOutputStream

class BrepTest {

    fun saveStl(volume: Volume){
        val meshGenerator = MeshGenerator()
        val mesh = meshGenerator.generate(volume)

        val stlExport = StlExport()
        val byteArray = stlExport.export(mesh)

        val outStream = FileOutputStream("test.stl")
        outStream.write(byteArray)
        outStream.close()
    }

    @Test
    fun cylinderTest(){
        val world = World()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionX, world.DirectionY )
        val volume = VolumeSuite.createCylinder(bottomWorkplane, 50.0, 200.0)
        saveStl(volume)
    }

    @Test
    fun pipeTest(){
        val world = World()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionX, world.DirectionY )
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        saveStl(volume)
    }

    @Test
    fun pipeTiltTest(){
        val world = World()
        val xzWorkplane = (world.DirectionX + world.DirectionZ).normalized()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, xzWorkplane, world.DirectionY )
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        saveStl(volume)
    }

    @Test
    fun planetest(){
        val world = World()
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionX, world.DirectionY )
        val volume = VolumeSuite.createPlane(workplane, 50.0)
        saveStl(volume)
    }

    @Test
    fun roundplanetest(){
        val world = World()
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionX, world.DirectionY )
        val volume = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        saveStl(volume)
    }
}