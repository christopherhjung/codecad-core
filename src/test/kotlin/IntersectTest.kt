import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.EdgeLoop
import com.codecad.core.brep.Face
import com.codecad.core.brep.FaceBound
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.part.BooleanCombine
import org.junit.jupiter.api.Test

class IntersectTest {
    private val world = World()

    fun vec3(x: Double, y: Double, z:Double) : Vec3Expr{
        return world.vec3(literal(x), literal(y), literal(z))
    }

    fun literal(value: Double) : Expr {
        return world.literal(value)
    }

    @Test
    fun planeLineIntersection(){
        val line = Line(vec3(0.0, 1.0, 0.0), vec3(0.0, 1.0, 0.0))

        val workplane = WorkplaneExpr(vec3(1.0, 1.0, 1.0), vec3(1.0, 0.0, 1.0).normalized(), world.DirectionY)
        val plane = workplane.toPlane()
        val intersection = plane.intersect(line)

        println(intersection)
    }

    @Test
    fun faceLineIntersection(){
        val line = Line(vec3(0.0, 1.0, 0.0), vec3(0.0, 1.0, 0.0))

        val workplane = WorkplaneExpr(vec3(100.0, 0.0, 0.0), world.DirectionZ, world.DirectionX)
        val plane = VolumeSuite.createPlane(workplane, 10.0)

        val combiner = BooleanCombine.isInside(vec3(100.0, 0.0, 0.0), plane)

        println(combiner)
        println(combiner)
    }

}