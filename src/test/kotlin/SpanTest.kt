import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.Sense
import com.codecad.core.sketch.createFaceTree
import com.codecad.core.sketch.cutLines
import org.junit.jupiter.api.Test

class SpanTest {
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
        val edges = listOf(
            Edge.arc(Vec2.Zero, Vec2(5.0, -1.0), Vec2(-5.0, -1.0), Sense.Same),

            //Edge.circle(Vec2.Zero, 5.0),

            Edge.line(Vec2(-10.0, 0.0), Vec2(10.0, 0.0))
        )

        val tree = createFaceTree(edges)

        println(edges)
    }
}