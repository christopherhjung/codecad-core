import com.codecad.core.*
import com.codecad.core.parser.ast.ParamExpr
import com.codecad.core.parser.ast.times
import com.codecad.core.sketch.World
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CacheTest {

    @Test
    fun cacheTest(){
        val world = World();
        val a = ParamExpr(world, 2.0)
        val b = ParamExpr(world, 3.0)
        assertSame(a + b, a + b)
        assertSame(a * b, a * b)
        assertSame(a / b, a / b)
        assertSame(a.pow(b), a.pow(b))
        assertSame(a.smaller(b), a.smaller(b))
    }

    @Test
    fun commuTest(){
        val world = World();
        val a = ParamExpr(world, 2.0)
        val b = ParamExpr(world, 3.0)
        assertSame(a + b, b + a)
        assertSame(a * b, b * a)
        assertNotSame(a / b, b / a)
        assertNotSame(a.pow(b), b.pow(a))
        assertNotSame(a.smaller(b), b.smaller(a))
    }

    @Test
    fun equalsTest(){
        val world = World();
        val a = ParamExpr(world, 2.0)
        val b = ParamExpr(world,3.0)
        assertEquals(world.add(a,b), world.add(a,b))
        assertEquals(world.mul(a,b), world.mul(a,b))
        assertEquals(world.div(a,b), world.div(a,b))
        assertEquals(world.pow(a,b), world.pow(a,b))
    }

    @Test
    fun addSimplification(){
        val world = World();
        val a = ParamExpr(world, 2.0)
        assertSame(a+a, 2.0 * a)
    }

    @Test
    fun timesSimplification(){
        val world = World();
        val a = ParamExpr(world, 2.0)
        assertSame(a*a, a.pow(2))
    }

    @Test
    fun complexCacheTest(){
        val world = World();
        val a = ParamExpr(world, 2.0)
        val b = ParamExpr(world, 3.0)
        val c = ParamExpr(world, 3.0)
        assertSame(a + b * c, c * b + a )
    }
}
