import com.codecad.core.World
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.mesh.Matrix
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class CircleTest {
    private val world = World()


    @Test
    fun cylinderTest(){
        val offset = Vec2(0.0, 0.0)
        val v1 = Vec2(1.0, 1.0) + offset
        val v2 = Vec2(2.0, 4.0) + offset
        val v3 = Vec2(5.0, 3.0) + offset
        val mat = Matrix(3, 4, doubleArrayOf(
            v1.squaredLength(), v1.x, v1.y, 1.0,
            v2.squaredLength(), v2.x, v2.y, 1.0,
            v3.squaredLength(), v3.x, v3.y, 1.0
        ))

        val fac = 1.0 / mat.withoutColumn(0).det()
        val x0 = 0.5 * mat.withoutColumn(1).det() * fac
        val y0 = -0.5 * mat.withoutColumn(2).det() * fac
        val r = sqrt(x0 * x0 + y0 * y0 + mat.withoutColumn(3).det() * fac)

        println("x0: $x0, y0: $y0, r: $r")
    }

    @Test
    fun cylinderTest2(){
        val offset = Vec2(0.0, 0.0)
        val v1 = Vec2(1.0, 1.0) + offset
        val v2 = Vec2(2.0, 4.0) + offset
        val v3 = Vec2(5.0, 3.0) + offset
        val A = Matrix(3, 3, doubleArrayOf(
            v1.x, v1.y, 1.0,
            v2.x, v2.y, 1.0,
            v3.x, v3.y, 1.0
        ))

        val B = Matrix(3, 1, doubleArrayOf(
            v1.squaredLength(),
            v2.squaredLength(),
            v3.squaredLength()
        ))

        val AT = A.transpose()
        val AP = AT * (A*AT).inverse()
        val xHat = AP * B
        val a = xHat[0, 0]
        val b = xHat[0, 1]
        val c = xHat[0, 2]
        val center = Vec2(a, b) / 2.0
        val r = sqrt(4.0 * c + a * a + b * b) / 2.0


        println("x0: ${center.x}, y0: ${center.y}, r: $r")
    }

}