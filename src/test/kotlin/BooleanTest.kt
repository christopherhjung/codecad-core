import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.part.BooleanCombine
import com.codecad.core.part.CombineKind
import com.codecad.core.part.Extruder
import com.codecad.core.part.Revolver
import com.codecad.core.sketch.cutLines
import com.codecad.core.sketch.offsetFace
import com.codecad.core.volume.Volume
import org.junit.jupiter.api.Test

class BooleanTest {
    @Test
    fun importTest2(){

        val workplaneA = Workplane(Vec3(0.0, 0.0, 0.0), Vec3.DirectionZ, Vec3.DirectionX)
        val faceA = VolumeSuite.createPlane(workplaneA, 1.0)

        val workplaneB = Workplane(Vec3(0.5, 0.0, 0.0), Vec3.DirectionY, Vec3.DirectionX)
        val faceB = VolumeSuite.createPlane(workplaneB, 1.0)

        val test = BooleanCombine.intersectFace(faceA, faceB)

    }
}