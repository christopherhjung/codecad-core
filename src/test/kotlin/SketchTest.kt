import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.LiteralExpr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.World
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SketchTest {

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun cubicDerivativeTest() {
        val world = World()
        val param = ParamExpr(world, Math.PI)
        val x2 = param.pow(3)
        val derivative = x2.derivative(param)
        val value = derivative.evalDouble()
        assertEquals(3.0 * Math.PI.pow(2), value)
    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun constBecomeConst() {
        val world = World()
        val param1 = world.literal(Math.PI)
        val param2 = world.literal(Math.PI)
        val x2 = param1.pow(param2)
        assertTrue(x2 is LiteralExpr)
        assertEquals(x2.value, Math.PI.pow(Math.PI))
    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun absTest() {
        val world = World()
        val param1 = ParamExpr(world, 1.0)
        val abs = Expr.abs(param1)
        val derivative = abs.derivative(param1)

        assertEquals(1.0, abs.evalDouble())
        assertEquals(1.0, derivative.evalDouble())
        param1.value = -1.0
        assertEquals(1.0, abs.evalDouble())
        assertEquals(-1.0, derivative.evalDouble())
    }
}
