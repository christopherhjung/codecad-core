import com.codecad.core.World
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.brep.WorkplaneExpr
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class WorkplaneTest {

    val world = World()

    fun createPlacement() : WorkplaneExpr {
        val origin = world.vec3(world.literal(2.3), world.literal(3.4), world.literal(4.5))
        val axisA = world.vec3(world.literal(1.0), world.literal(1.0), world.literal(0.0))
        val axisB = world.vec3(world.literal(0.0), world.literal(0.0), world.literal(1.0))
        val placement = WorkplaneExpr(origin, axisA, axisB)
        return placement
    }

    @Test
    fun first(){
        val placement = createPlacement()

        val x = ParamExpr(world,1.0)
        val y = ParamExpr(world,2.0)

        val result = placement.unproject(x, y)
        val actual = placement.project(result)

        val expected = world.vec2(x, y)
        assertSame(expected, actual)
    }
}