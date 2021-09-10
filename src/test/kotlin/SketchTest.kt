import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.SketchScope.Companion.ORIGIN
import com.codecad.core.Value.Companion.const
import com.codecad.core.Value.Companion.cos
import com.codecad.core.Value.Companion.sin
import com.codecad.core.test.DirectedPlane
import com.codecad.core.test.Node
import com.codecad.core.test.mapModel
import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SketchTest {

    @Test
    fun complexSketch() {

    }

    @Test
    fun cubicDerivativeTest() {
        val param = Parameter(Math.PI)
        val x2 = param.pow(3)
        val derivative = x2.derivative(param)
        val value = derivative.value
        assertEquals(3.0 * Math.PI.pow(2), value)
    }

    @Test
    fun constBecomeConst() {
        val param1 = Const(Math.PI)
        val param2 = Const(Math.PI)
        val x2 = param1.pow(param2)
        assertTrue(x2 is Const)
        assertEquals(x2.value, Math.PI.pow(Math.PI))
    }

    @Test
    fun absTest() {
        val param1 = Parameter(1.0)
        val abs = Value.abs(param1)
        val derivative = abs.derivative(param1)

        assertEquals(1.0, abs.value)
        assertEquals(1.0, derivative.value)
        param1.value = -1.0
        assertEquals(1.0, abs.value)
        assertEquals(-1.0, derivative.value)
    }

    @Test
    fun complexTest() {
        val proj = project {
            sketch {
                val topLine = line(point(0.0, 1.0), point(1.0, 1.0))
                val bottomLine = line(point(0.0, 0.0), point(1.0, 0.0))

                val vertLine = line(topLine.p0, bottomLine.p0)
                val vertLine2 = line(topLine.p1, bottomLine.p1)

                val leftArc = arc(vertLine.p0, vertLine.p1, param(1.0))
                val rightArc = arc(vertLine2.p1, vertLine2.p0, param(1.0))
                equals(topLine.length, bottomLine.length)
                perpendicular(topLine, vertLine)
                equals(topLine.length, bottomLine.length)
                equals(leftArc.radius, rightArc.radius)
                equals(topLine.length, rightArc.radius)

                equals(vertLine.length, vertLine2.length)
                equals(vertLine2.length, const(2.0))

                equals(leftArc.center, vertLine.midPoint)
                equals(rightArc.center, vertLine2.midPoint)

                equals(leftArc.radius, topLine.length)
                equals(topLine.p0, ORIGIN)

                equals(topLine.p0.y, topLine.p1.y)
            }
        }

        assertTrue(proj.sketches.first().solveImpl(1e-8))
    }

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

                    val arc = arc(b.p1, a.p1, param(1.0))

                    angle(a, b, const((360.0 / repeat) unit deg))
                    equals(a.length, b.length)
                    equals(b.length, const(1.0))
                    tangent(arc, a)

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
    }


    @Test
    fun functionTest() {
        val proj = project {
            sketch {
                func {
                    t -> Point(Value.cos(t), Value.sin(t))
                }
            }
        }
    }

    @Test
    fun testets(){
        fun SketchScope.cycloid(bR: Value, sR: Value): FunctionFigure {
            return func { t ->
                val r = t*Math.PI*2
                val combined = sR + bR
                Point(combined*cos(r) - sR*cos(combined*(r/sR)) ,
                    combined*sin(r) - sR*sin(combined*(r/sR)))
            }
        }

        val project = project {

            val big = sketch {
                polygon(
                    Point(const(-0.5), const(-0.5)),
                    Point(const(0.5), const(-0.5)),
                    Point(const(0.5), const(0.5)),
                    Point(const(-0.5), const(0.5)),
                )
            }

            val small = sketch {
                polygon(
                    Point(const(-0.2), const(-0.2)),
                    Point(const(0.2), const(-0.2)),
                    Point(const(0.2), const(0.2)),
                    Point(const(-0.2), const(0.2)),
                )
            }

            extrude(big, const(1.0))
            extrude(small, const(2.0))


        }

        val model = mapModel(project)

        println("test")

    }

}
