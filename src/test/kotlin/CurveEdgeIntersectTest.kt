
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.part.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CurveEdgeIntersectTest {
    @Test
    fun importTest2(){

        val line = Line(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 1.0, 0.0))
        val edge = Edge.line(Vec3(0.5, 0.0, 0.0), Vec3(0.5, 0.5, 0.0))

        val intersect = CurveEdgeIntersect.intersectLinePlane(line, edge.curve)
        assertEquals(Vec3(0.5, 0.5, 0.0), intersect.first())
    }

    @Test
    fun importTest3(){

        val line = Line(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.4, 0.0))
        val edge = Edge.line(Vec3(0.0, 0.4, 0.0), Vec3(1.0, 0.0, 0.0))

        val intersect = CurveEdgeIntersect.intersectLinePlane(line, edge.curve)
        assertEquals(Vec3(0.5, 0.2, 0.0), intersect.first())
    }
}