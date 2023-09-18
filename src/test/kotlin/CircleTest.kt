import com.codecad.core.World
import com.codecad.core.ast.vec.Matrix
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.regression.Regression
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
    fun lineFitting(){

        val v1 = Vec3(1.0, 1.0, 0.0)
        val v2 = Vec3(2.0, 2.0, 0.0)
        val v3 = Vec3(3.0, 3.0, 0.0)
        val points = listOf(v1, v2, v3)

        val centroid = Vec3(
            points.sumOf { it.x },
            points.sumOf { it.y },
            points.sumOf { it.z }
        ) / points.size

        // Calculate the covariance matrix
        var xx = 0.0
        var yy = 0.0
        var zz = 0.0
        var xy = 0.0
        var xz = 0.0
        var yz = 0.0

        for (point in points) {
            val dx = point.x - centroid.x
            val dy = point.y - centroid.y
            val dz = point.z - centroid.z

            xx += dx * dx
            yy += dy * dy
            zz += dz * dz
            xy += dx * dy
            xz += dx * dz
            yz += dy * dz
        }

        val COV = Matrix(3, 3, doubleArrayOf(
            xx, xy, xz,
            xy, yy, yz,
            xz, yz, zz
        ))

        val ident = Matrix.identity(3)

        val eigValues = COV.eigenvalues()

        println(eigValues.toList().toString())
    }




    fun fit3DLine(points: List<Vec3>): Pair<Vec3, Vec3> {
        // Calculate the means of x, y, and z coordinates
        val meanX = points.map { it.x }.average()
        val meanY = points.map { it.y }.average()
        val meanZ = points.map { it.z }.average()

        // Calculate the sums of products
        var sumXY = 0.0
        var sumXZ = 0.0
        var sumYZ = 0.0
        var sumXX = 0.0
        var sumYY = 0.0
        var sumZZ = 0.0

        for (point in points) {
            val dx = point.x - meanX
            val dy = point.y - meanY
            val dz = point.z - meanZ

            sumXY += dx * dy
            sumXZ += dx * dz
            sumYZ += dy * dz
            sumXX += dx * dx
            sumYY += dy * dy
            sumZZ += dz * dz
        }

        val denominator = (sumXX * sumZZ - sumXZ * sumXZ)
        if (denominator == 0.0) {
            throw RuntimeException("")
        }

        // Calculate the coefficients of the line equation
        val a = (sumYY * sumZZ - sumYZ * sumYZ) / denominator
        val b = (sumXY * sumZZ - sumXZ * sumYZ) / denominator
        val c = (sumXY * sumYZ - sumXX * sumYZ) / denominator

        // Calculate two points on the line
        val point1 = Vec3(meanX - 10.0, meanY - 10.0, a * (meanX - 10.0) + b * (meanY - 10.0) + c * meanZ)
        val point2 = Vec3(meanX + 10.0, meanY + 10.0, a * (meanX + 10.0) + b * (meanY + 10.0) + c * meanZ)

        return Pair(point1, point2)
    }


    @Test
    fun xxx(){
        val points = listOf(
            Vec3(1.0, 2.0, 3.0),
            Vec3(2.0, 3.0, 4.0),
            Vec3(3.0, 4.0, 5.0),
            Vec3(4.0, 5.0, 6.0),
            Vec3(5.0, 6.0, 7.0)
        )

        // Fit a 3D line to the points
        val (point1, point2) = fit3DLine(points)

        // Print the two points on the fitted line
        println("Point 1 on the line: (${point1.x}, ${point1.y}, ${point1.z})")
        println("Point 2 on the line: (${point2.x}, ${point2.y}, ${point2.z})")
    }
}