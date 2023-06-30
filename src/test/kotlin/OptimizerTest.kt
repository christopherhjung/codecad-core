import com.codecad.core.Solver
import com.codecad.core.Tracker
import com.codecad.core.parser.ast.primitive.ParamExpr
import com.codecad.core.sketch.World
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OptimizerTest {

    @Test
    fun tangentConstraint(){
        val solver = Solver(Tracker())
        val world = World()
        val target = world.literal(Math.PI)
        val current = ParamExpr(world, 100.0)
        val errorTerm = (current - target).pow(2)
        val success = solver.solveImpl(listOf(current), errorTerm, listOf(errorTerm.derivative(current)), 1e-6)

        assertEquals(target.evalDouble(), current.evalDouble(), 1e-3)
        assertTrue(success)
    }

    /*
    * A =

   0   1
   0   0
   *
   * B =

   0
   1

    * */

    @Test
    fun accelTest(){
        /*var currentX = 0.0
        var currentXD = 0.0
        var targetX = 10.0
        var targetXD = 0.0
        var K1 = 100.0
        var K2 = 100.995
        while(true){
            val u = -( K1 * (currentX - targetX) + K2 * ( currentXD - targetXD) )

            currentX += 0.01 * currentXD
            currentXD += 0.01 * u

            println(u)
        }*/

    }

}
