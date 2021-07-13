import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SketchTest {

    @Test
    fun complexSketch(){

    }

    @Test
    fun cubicDerivativeTest(){
        val param = Parameter(0.0)
        val x2 = param.pow(3)
        val derivative = x2.derivative(param)
        val value = derivative.value
        assertEquals(value, 3.0 * Math.PI.pow(2))
    }

    @Test
    fun constBecomeConst(){
        val param1 = Const(Math.PI)
        val param2 = Const(Math.PI)
        val x2 = param1.pow(param2)
        assertTrue(x2 is Const)
        assertEquals(x2.value, Math.PI.pow(Math.PI))
    }

    @Test
    fun proxyTest(){
        val param1 = Parameter(0.0)
        val param2 = Const(Math.PI)
        val x2 = param1.pow(param2)
        assertTrue(x2 is Const)
        assertEquals(x2.value, Math.PI.pow(Math.PI))
    }

    @Test
    fun absTest(){
        val param1 = Parameter(1.0)
        val abs = Value.abs(param1)
        val derivative = abs.derivative(param1)

        assertEquals(abs.value, 1.0)
        assertEquals(derivative.value, 1.0)
        param1.value = -1.0
        assertEquals(abs.value, 1.0)
        assertEquals(derivative.value, -1.0)



    }
}
