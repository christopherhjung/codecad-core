import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.sketch.isInside
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InsideTest {
    fun testShape() : Loop<Vec2>{
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
        return loop
    }

    @Test
    fun isLeftInside(){
        assertTrue(testShape().isInside(Vec2(-1.4, 0.4)))
    }

    @Test
    fun isRightInside(){
        assertTrue(testShape().isInside(Vec2(1.4, 0.4)))
    }

    @Test
    fun isLeftOutside(){
        assertFalse(testShape().isInside(Vec2(-2.0, 0.4)))
    }

    @Test
    fun isRightOutside(){
        assertFalse(testShape().isInside(Vec2(2.0, 0.4)))
    }
}