import com.codecad.core.World
import com.codecad.core.ast.primitive.ParamExpr
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class NormalizeTest {
    val world = World()

    @Test
    fun first(){
        val x = ParamExpr(world,1.0)
        val y = ParamExpr(world,2.0)

        val vec = world.vec2(x, y)
        val normalizedTest = vec.normalized()
        val dblNormalizedTest = normalizedTest.normalized()
        assertSame(normalizedTest, dblNormalizedTest)
    }

    @Test
    fun lengthOne(){
        val x = ParamExpr(world,1.0)
        val y = ParamExpr(world,2.0)

        val vec = world.vec2(x, y)
        val normalizedTest = vec.normalized()
        assertSame(normalizedTest.length(), world.literal(1.0))
    }
}