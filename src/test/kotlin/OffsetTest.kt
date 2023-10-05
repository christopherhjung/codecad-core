import com.codecad.core.Intersect
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.sketch.offsetFace
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OffsetTest {



    @Test
    fun importTest(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

        val topLeft = Vertex(Vec2(-1.0, 1.0))
        val bottomLeft = Vertex(Vec2(-1.0, -1.0))
        val bottomRight = Vertex(Vec2(1.0, -1.0))
        val topRight = Vertex(Vec2(1.0, 1.0))

        val left = Edge.line(topLeft, bottomLeft)
        val bottom = Edge.line(bottomLeft, bottomRight)
        val right = Edge.line(bottomRight, topRight)
        val top = Edge.line(topRight, topLeft)

        val loop = Loop.wireCircular(left, bottom, right, top)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val offsetFace = offsetFace(sketchFace, -0.1)

        println(offsetFace)
    }

    @Test
    fun importTest2(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

        val topLeft = Vertex(Vec2(-1.0, 1.0))
        val bottomLeft = Vertex(Vec2(-1.0, -1.0))
        val bottomRight = Vertex(Vec2(1.0, -1.0))
        val topRight = Vertex(Vec2(1.0, 1.0))

        val leftArcWp = Workplane(Vec2(-1.0, 0.0), Vec2.DirY, Vec2.DirX)
        val rightArcWp = Workplane(Vec2(1.0, 0.0), Vec2.DirY, Vec2.DirX)

        val left = Edge.arc(leftArcWp, topLeft, bottomLeft, Sense.Same)
        val bottom = Edge.line(bottomLeft, bottomRight)
        val right = Edge.arc(rightArcWp, bottomRight, topRight, Sense.Same)
        val top = Edge.line(topRight, topLeft)

        val loop = Loop.wireCircular(left, bottom, right, top)
        val sketchFace = SketchFace(listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

        val offsetFace = offsetFace(sketchFace, -0.1)

        println(offsetFace)
    }

}