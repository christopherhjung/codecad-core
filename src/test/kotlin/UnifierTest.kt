import com.codecad.core.ast.vec.Vec2
import com.codecad.core.sketch.Unifier
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class UnifierTest {

    @Test
    fun unifierTest(){
        val unifier = Unifier<Vec2>{ lhs, rhs ->
            lhs.distance(rhs) < 0.001
        }

        val a = Vec2(0.0001, 0.0)
        val b = Vec2(0.0, 0.0)

        unifier.add(a)
        unifier.add(b)

        val a2 = unifier.get(a)
        val b2 = unifier.get(b)

        assertSame(a2, b2)
    }

    @Test
    fun unifierTest2(){
        val unifier = Unifier<Vec2>{ lhs, rhs ->
            lhs.distance(rhs) < 0.001
        }

        val a = Vec2(-0.0008, 0.0)
        val b = Vec2(0.0, 0.0)
        val c = Vec2(0.0008, 0.0)

        unifier.add(a)
        unifier.add(c)
        unifier.add(b)

        val a2 = unifier.get(a)
        val b2 = unifier.get(b)
        val c2 = unifier.get(c)

        assertSame(a2, b2)
        assertSame(b2, c2)
    }
}