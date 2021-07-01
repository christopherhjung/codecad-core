
import com.angusj.clipper.Clipper
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

    fun line(a: Point, b: Point, type: LineType = LineType.Normal): Line {
        return sketch.createLine(a, b, type)
    }

    fun circle(center: Point, radius: Value): Circle {
        return sketch.createCircle(center, radius)
    }

    fun arc(center: Point, radius: Value, start: Value, end: Value): Circle {
        return sketch.createArc(center, radius, start, end)
    }

    fun polygon(vararg points: Point) : List<Line> {
        val points = mutableListOf(*points)
        points.add(points.first())
        var current = points.first()
        val lines = mutableListOf<Line>()
        for (point in points) {
            lines.add(line(current, point))
            current = point
        }

        return lines
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

    override fun hasNext(): Boolean {
        return i <= 1
    }

    override fun next(): Vector {
        val value = if(i == 0) line.a else line.b
        i++
        return value.toVector()
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

fun elementsToPath(elements: List<Element>) : Array<DoubleArray>{
    val path = mutableListOf<Double>()

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
            path.add(next.x)
            path.add(next.y)
        }
    }


    return arrayOf(path.toTypedArray().toDoubleArray())
}

fun Builder.offsetPolygons(input : Array<DoubleArray>, delta: Double, outerContour: Boolean = false): Array<DoubleArray> {
    val clipper = Clipper()
    val output = clipper.generatePath(2.0,0.01, input, delta)

    val lineType = if(outerContour) LineType.ToolContour else LineType.Construction

    for (path in output) {
        var last: Point? = null
        var first: Point? = null
        for (i in path.indices step 2) {
            val x = path[i]
            val y = path[i + 1]

            val thePoint = Point(const(x), const(y))

            if (last != null) {
                line(last, thePoint,  lineType)
            } else {
                first = thePoint
            }

            last = thePoint
        }


        if (last != null && first != null) {
            line(last, first,  lineType)
        }
    }

    return output
}

fun main(args: Array<String>) {

    val start = System.currentTimeMillis()
    val sketch = sketch {
        val poly = polygon(
            point(0.0, -2.0),
            point(1.0, -0.55),
            point(2.0, -2.0),
            point(2.0, 2.0),
            point(1.0, 0.55),
            point(0.0, 2.0),
        )

        val paths = offsetPolygons(elementsToPath(poly), -0.3)

        offsetPolygons(paths, 0.3, true)
        offsetPolygons(paths, -0.3, true)
    }


    sketch.draw()

    val end = System.currentTimeMillis()
    println("time need: ${end - start}")
}

