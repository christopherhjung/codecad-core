import Value.Companion.conditional
import kotlin.math.*

abstract class Constraint {

    var lineNumber: Int = -1
    var cache: Value? = null

    protected abstract fun equationImpl() : Value

    val equation: Value
        get() {
            if(cache == null) {
                cache = equationImpl()
            }

            return cache!!
        }

    open fun prune(sketch: Sketch) {
    }
}

//class Arc(val center: Point, val rad: Value, val start: Value, val end: Value) : Element

class PointOnPoint(val a: Point, val b: Point) : Constraint() {


    override fun equationImpl() : Value{
        return (a.x - b.x).pow(2) + (a.y - b.y).pow(2)
    }

    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(a.x, b.x)
        sketch.paramIsEquals(a.y, b.y)
    }
}

class PointToPointDistance(val a: Point, val b: Point, val distance: Value) : Constraint() {


    override fun equationImpl(): Value {
        return (a.x - b.x).pow(2) + (a.y - b.y).pow(2) - distance.pow(2)
    }
}

class PointOnLine(val point: Point, val line: Line) : Constraint() {
    /*override fun error(): Double {
        val dx = line.b.x.value - line.a.x.value
        val dy = line.b.y.value - line.a.y.value

        val m = dy / dx
        val n = dx / dy

        return if (m <= 1 && m >= -1) {
            //Calculate the expected y point given the x coordinate of the point
            val Ey = line.a.y.value + m * (point.x.value - line.a.x.value)
            (Ey - point.y.value).pow(2)
        } else {
            //Calculate the expected x point given the y coordinate of the point
            val Ex = line.a.x.value + n * (point.y.value - line.a.y.value)
            (Ex - point.x.value).pow(2)
        }
    }*/

    override fun equationImpl(): Value {
        TODO("Not yet implemented")
    }
}

class LineLength(val line: Line, val length: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (line.length() - length).pow(2)
    }
}

class EqualLength(val line1: Line, val line2: Line) : Constraint() {
    override fun equationImpl(): Value {
        return (line1.length() - line2.length()).pow(2)
    }
}

class Horizontal(val line: Line) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(line.a.y, line.b.y)
    }

    override fun equationImpl(): Value {
        val ody = line.b.y - line.a.y
        return ody * ody * 1000
    }
}

class Vertical(val line: Line) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(line.a.x, line.b.x)
    }

    override fun equationImpl(): Value {
        val ody = line.b.x - line.a.x
        return ody * ody * 1000
    }
}

data class Vector(val x: Double, val y: Double) {
    operator fun plus(right: Vector): Vector {
        return Vector(x + right.x, y + right.y)
    }

    operator fun minus(right: Vector): Vector {
        return Vector(x - right.x, y - right.y)
    }

    operator fun rangeTo(other: Vector): Double {
        return x * other.x + y * other.y
    }

    operator fun times(other: Vector): Double {
        return x * other.y - y * other.x
    }

    operator fun times(other: Double): Vector {
        return Vector(x * other, y * other)
    }

    fun squaredLength(): Double {
        return x * x + y * y
    }

    fun length(): Double {
        return sqrt(squaredLength())
    }
}

operator fun Double.times(right: Vector): Vector {
    return Vector(this * right.x, this * right.y)
}

class CircleTangent(val circle: Circle, val line: Line) : Constraint() {


    override fun equationImpl(): Value {
        /*val circleCenter = circle.center
        val lineStart = line.a
        val lineEnd = line.b
        val lineDirection = lineEnd - lineStart

        val distToCenter = circleCenter - lineStart

        val temp = ( distToCenter.scalarProduct(lineDirection) ) / ( lineDirection.scalarProduct(lineDirection) )
        val temp2 = lineDirection * temp
        val perpendicular = distToCenter - temp2

        val a = (perpendicular.squaredLength() - circle.rad.pow(2.0) ).pow(2.0)
        val b = ( perpendicular.scalarProduct(lineDirection) ).pow(2.0)

        return a + b*/

        val lineStart = line.a
        val lineEnd = line.b
        val lineDirection = lineEnd - lineStart

        val hyp = lineDirection.length()
        val hypRad = circle.rad / hyp

        val Rx =
            Point(circle.center.x - lineDirection.y * hypRad, circle.center.y + lineDirection.x * hypRad)
        val Ry =
            Point(circle.center.x + lineDirection.y * hypRad, circle.center.y - lineDirection.x * hypRad)

        val cross = lineStart * lineEnd
        var error1 = (lineDirection.vectorProduct(Rx) + cross) / hyp
        var error2 = (lineDirection.vectorProduct(Ry) + cross) / hyp
        error1 *= error1
        error2 *= error2
        return Value.min(error1, error2)
    }
}

class Perpendicular(val line1: Line, val line2: Line) : Constraint() {
    override fun equationImpl(): Value {
        return lineCross(line1, line2, false).pow(2)
    }
}

fun lineCross(line1: Line, line2: Line, cross: Boolean = true): Value {
    var dx = line1.b.x - line1.a.x
    var dy = line1.b.y - line1.a.y
    var dx2 = line2.b.x - line2.a.x
    var dy2 = line2.b.y - line2.a.y

    val hyp1 = line1.length()
    val hyp2 = line2.length()

    dx /= hyp1
    dy /= hyp1
    dx2 /= hyp2
    dy2 /= hyp2

    return if (cross) {
        dx * dy2 - dy * dx2
    } else {
        dx * dx2 + dy * dy2
    }
}

class Parallel(val line1: Line, val line2: Line) : Constraint() {
    override fun equationImpl(): Value {
        return lineCross(line1, line2).pow(2)
    }
}

class Colinear(val line1: Line, val line2: Line) : Constraint() {
    /*override fun error(): Double {
        var error = 0.0
        val dx = line1.b.x.value - line1.a.x.value
        val dy = line1.b.y.value - line1.a.y.value

        val m = dy / dx
        val n = dx / dy
        // Calculate the error between the expected intersection point
        // and the true point of the second lines two end points on the
        // first line
        if (m <= 1 && m > -1) {
            //Calculate the expected y point given the x coordinate of the point
            var Ey = line1.a.y.value + m * (line2.a.x.value - line1.a.x.value)
            error += (Ey - line2.a.y.value).pow(2.0)

            Ey = line1.a.y.value + m * (line2.b.x.value - line1.a.x.value)
            error += (Ey - line2.b.y.value).pow(2.0)
        } else {
            //Calculate the expected x point given the y coordinate of the point
            var Ex = line1.a.x.value + n * (line2.a.y.value - line1.a.y.value)
            error += (Ex - line2.a.x.value).pow(2.0)

            Ex = line1.a.x.value + n * (line2.b.y.value - line1.a.y.value)
            error += (Ex - line2.b.x.value).pow(2.0)
        }

        return error
    }*/

    override fun equationImpl(): Value {
        TODO("Not yet implemented")
    }
}

class PointOnCircle(val point: Point, val circle: Circle) : Constraint() {

    override fun equationImpl(): Value {
        val rad1 = circle.center.length(point)
        return (rad1 - circle.rad).pow(2)
    }
}

class Concentric(val circle1: Circle, val circle2: Circle) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(circle1.rad, circle2.rad)
    }

    override fun equationImpl(): Value {
        return circle1.center.squaredLength(circle2.center)
    }
}

fun pointOnArcError(point: Point, arc: Circle, angle: Value): Value {
    val x = (arc.center.x + arc.rad * Value.cos(angle))
    val y = (arc.center.y + arc.rad * Value.sin(angle))

    return (point.x - x).pow(2) + (point.y - y).pow(2)
}

class PointOnArcStart(val point: Point, val arc: Arc) : Constraint() {
    override fun equationImpl(): Value {
        return pointOnArcError(point, arc, arc.start)
    }
}

class PointOnArcEnd(val point: Point, val arc: Arc) : Constraint() {
    override fun equationImpl(): Value {
        return pointOnArcError(point, arc, arc.end)
    }
}

class PointOnLineMidpoint(val point: Point, val line: Line) : Constraint() {
    override fun equationImpl(): Value {
        val eX = (line.a.x + line.b.x) / 2
        val eY = (line.a.y + line.b.y) / 2
        return (eX - point.x).pow(2) + (eY - point.y).pow(2)
    }
}

class InternalAngle(val line1: Line, val line2: Line, val angle: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (lineCross(line1, line2, false) - CosValue(angle)).pow(2)
    }
}

class Radius(val circle: Circle, val radius: Value) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(circle.rad, radius)
    }

    override fun equationImpl(): Value {
        return (radius - circle.rad).pow(2)
    }
}

class Equals(val left: Value, val right: Value) : Constraint() {


    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(left, right)
    }

    override fun equationImpl(): Value {
        return (left - right).pow(2)
    }
}
