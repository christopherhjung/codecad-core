import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertTrue

class SketchTest {

    @Test
    fun complexSketch(){
        sketch{
            val A = constPoint()
            val M = point(0.9,0.0)
            val C = point(3.8,0.0)
            val P = point(4.0,1.0)
            val B = point(1.0,2.0)

            val r = const(1.0)
            val circle = circle(P,r)

            val lineAB = line(A,B)
            val lineAC = line(A,C)
            val lineAP = line(A,P)
            val linePC = line(P,C)
            val lineMP = line(M,P)
            val linePM = line(P,M)
            val lineMC = line(M,C)

            tangent(circle, lineAB)
            tangent(circle, lineAC)
            pointOnLineMidpoint(M, lineAC)
            horizontal(lineAC)
            vertical(linePC)

            pointOnCircle(C, circle)
            pointOnCircle(B, circle)

            val rad = param(30.0 unit deg)
            angle(linePC, linePM, rad)
            angle(lineMC, lineMP, rad)

            assertTrue {
                val evaluatedLength = (lineAP.a.toVector() - lineAP.b.toVector()).length()
                val realLength = sqrt(5.0)

                abs(evaluatedLength - realLength) < 10e-6
            }
        }
    }

    @Test
    fun angleTest(){
        val start = System.currentTimeMillis()
        val sketch = sketch{
            val A = constPoint()
            val B = constPoint(1.0,1.0)
            val C = point(2.0,1.0)

            val lineA = line(A, B)
            val lineB = line(A, C)

            angle(lineA, lineB, const(179.0 unit deg))
        }
    }
}
