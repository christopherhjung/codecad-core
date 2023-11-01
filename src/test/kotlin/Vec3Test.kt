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
import kotlin.test.assertEquals

class Vec3Test {
    @Test
    fun angleToTop(){
        val a = Vec3(2.0, 0.0, 0.0)
        val b = Vec3(-2.0, 2.0, 0.0)
        val angle = a.angleTo(b, Vec3.DirectionZ)
        assertEquals(Math.PI / 4.0 * 3.0, angle, 1e-10)
    }
    @Test
    fun angleToBottom(){
        val a = Vec3(2.0, 0.0, 0.0)
        val b = Vec3(-2.0, -2.0, 0.0)
        val angle = a.angleTo(b, Vec3.DirectionZ)
        assertEquals(-Math.PI / 4.0 * 3.0, angle, 1e-10)
    }


    @Test
    fun angleToTopDiff(){
        val a = Vec3(1.0, 0.0, 0.0)
        val b = Vec3(-2.0, 2.0, 0.0)
        val angle = a.angleTo(b, Vec3.DirectionZ)
        assertEquals(Math.PI / 4.0 * 3.0, angle, 1e-10)
    }
    @Test
    fun angleToBottomDiff(){
        val a = Vec3(2.0, 0.0, 0.0)
        val b = Vec3(-1.0, -1.0, 0.0)
        val angle = a.angleTo(b, Vec3.DirectionZ)
        assertEquals(-Math.PI / 4.0 * 3.0, angle, 1e-10)
    }
    @Test
    fun angleToHalf(){
        val a = Vec3(2.0, 0.0, 0.0)
        val b = Vec3(-2.0, 0.0, 0.0)
        val angle = a.angleTo(b, Vec3.DirectionZ)
        assertEquals(Math.PI, angle, 1e-10)
    }
    @Test
    fun angleToFull(){
        val a = Vec3(2.0, 0.0, 0.0)
        val b = Vec3(2.0, 0.0, 0.0)
        val angle = a.angleTo(b, Vec3.DirectionZ)
        assertEquals(0.0, angle, 1e-10)
    }
}