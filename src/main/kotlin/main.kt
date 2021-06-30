import de.lighti.clipper.Clipper
import de.lighti.clipper.ClipperOffset
import de.lighti.clipper.Path
import de.lighti.clipper.Paths
import de.lighti.clipper.Point.LongPoint
import kotlin.math.cos
import kotlin.math.sin

class Builder {
    val sketch = Sketch()

    val deg = 0
    val rad = 1

    infix fun Double.unit(other: Int): Double {
        return if (other == deg) {
            Math.toRadians(this)
        } else {
            this
        }
    }

    fun param(value: Double = 0.0): ProxyValue {
        return sketch.createParameter(value)
    }

    fun const(value: Double = 0.0): Parameter {
        return sketch.createConst(value)
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0): Point {
        return sketch.createConstPoint(x, y)
    }

    fun point(x: Double = 0.0, y: Double = 0.0): Point {
        return sketch.createPoint(x, y)
    }

    fun line(a: Point, b: Point): Line {
        return sketch.createLine(a, b)
    }

    fun circle(center: Point, radius: Value): Circle {
        return sketch.createCircle(center, radius)
    }

    fun arc(center: Point, radius: Value, start: Value, end: Value): Circle {
        return sketch.createArc(center, radius, start, end)
    }

    fun polygon(vararg points: Point) {
        val points = mutableListOf(*points)
        points.add(points.first())
        var current = points.first()
        for (point in points) {
            line(current, point)
            current = point
        }
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): Line {
        return sketch.createLine(x, y, x2, y2)
    }

    fun tangent(circle: Circle, line: Line) {
        sketch.addConstraint(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Point, line: Line) {
        sketch.addConstraint(PointOnLineMidpoint(point, line))
    }

    fun horizontal(line: Line) {
        sketch.addConstraint(Horizontal(line))
    }

    fun vertical(line: Line) {
        sketch.addConstraint(Vertical(line))
    }

    fun pointOnCircle(point: Point, circle: Circle) {
        sketch.addConstraint(PointOnCircle(point, circle))
    }

    fun equalLength(line1: Line, line2: Line) {
        sketch.addConstraint(EqualLength(line1, line2))
    }

    fun angle(line1: Line, line2: Line, angle: Value) {
        sketch.addConstraint(InternalAngle(line1, line2, angle))
    }

    fun pointOnArcStart(point: Point, arc: Circle) {
        sketch.addConstraint(PointOnArcStart(point, arc))
    }

    fun pointOnArcEnd(point: Point, arc: Circle) {
        sketch.addConstraint(PointOnArcEnd(point, arc))
    }

    fun solve() {
        sketch.solve()
    }
}


fun sketch(init: Builder.() -> Unit): Sketch {
    val builder = Builder()
    builder.init()
    return builder.sketch
}

/*
fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}

fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}*/

interface Stepper {
    fun next(): Vector
    fun hasNext(): Boolean
}

class LineStepper(val line: Line) : Stepper {
    var i = 0
    var max = 100

    val start = line.a.toVector()
    val end = line.b.toVector()

    val diff = end - start

    override fun hasNext(): Boolean {
        return i <= 100
    }

    override fun next(): Vector {
        val value = start + diff * (i / 100.0)
        i++
        return value
    }
}

class ArcStepper(val arc: Circle) : Stepper {
    var i = 0
    var max = 100

    val start = arc.end!!.value
    val end = arc.start!!.value

    val diff = end - start

    override fun hasNext(): Boolean {
        return i <= 100
    }

    override fun next(): Vector {
        val currentAngle = start + diff * (i / 100.0)
        val x = (arc.center.x.value + arc.rad.value * cos(currentAngle))
        val y = (arc.center.y.value + arc.rad.value * sin(currentAngle))
        i++
        return Vector(x, y)
    }
}

fun Builder.offsetPolygons(elements: List<Element>, delta: Double): List<Line> {

    val path = Path()

    for (element in elements) {
        val stepper: Stepper = if (element is Line) {
            LineStepper(element)
        } else if (element is Circle) {
            ArcStepper(element)
        } else {
            throw RuntimeException()
        }

        while (stepper.hasNext()) {
            val next = stepper.next()

            path.add(LongPoint((next.x * 1000).toLong(), (next.y * 1000).toLong()))
        }
    }


    val offset = ClipperOffset()

    offset.addPath(path, Clipper.JoinType.ROUND, Clipper.EndType.CLOSED_POLYGON)

    val paths = Paths()

    offset.execute(paths, delta * 1000)

    val lines = mutableListOf<Line>()

    var last: Point? = null
    var first: Point? = null
    for (path in paths) {
        for (point in path) {
            val thePoint = point(point.x / 1000.0, point.y / 1000.0)

            if (last != null) {
                lines.add(line(last, thePoint))
            } else {
                first = thePoint
            }

            last = thePoint
        }
    }

    if (last != null && first != null) {
        lines.add(line(last, first))
    }

    return lines
}

fun main(args: Array<String>) {

    val clipperOffset = ClipperOffset()

    val path = Path()


    val start = System.currentTimeMillis()
    val sketch = sketch {
        /*val A = constPoint(0.0,0.0)
        val B = constPoint(1.0,1.0)
        val C = constPoint(1.0, 0.0)
        val arc = arc(constPoint(1.0,0.5), param(1.0), param(0.0 unit deg), param(180.0 unit deg))

        val lineA = line(A, B)
        val lineB = line(C, A)

        pointOnArcStart(C,arc)
        pointOnArcEnd(B, arc)*/

        polygon(
            point(0.0, 0.0),
            point(2.0, 0.0),
            point(2.0, 2.0),
            point(1.0, 1.0),
            point(0.0, 2.0),
        )


        val list = ArrayList<Element>()
        for (element in sketch.elements) {
            if (element is Line || element is Circle) {
                list.add(element)
            }
        }

        val offsetLines = offsetPolygons(list, -0.3)


        //offsetPolygons(offsetLines, 0.3)

        //angle(lineA, lineB, const(179.0 unit deg))
    }


    sketch.draw()


    val end = System.currentTimeMillis()
    println("time need: ${end - start}")


}

