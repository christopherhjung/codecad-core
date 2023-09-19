import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.curve.Line
import com.codecad.core.part.BooleanCombine
import com.codecad.core.part.CombineKind
import com.codecad.core.part.Extruder
import com.codecad.core.part.Revolver
import com.codecad.core.volume.Volume
import org.junit.jupiter.api.Test

class BrepTest {
    private val world = World()

    fun literal(value: Double) : Expr {
        return world.literal(value)
    }

    @Test
    fun cylinderTest(){
        val bottomWorkplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val volume = VolumeSuite.createCylinder(bottomWorkplane, 50.0, 200.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun pipeTest(){
        val bottomWorkplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun pipeTiltTest(){
        val xzWorkplane = (Vec3.DirectionX + Vec3.DirectionY).normalized()
        val bottomWorkplane = Workplane(Vec3.Zero, xzWorkplane, Vec3.DirectionY)
        val volume = VolumeSuite.createPipe(bottomWorkplane, 50.0, 10.0, 200.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun pipeTiltExtrudeTest(){
        val bottomWorkplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.createCircleWithHole(bottomWorkplane, 50.0, 10.0)

        val volume = Extruder.extrude(face, Vec3.DirectionZ, 10.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun planetest(){
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.createPlane(workplane, 50.0)

        val volume = Extruder
            .extrude(face, Vec3.DirectionZ, 10.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun roundplanetest(){
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        ExportHelper.saveStep(face)
    }

    @Test
    fun roundPlaneExtrudeTest(){
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        val volume = Extruder
            .extrude(face, Vec3.DirectionZ, 10.0)
        ExportHelper.saveStep(volume, "extrude")
    }

    @Test
    fun circleExtrudeTest(){
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.createCircleWithHole(workplane, 50.0, 5.0)
        val volume = Extruder
            .extrude(face, Vec3.DirectionZ, 10.0)
        ExportHelper.saveStep(volume, "extrude")
    }

    @Test
    fun splineFaceTest(){
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.splineCircle(workplane, 50.0)
        ExportHelper.saveStep(face)
    }

    @Test
    fun splineVolumeTest(){
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.splineCircle(workplane, 50.0)
        val volume = Extruder
            .extrude(face, Vec3.DirectionZ, 10.0)
        ExportHelper.saveStep(volume)
    }

    @Test
    fun revolveRectTest(){
        val workplane = Workplane(Vec3(100.0, 0.0, 0.0), Vec3.DirectionY, Vec3.DirectionX)
        val face = VolumeSuite.createPlane(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(Vec3.Zero, Vec3.DirectionZ), 2.0)
        ExportHelper.saveStep(volume, "revolve")
        ExportHelper.saveStep(face, "face")
    }

    @Test
    fun revolveCircleTest(){
        val workplane = Workplane(Vec3(100.0, 0.0, 0.0), Vec3.DirectionY, Vec3.DirectionX)
        val face = VolumeSuite.createCircle(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(Vec3.Zero, Vec3.DirectionZ), 2.0)
        ExportHelper.saveStep(volume, "revolve")
        ExportHelper.saveStep(face, "face")
    }

    @Test
    fun roundPlaneRevolveTest(){
        val workplane = Workplane(Vec3(100.0, 0.0, 0.0), Vec3.DirectionY, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 10.0, 5.0)
        val volume = Revolver()
            .revolve(face, Line(Vec3.Zero, Vec3.DirectionZ), 1.0)
        println(volume.toString())
        ExportHelper.saveStep(volume, "revolve")
        ExportHelper.saveStep(face, "face")
    }

    @Test
    fun sphereTest(){
        val workplane = Workplane(Vec3(50.0,0.0,0.0), Vec3.DirectionY, Vec3.DirectionX)
        val face = VolumeSuite.createCircle(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(Vec3.Zero, Vec3.DirectionZ), 5.0)
        println(volume.toString())
        ExportHelper.saveStep(volume, "revolve")
    }

    @Test
    fun revolveTriangleTest(){
        val workplane = Workplane(Vec3(50.0,0.0,0.0), Vec3.DirectionY, Vec3.DirectionX)
        val face = VolumeSuite.createTriangle(workplane, 10.0)
        val volume = Revolver()
            .revolve(face, Line(Vec3.Zero, Vec3.DirectionZ), 5.0)
        println(volume.toString())
        //ExportHelper.saveStep(volume.shells.first().faces[2], "revolve")
        ExportHelper.saveStep(volume, "revolve")
    }

    fun box(position : Vec3, size: Double) : Volume{
        val halfSizeExpr = size / 2.0
        val workplane = Workplane(position - Vec3.DirectionZ * halfSizeExpr, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.createPlane(workplane, size)
        val volume = Extruder.extrude(face, Vec3.DirectionZ, size)
        return volume
    }

    @Test
    fun unionTest(){
        val box1 = box(Vec3(5.0, 5.0, 5.0), 10.0)
        val box2 = box(Vec3(10.0, 10.0, 10.0), 10.0)
        //ExportHelper.saveStep(volume.shells.first().faces[2], "revolve")
        val result = BooleanCombine.combine(CombineKind.Add, box1, box2)
        ExportHelper.saveStep(result, "extrude")
    }
}