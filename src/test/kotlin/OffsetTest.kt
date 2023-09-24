import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Face
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import org.junit.jupiter.api.Test

class OffsetTest {

    fun offsetFace(face: Face) : List<Face>{
        val surface = face.surface as PlaneSurface
        val workplane = surface.workplane
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
        val workplane = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val face = VolumeSuite.roundedPlane(workplane, 50.0, 5.0)
        offsetFace(face)
    }
}