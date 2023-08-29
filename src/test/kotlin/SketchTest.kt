import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.LiteralExpr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.World
import com.codecad.core.part.PartStudio
import com.codecad.core.part.Sketch
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

    @Test
    fun sketchTest() {
        val world = World()
        val partStudio = PartStudio(world)
        val sketch = Sketch(partStudio, world.WorkplaneXY, "")
        //var arc = sketch.arc(world.vec2(world.Zero, world.Zero), world.vec2(world.One, world.One))

        with(sketch){
            val lineA = line(0.0, 0.0, 1.0, 0.0)
            val lineB = line(1.0, 0.0, 1.0, 1.0)
            val lineC = line(1.0, 1.0, 0.0, 1.0)
            val lineD = line(0.0, 1.0, 0.0, 0.0)

            eq(world.ZeroVec2, lineA.p0)
            eq(lineD.p1, lineA.p0)
            eq(lineA.p1, lineB.p0)
            eq(lineB.p1, lineC.p0)
            eq(lineC.p1, lineD.p0)

            val width = world.literal(5.0)
            val height = world.literal(2.0)

            len(lineA, width)
            len(lineC, width)

            len(lineB, height)
            len(lineD, height)

            horizontal(lineA)
            horizontal(lineC)

            vertical(lineB)
            vertical(lineD)

            //init(lineA, 0.0, 0.0, 1.0, 0.0)
            //init(lineB, 1.0, 0.0, 1.0, 1.0)
            //init(lineC, 1.0, 1.0, 0.0, 1.0)
            //init(lineD, 0.0, 1.0, 0.0, 0.0)
            solve(1e-10)
            println("xxx")
        }

        println(sketch)

        val face = partStudio.faces.first()
        ExportHelper.saveStl(face)
    }
}
