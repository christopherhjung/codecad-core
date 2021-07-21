import SketchScope.Companion.ORIGIN
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
        val param = Parameter( Math.PI)
        val x2 = param.pow(3)
        val derivative = x2.derivative(param)
        val value = derivative.value
        assertEquals(3.0 * Math.PI.pow(2), value)
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
    fun absTest(){
        val param1 = Parameter(1.0)
        val abs = Value.abs(param1)
        val derivative = abs.derivative(param1)

        assertEquals(1.0, abs.value)
        assertEquals(1.0, derivative.value)
        param1.value = -1.0
        assertEquals(1.0, abs.value)
        assertEquals(-1.0, derivative.value)
    }

    /*@Test
    fun complexTest(){
        val proj = project {
            sketch {
                val topLine = line(point(0.0, 1.0),point(1.0,1.0))
                val bottomLine = line(point(0.0,0.0),point(1.0,0.0))

                val leftArc = arc(point(-1.0,0.0), param(1.0), param(2.0), param(4.0))
                val rightArc = arc(point(1.0,0.0), param(1.0))

                val vertLine = line(topLine.p0, bottomLine.p0)
                val vertLine2 = line(topLine.p1, bottomLine.p1)

                equals(topLine.p0, leftArc.p0)
                equals(bottomLine.p0, leftArc.p1)

                equals(topLine.p1, rightArc.p1)
                equals(bottomLine.p1, rightArc.p0)

                perpendicular(topLine, vertLine)


                equals(topLine.length, bottomLine.length)
                equals(leftArc.radius, rightArc.radius)
                equals(topLine.length, rightArc.radius)

                equals(vertLine.length, vertLine2.length)
                equals(vertLine2.length, const(2.0))

                equals(leftArc.center, vertLine.midPoint )
                equals(rightArc.center, vertLine2.midPoint )

                equals(leftArc.radius, topLine.length)
                equals(topLine.p0, ORIGIN)

                equals(topLine.p0.y, topLine.p1.y)
            }
        }

        assertTrue(proj.sketches.first().solveImpl(1e-8))
    }*/
/*
    @Test
    fun complexTest2() {
        val proj = project {
            sketch {
                //circle(point(0.0,0.0), const(1.0))

                val points = list<Point>()
                val points2 = list<Point>()

                val repeat = 10
                pattern(repeat / 2, ORIGIN) { center ->
                    val a = line(center, point(1.0, 1.0))
                    val b = line(center, point(1.0, 0.5))

                    val arc = arc(point(), param(1.0), param(0.0), param((360.0 / repeat) unit deg))

                    equals(a.p1, arc.p1)
                    equals(b.p1, arc.p0)
                    angle(a, b, const((360.0 / repeat) unit deg))
                    equals(a.length, b.length)
                    equals(b.length, const(1.0))
                    tangent(arc, a)
                    /*tangent(arc, a)
                    tangent(arc, b)*/

                    all(a.p1, points)
                    all(b.p1, points2)
                }


                for (i in 0 until points.size) {
                    line(points[i], points[(i + 1) % points.size])
                    line(points2[i], points2[(i + 1) % points2.size])
                }

                print(points)
            }
        }

        assertTrue(proj.sketches.first().solveImpl(1e-8))
    }*/
}
