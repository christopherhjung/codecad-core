import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstraintTest {

    @Test
    fun tangentConstraint(){
        val sqrt2 = 2.0.pow(-.5)
        val x = Parameter(0.0)
        val y = Parameter(1.0)
        val line = Line(Point(Parameter(0.0), Parameter(0.0)), Point(Parameter(1.0), Parameter(1.0)))
        val circle = Circle(Point(x, y), Const(1.0))
        val constraint = CircleTangent(circle, line)
        val delta = constraint.getDelta()
        assertEquals(sqrt2, delta.value)
        y.value = -1.0
        assertEquals(sqrt2, delta.value)

        y.value = 1.0
        val dx = constraint.equation.derivative(x)
        val dy = constraint.equation.derivative(y)


        println(dx)
    }

}
