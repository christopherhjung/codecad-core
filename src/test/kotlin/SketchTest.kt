import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.parser.ast.Expr
import com.codecad.core.parser.ast.LiteralExpr
import com.codecad.core.parser.ast.ParamExpr
import com.codecad.core.sketch.World
import com.codecad.core.test.DirectedPlane
import com.codecad.core.test.Node
import com.codecad.core.test.addVolumes
import com.codecad.core.test.mapModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SketchTest {

    @Test
    fun complexSketch() {

    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun cubicDerivativeTest() {
        val world = World()
        val param = ParamExpr(world, Math.PI)
        val x2 = param.pow(3)
        val derivative = x2.derivative(param)
        val value = derivative.evalDouble()
        assertEquals(3.0 * Math.PI.pow(2), value)
    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun constBecomeConst() {
        val world = World()
        val param1 = world.literal(Math.PI)
        val param2 = world.literal(Math.PI)
        val x2 = param1.pow(param2)
        assertTrue(x2 is LiteralExpr)
        assertEquals(x2.value, Math.PI.pow(Math.PI))
    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun absTest() {
        val world = World()
        val param1 = ParamExpr(world, 1.0)
        val abs = Expr.abs(param1)
        val derivative = abs.derivative(param1)

        assertEquals(1.0, abs.evalDouble())
        assertEquals(1.0, derivative.evalDouble())
        param1.value = -1.0
        assertEquals(1.0, abs.evalDouble())
        assertEquals(-1.0, derivative.evalDouble())
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
                equal(topLine.length, bottomLine.length)
                perpendicular(topLine, vertLine)
                equal(topLine.length, bottomLine.length)
                equal(leftArc.radius, rightArc.radius)
                equal(topLine.length, rightArc.radius)

                equal(vertLine.length, vertLine2.length)
                equal(vertLine2.length, literal(2.0))

                equal(leftArc.center, vertLine.midPoint)
                equal(rightArc.center, vertLine2.midPoint)

                equal(leftArc.radius, topLine.length)
                equal(topLine.p0, origin())

                equal(topLine.p0.y, topLine.p1.y)
            }
        }

        assertTrue(proj.sketches.first().solveImpl(1e-8))
    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun complexTest2() {
        val proj = project {
            sketch {
                //circle(point(0.0,0.0), const(1.0))

                val points = list<Point2>()
                val points2 = list<Point2>()

                val repeat = 10
                pattern(repeat / 2, origin()) { center ->
                    val a = line(center, point(1.0, 1.0))
                    val b = line(center, point(1.0, 0.5))

                    val arc = arc(b.p1, a.p1, param(1.0))

                    angle(a, b, literal((360.0 / repeat) unit deg))
                    equal(a.length, b.length)
                    equal(b.length, literal(1.0))
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
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun functionTest() {
        val proj = project {
            sketch {
                func {
                    t -> Point2(Expr.cos(t), Expr.sin(t))
                }
            }
        }
    }

    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun testets(){
        fun SketchScope.cycloid(bR: Expr, sR: Expr): FunctionFigure {
            return func { t ->
                val r = t*Math.PI*2
                val combined = sR + bR
                Point2(combined * Expr.cos(r) - sR * Expr.cos(combined*(r/sR)) ,
                    combined * Expr.sin(r) - sR * Expr.sin(combined*(r/sR)))
            }
        }

        val project = project {
            val big = sketch {
                polygon(
                    Point2(literal(-0.5), literal(-0.5)),
                    Point2(literal(0.5), literal(-0.5)),
                    Point2(literal(0.5), literal(0.5)),
                    Point2(literal(-0.5), literal(0.5)),
                )
            }


            val posX = 0.4
            val posY = 0.0

            val small = sketch {
                polygon(
                    Point2(literal(-0.2 + posX), literal(-0.2 + posY)),
                    Point2(literal(0.2 + posX), literal(-0.2 + posY)),
                    Point2(literal(0.2 + posX), literal(0.2 + posY)),
                    Point2(literal(-0.2 + posX), literal(0.2 + posY)),
                )
            }

            extrude(big, LiteralExpr(World(), 1.0))
            extrude(small, LiteralExpr(World(), 2.0))
        }

        val model = mapModel(project)

        println("test")

    }


    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
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
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
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


                equal(topLine.length, bottomLine.length)



                perpendicular(topLine, vertLine)



                equal(leftArc.radius, rightArc.radius)
                equal(topLine.length, rightArc.radius)

                equal(vertLine.length, vertLine2.length)
                equal(vertLine2.length, literal(2.0))

                equal(leftArc.center, vertLine.midPoint )
                equal(rightArc.center, vertLine2.midPoint )

                equal(leftArc.radius, topLine.length)
                equal(topLine.p0, origin())

                equal(topLine.p0.y, topLine.p1.y)

            }



            val posX = 0.0
            val posY = 0.0

            val witdth = 1
            val height = 1

            val small = sketch {
                polygon(
                    Point2(literal(-2 + posX), literal(-2.5 + posY)),
                    Point2(literal(2.5 + posX), literal(-2.5 + posY)),
                    Point2(literal(2.5 + posX), literal(0.1 + posY)),
                    Point2(literal(-2 + posX), literal(0.1 + posY)),
                )
            }

            extrude(small, LiteralExpr(World(),1.0))
            extrude(a, LiteralExpr(World(),0.2), DirectedPlane.from(Plane(Plane.XY.normal, 0.1)))
        }


        val model = mapModel(project)

        println("test")
    }



    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun fingerboard(){
        val project = project {
            val posX = 0.0
            val posY = 0.0

            val small = sketch {
                val rect = create(Rect::class)
                equal(rect.center, origin())
                equal(rect.width, literal(4))
                equal(rect.height, literal(5))
                equal(rect.top.p0.y, rect.top.p1.y)
            }

            val hole = sketch {
                val rect = create(RoundRect::class)
                equal(rect.center, literalPoint(0,1))
                equal(rect.width, literal(1))
                equal(rect.height, literal(1))
            }

            val hole2 = sketch {
                val rect = create(RoundRect::class)
                equal(rect.center, literalPoint(0,-1))
                equal(rect.width, literal(1))
                equal(rect.height, literal(2))
            }

            extrude(small, LiteralExpr(World(), 1.0))
            extrude(hole, LiteralExpr(World(), 0.8), DirectedPlane.from(Plane(Plane.XY.normal, -0.1), PointD(1.0)))
            extrude(hole2, LiteralExpr(World(), 0.8), DirectedPlane.from(Plane(Plane.XY.normal, -0.1), PointD(1.0)))

        }


        val model = mapModel(project)

        println("test")
    }

/*
    @Test
    @Timeout(1000, unit= TimeUnit.MILLISECONDS)
    fun routedTest(){
        val project = project {
            val big = sketch {
                polygon(
                    Point2(literal(-0.5), literal(-0.5)),
                    Point2(literal(0.5), literal(-0.5)),
                    Point2(literal(0.5), literal(0.5)),
                    Point2(literal(-0.5), literal(0.5)),
                )
            }

            val posX = 0.4
            val posY = 0.0

            val small = sketch {
                polygon(
                    Point2(literal(-0.2 + posX), literal(-0.2 + posY)),
                    Point2(literal(0.2 + posX), literal(-0.2 + posY)),
                    Point2(literal(0.2 + posX), literal(0.2 + posY)),
                    Point2(literal(-0.2 + posX), literal(0.2 + posY)),
                )
            }

            extrude(big, LiteralExpr(World(), 1.0))
            extrude(small, LiteralExpr(World(), 2.0), DirectedPlane.from(Plane(Plane.XY.normal, -0.1)))
        }

        val model = mapModel(project)

        println("test")
    }*/
}
