import com.codecad.core.SketchArc
import com.codecad.core.SketchCircle
import com.codecad.core.SketchLine
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.Sense
import com.codecad.core.sketch.cutLines
import org.junit.jupiter.api.Test

class CutTest {
    @Test
    fun lineCircleCut(){
        val lines = cutLines(listOf(
            Edge.circle(Vec2.Zero, 5.0),
            Edge.line(Vec2(-10.0, 0.0), Vec2(10.0, 0.0))
        ))

        println(lines)
    }

    @Test
    fun lineArcCut(){
        val lines = cutLines(listOf(
            Edge.arc(Vec2.Zero, Vec2(5.0, -1.0), Vec2(-5.0, -1.0), Sense.Same),
            Edge.line(Vec2(-10.0, 0.0), Vec2(10.0, 0.0))
        ))

        println(lines)
    }
}