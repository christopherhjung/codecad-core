import com.codecad.core.ast.vec.Vec2
import com.codecad.core.sketch.RotaryVertexComparator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RotaryComparatorTest {
    @Test
    fun lineCircleCut(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(1.0, 1.0)
        val second = Vec2(-1.0, 1.0)
        assertEquals(-1, comp.compare(first, second))
    }
    @Test
    fun lineCircleCut10(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(0.0, 1.0)
        val second = Vec2(0.0, -1.0)
        assertEquals(-1, comp.compare(first, second))
    }
    @Test
    fun lineCircleCut11(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first =  Vec2.DirX
        val second = Vec2(0.0, -1.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun segmentTestReference(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(-1.0, 0.0)
        val second = Vec2(1.0, 2.0)

        assertEquals(1, comp.compare(first, second))
    }

    @Test
    fun segmentTest(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(2.0, 1.0)
        val second = Vec2(1.0, 2.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun segmentTest2(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(-1.0, 2.0)
        val second = Vec2(-2.0, 1.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun segmentTest3(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(-2.0, -1.0)
        val second = Vec2(-1.0, -2.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun segmentTest4(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(1.0, -2.0)
        val second = Vec2(2.0, -1.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun segmentTest5(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(1.0, 2.0)
        val second = Vec2(-2.0, -1.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun segmentTest6(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(1.0, 2.0)
        val second = Vec2(-1.0, -2.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun lineCircleCut2(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(1.0, 1.0)
        val second = Vec2(-0.1, -1.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun lineCircleCut3(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(1.0, 0.1)
        val second = Vec2(1.0, -0.1)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun lineCircleCut4(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(0.0, 1.0)
        val second = Vec2(0.0, -1.0)
        assertEquals(-1, comp.compare(first, second))
    }

    @Test
    fun lineCircleCut5(){
        val comp = RotaryVertexComparator(Vec2.Zero, Vec2.DirX)
        val first = Vec2(0.0, -1.0)
        val second = Vec2(0.0, 1.0)
        assertEquals(1, comp.compare(first, second))
    }
}