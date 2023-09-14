import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.part.BooleanCombine
import org.junit.jupiter.api.Test

class IntersectTest {
    private val world = World()


    @Test
    fun planeLineIntersection(){
        val line = Line(Vec3(0.0, 1.0, 0.0), Vec3(0.0, 1.0, 0.0))

        val workplane = Workplane(Vec3(1.0, 1.0, 1.0), Vec3(1.0, 0.0, 1.0).normalized(), Vec3.DirectionY)
        val plane = workplane.toPlane()
        val intersection = plane.intersect(line)

        println(intersection)
    }

    @Test
    fun faceLineIntersection(){
        val line = Line(Vec3(0.0, 1.0, 0.0), Vec3(0.0, 1.0, 0.0))

        val workplane = Workplane(Vec3(100.0, 0.0, 0.0), Vec3.DirectionZ, Vec3.DirectionX)
        val plane = VolumeSuite.createPlane(workplane, 10.0)

        val combiner = BooleanCombine.isInside(Vec3(100.0, 0.0, 0.0), plane)

        println(combiner)
        println(combiner)
    }

}