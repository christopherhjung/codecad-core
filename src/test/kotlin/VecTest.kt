import com.codecad.core.SketchArc
import com.codecad.core.SketchCircle
import com.codecad.core.SketchLine
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.sketch.cutLines
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VecTest {
    @Test
    fun angleToTest(){
        assertEquals(
            Math.PI / 2,
            Vec2(1.0, 1.0).angleTo(Vec2(-1.0, 1.0))
        )
    }

    @Test
    fun angleToTest2(){
        assertEquals(
            Math.PI / 2,
            Vec2(1.0, -0.1).angleTo(Vec2(1.0, 0.1))
        )
    }
}