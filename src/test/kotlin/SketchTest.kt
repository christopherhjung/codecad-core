import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.SketchScope.Companion.ORIGIN
import com.codecad.core.Value.Companion.const
import com.codecad.core.Value.Companion.cos
import com.codecad.core.Value.Companion.sin
import com.codecad.core.test.DirectedPlane
import com.codecad.core.test.Node
import com.codecad.core.test.addVolumes
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


            val posX = 0.4
            val posY = 0.0

            val small = sketch {
                polygon(
                    Point(const(-0.2 + posX), const(-0.2 + posY)),
                    Point(const(0.2 + posX), const(-0.2 + posY)),
                    Point(const(0.2 + posX), const(0.2 + posY)),
                    Point(const(-0.2 + posX), const(0.2 + posY)),
                )
            }

            extrude(big, Const(1.0))
            extrude(small, Const(2.0))
        }

        val model = mapModel(project)

        println("test")

    }


    @Test
    fun cornerCase(){

        val leftFrontBottom = Node(PointD(-0.5,-0.5))
        val rightFrontBottom = Node(PointD(0.5,-0.5))
        val rightBackBottom = Node(PointD(0.5,0.5))
        val leftBackBottom = Node(PointD(-0.5,0.5))

        val leftFrontTop = Node(PointD(-0.5,-0.5, 1.0))
        val rightFrontTop = Node(PointD(0.5,-0.5, 1.0))
        val rightBackTop = Node(PointD(0.5,0.5, 1.0))
        val leftBackTop = Node(PointD(-0.5,0.5, 1.0))

        val leftBottom = Node(PointD(0.4, 0.0, 0.0))
        val rightBottom = Node(PointD(0.6, 0.0, 0.0))
        val leftTop = Node(PointD(0.4, 0.0, 2.0))
        val rightTop = Node(PointD(0.6, 0.0, 2.0))

        val bottom = listOf(
            leftBackBottom,
            rightBackBottom,
            rightFrontBottom,
            leftFrontBottom,
        )

        val top = listOf(
            leftFrontTop,
            rightFrontTop,
            rightBackTop,
            leftBackTop,
        )

        val right = listOf(
            rightFrontBottom,
            rightBackBottom,
            rightBackTop,
            rightFrontTop
        )

        val tool = listOf(
            leftBottom,
            rightBottom,
            rightTop,
            leftTop
        )

        val baseVolume = FacedVolume(
            listOf(
                PolygonFace(bottom, Plane.fromPoints(bottom.map { it.point })),
                PolygonFace(top, Plane.fromPoints(top.map { it.point })),
                PolygonFace(right, Plane.fromPoints(right.map { it.point })),
            )
        )

        val toolVolume = FacedVolume(
            listOf(
                PolygonFace(tool, Plane.fromPoints(tool.map { it.point })),
            )
        )

        val result = addVolumes(baseVolume, toolVolume)

        println(result)
    }

    @Test
    fun tetstststs(){
        val project = project {
            val a = sketch {
                //circle(point(0.0,0.0), const(1.0))

                val topLine = line(point(0.0, 1.0),point(1.0,1.0))
                val bottomLine = line(point(0.0,0.0),point(1.0,0.0))

                val vertLine = cline(topLine.p0, bottomLine.p0)
                val vertLine2 = cline(topLine.p1, bottomLine.p1)

                val leftArc = arc(vertLine.p0, vertLine.p1, param(1.0))
                val rightArc = arc(vertLine2.p1, vertLine2.p0, param(1.0))


                equals(topLine.length, bottomLine.length)



                perpendicular(topLine, vertLine)



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



            val posX = 0.0
            val posY = 0.0

            val witdth = 1
            val height = 1

            val small = sketch {
                polygon(
                    Point(const(-2 + posX), const(-2.5 + posY)),
                    Point(const(2.5 + posX), const(-2.5 + posY)),
                    Point(const(2.5 + posX), const(0.1 + posY)),
                    Point(const(-2 + posX), const(0.1 + posY)),
                )
            }

            extrude(small, Const(1.0))
            extrude(a, Const(0.2), DirectedPlane.from(Plane(Plane.XY.normal, 0.1)))
        }


        val model = mapModel(project)

        println("test")
    }



    @Test
    fun fingerboard(){
        val project = project {
            val posX = 0.0
            val posY = 0.0

            val small = sketch {
                val rect = create(Rect::class)
                equals(rect.center, ORIGIN)
                equals(rect.width, const(4))
                equals(rect.height, const(5))
                equals(rect.top.p0.y, rect.top.p1.y)
            }

            val hole = sketch {
                val rect = create(RoundRect::class)
                equals(rect.center, constPoint(0,1))
                equals(rect.width, const(1))
                equals(rect.height, const(1))
            }

            val hole2 = sketch {
                val rect = create(RoundRect::class)
                equals(rect.center, constPoint(0,-1))
                equals(rect.width, const(1))
                equals(rect.height, const(2))
            }

            extrude(small, Const(1.0))
            extrude(hole, Const(0.8), DirectedPlane.from(Plane(Plane.XY.normal, -0.1), PointD(1.0)))
            extrude(hole2, Const(0.8), DirectedPlane.from(Plane(Plane.XY.normal, -0.1), PointD(1.0)))

        }


        val model = mapModel(project)

        println("test")
    }


    @Test
    fun routedTest(){
        val project = project {
            val big = sketch {
                polygon(
                    Point(const(-0.5), const(-0.5)),
                    Point(const(0.5), const(-0.5)),
                    Point(const(0.5), const(0.5)),
                    Point(const(-0.5), const(0.5)),
                )
            }

            val posX = 0.4
            val posY = 0.0

            val small = sketch {
                polygon(
                    Point(const(-0.2 + posX), const(-0.2 + posY)),
                    Point(const(0.2 + posX), const(-0.2 + posY)),
                    Point(const(0.2 + posX), const(0.2 + posY)),
                    Point(const(-0.2 + posX), const(0.2 + posY)),
                )
            }

            extrude(big, Const(1.0))
            extrude(small, Const(2.0), DirectedPlane.from(Plane.XY.move(-0.1)))
        }

        val model = mapModel(project)

        println("test")

    }
}
