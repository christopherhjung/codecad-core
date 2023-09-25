import com.codecad.core.Circle2d
import com.codecad.core.Intersect
import com.codecad.core.LineSegment
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Face
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

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

        val c1 = Circle2d(Vec2(0.0, 0.0), 1.0)
        val c2 = Circle2d(Vec2(4.9, 0.0), 5.0)

        println(Intersect.of(c1, c2).toList())
    }

    @Test
    fun importTest2(){
        /*val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face, 1.0)*/

        val c1 = LineSegment(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val c2 = Circle2d(Vec2(0.5, 0.0), 0.5)

        println(Intersect.of(c1, c2).toList())
    }
}