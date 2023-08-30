import com.codecad.core.World
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.part.Extruder
import org.junit.jupiter.api.Test

class BrepTest {

    @Test
    fun cylinderTest(){
        val world = World()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val volume = VolumeSuite.createCylinder(bottomWorkplane, 50.0, 200.0)
        ExportHelper.saveStl(volume)
    }

    @Test
    fun pipeTest(){
        val world = World()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        ExportHelper.saveStl(volume)
    }

    @Test
    fun pipeTiltTest(){
        val world = World()
        val xzWorkplane = (world.DirectionX + world.DirectionY).normalized()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, xzWorkplane, world.DirectionY)
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        ExportHelper.saveStl(volume)
    }

    @Test
    fun pipeTiltExtrudeTest(){
        val world = World()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.createCircleWithHole(bottomWorkplane, 50.0, 10.0, 200.0)

        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        ExportHelper.saveStl(volume)
    }

    @Test
    fun planetest(){
        val world = World()
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.createPlane(workplane, 50.0)

        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        ExportHelper.saveStl(volume)
    }

    @Test
    fun roundplanetest(){
        val world = World()
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        ExportHelper.saveStl(face)
    }

    @Test
    fun roundplaneextrudetest(){
        val world = World()
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        println(volume.toString())
        ExportHelper.saveStl(volume)
    }
}