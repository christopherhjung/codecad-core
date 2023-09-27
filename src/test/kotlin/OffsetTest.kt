import com.codecad.core.SketchCircle
import com.codecad.core.Intersect
import com.codecad.core.SketchLine
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Face
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OffsetTest {

    fun offsetFace(face: Face, offset : Double) : List<Face>{
        val surface = face.surface as PlaneSurface
        val workplane = surface.workplane
        val faceNormal = workplane.normal
        for(bound in face.bounds){
            val area = bound.loop.computeArea()
            println(area)
            for( loop in bound.loop ){
                val orientedEdge = loop.edge
                val edge = orientedEdge.edge
                val curve = edge.curve

                when(curve){
                    is Line -> {
                        val origin = curve.origin
                        val direction = curve.direction

                        val projOrigin = workplane.project2d(origin)
                        val projDirection = workplane.projectDir2d(direction)

                        //TODO: can remove normalized?
                        val offsetVec = faceNormal.cross(direction).scaleTo(offset)

                        val offsetLine = Line(origin + offsetVec, direction)


                        println(projOrigin)
                        println(projDirection)
                        println(projDirection)
                    }
                }
            }
        }

        return listOf()
    }

    @Test
    fun importTest(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

        val c1 = SketchCircle(Vec2(0.0, 0.0), 1.0)
        val c2 = SketchCircle(Vec2(4.9, 0.0), 5.0)

        println(Intersect.ofLineLine(c1, c2).toList())
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
        val c1 = SketchLine(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = SketchCircle(Vec2(0.5, 0.4), 0.5)

        assertContentEquals(listOf(Vec2(0.2, 0.0), Vec2(0.8, 0.0)), Intersect.ofLineLine(c1, c2), 1e-8)
    }

    @Test
    fun lineCircleIntersecionRight(){
        val c1 = SketchLine(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = SketchCircle(Vec2(0.8, 0.4), 0.5)

        assertContentEquals(listOf(Vec2(0.5, 0.0)), Intersect.ofLineLine(c1, c2), 1e-8)
    }

    @Test
    fun lineCircleIntersecionLeft(){
        val c1 = SketchLine(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = SketchCircle(Vec2(-0.2, 0.4), 0.5)

        assertContentEquals(listOf(Vec2(0.1, 0.0)), Intersect.ofLineLine(c1, c2), 1e-8)
    }

    @Test
    fun lineCircleIntersecionOnLine(){
        val c1 = SketchLine(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = SketchCircle(Vec2(0.5, 0.0), 0.5)

        assertContentEquals(listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0)), Intersect.ofLineLine(c1, c2), 1e-8)
    }
}