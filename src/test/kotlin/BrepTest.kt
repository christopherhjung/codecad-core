import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.curve.Line
import com.codecad.core.part.Extruder
import com.codecad.core.part.Revolver
import org.junit.jupiter.api.Test

class BrepTest {


    val world = World()

    @Test
    fun cylinderTest(){
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val volume = VolumeSuite.createCylinder(bottomWorkplane, 50.0, 200.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun pipeTest(){
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun pipeTiltTest(){
        val xzWorkplane = (world.DirectionX + world.DirectionY).normalized()
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, xzWorkplane, world.DirectionY)
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun pipeTiltExtrudeTest(){
        val bottomWorkplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.createCircleWithHole(bottomWorkplane, 50.0, 10.0, 200.0)

        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        ExportHelper.saveStep(volume)
    }

    @Test
    fun planetest(){
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.createPlane(workplane, 50.0)

        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        ExportHelper.saveStep(volume)
    }

    @Test
    fun roundplanetest(){
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        ExportHelper.saveStep(face)
    }

    @Test
    fun roundPlaneExtrudeTest(){
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        println(volume.toString())
        ExportHelper.saveStep(volume)
    }

    @Test
    fun splineFaceTest(){
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.splineCircle(workplane, 50.0)
        ExportHelper.saveStep(face)
    }

    @Test
    fun splineVolumeTest(){
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionZ, world.DirectionX)
        val face = VolumeSuite.splineCircle(workplane, 50.0)
        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(10.0))
        ExportHelper.saveStep(volume)
    }

    fun vec3(x: Double, y: Double, z:Double) : Vec3Expr{
        return world.vec3(literal(x), literal(y), literal(z))
    }

    fun literal(value: Double) : Expr {
        return world.literal(value)
    }

    @Test
    fun revolveRectTest(){
        val workplane = WorkplaneExpr(vec3(100.0, 0.0, 0.0), world.DirectionY, world.DirectionX)
        val face = VolumeSuite.createPlane(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(world.ZeroVec3, world.DirectionZ), literal(2.0))
        ExportHelper.saveStep(volume, "revolve")
        ExportHelper.saveStep(face, "face")
    }

    @Test
    fun revolveCircleTest(){
        val workplane = WorkplaneExpr(vec3(100.0, 0.0, 0.0), world.DirectionY, world.DirectionX)
        val face = VolumeSuite.createCircle(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(world.ZeroVec3, world.DirectionZ), literal(2.0))
        ExportHelper.saveStep(volume, "revolve")
        ExportHelper.saveStep(face, "face")
    }

    @Test
    fun roundPlaneRevolveTest(){
        val workplane = WorkplaneExpr(vec3(100.0, 0.0, 0.0), world.DirectionY, world.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 10.0, 2.0)
        val volume = Revolver()
            .revolve(face, Line(world.ZeroVec3, world.DirectionZ), literal(2.0))
        println(volume.toString())
        ExportHelper.saveStep(volume, "revolve")
        ExportHelper.saveStep(face, "face")
    }

    @Test
    fun sphereTest(){
        val workplane = WorkplaneExpr(world.ZeroVec3, world.DirectionY, world.DirectionX)
        val face = VolumeSuite.createCircle(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(world.ZeroVec3, world.DirectionZ), literal(2.0))
        println(volume.toString())
        ExportHelper.saveStep(volume, "revolve")
    }
}