import com.codecad.core.Intersect
import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.part.BooleanCombine
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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



    @Test
    fun importTest(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

        val c1 = Edge.circle(Vec2(0.0, 0.0), 1.0)
        val c2 = Edge.circle(Vec2(4.9, 0.0), 5.0)

        println(Intersect.of(c1, c2).toList())
    }

    private fun assertContentEquals(expected: Vec2, actual: Vec2, error: Double){
        assertEquals(expected.x, actual.x, error)
        assertEquals(expected.y, actual.y, error)
    }

    private fun assertContentEquals(expected: List<Vec2>, actual: List<Vec2>, error: Double){
        assertEquals(expected.size, actual.size)
        for( (p1, p2) in expected.zip(actual) ){
            assertContentEquals(p1, p2, error)
        }
    }

    @Test
    fun lineCircleIntersecionBetween(){
        val c1 = Edge.line(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = Edge.circle(Vec2(0.5, 0.4), 0.5)

        assertContentEquals(listOf(Vec2(0.2, 0.0), Vec2(0.8, 0.0)), Intersect.of(c1, c2), 1e-8)
    }

    @Test
    fun lineCircleIntersecionRight(){
        val c1 = Edge.line(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = Edge.circle(Vec2(0.8, 0.4), 0.5)

        assertContentEquals(listOf(Vec2(0.5, 0.0)), Intersect.of(c1, c2), 1e-8)
    }

    @Test
    fun lineCircleIntersecionLeft(){
        val c1 = Edge.line(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = Edge.circle(Vec2(-0.2, 0.4), 0.5)

        assertContentEquals(listOf(Vec2(0.1, 0.0)), Intersect.of(c1, c2), 1e-8)
    }

    @Test
    fun lineCircleIntersecionOnLine(){
        val c1 = Edge.line(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = Edge.circle(Vec2(0.5, 0.0), 0.5)

        assertContentEquals(listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0)), Intersect.of(c1, c2), 1e-8)
    }

}