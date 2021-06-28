import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

abstract class Constraint {

    abstract fun error(): Double

    open fun prune(sketch : Sketch){

    }
}

abstract class Value{
    abstract var value : Double

    override fun toString(): String {
        return value.toString()
    }
}

class Parameter(_value: Double) : Value(){
    override var value: Double = _value
}

class ProxyValue(var proxy: Value) : Value(){
    override var value: Double
        get() = proxy.value
        set(value) {proxy.value = value}
}



interface Element{

}

data class Point(val x: Value, val y: Value) : Element {
    fun toVector(): Vector {
        return Vector(x.value, y.value)
    }
}


data class Line(val a: Point, val b: Point) : Element

class Circle(val center: Point, val rad: Value) : Element

class Arc(val center: Point, val rad: Value, val start: Value, val end: Value) : Element

class PointOnPoint(val a: Point, val b: Point) : Constraint() {
    override fun error(): Double {
        return (a.x.value - b.x.value).pow(2) + (a.y.value - b.y.value).pow(2)
    }

    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(a.x, b.x)
        sketch.paramIsEquals(b.y, b.y)
    }
}

class PointToPointDistance(val a: Point, val b: Point, val distance: Value) : Constraint() {
    override fun error(): Double {
        return (a.x.value - b.x.value).pow(2) + (a.y.value - b.y.value).pow(2) - distance.value.pow(2)
    }
}

class PointOnLine(val point: Point, val line: Line) : Constraint() {
    override fun error(): Double {
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
    }
}

class LineLength(val line: Line, val length: Value) : Constraint() {
    override fun error(): Double {
        val temp =
            sqrt((line.b.x.value - line.a.x.value).pow(2) + (line.b.y.value - line.a.y.value).pow(2)) - length.value
        return temp * temp * 100
    }
}

class EqualLength(val line1: Line, val line2: Line) : Constraint() {
    override fun error(): Double {
        val temp = hypot(
            line1.b.x.value - line1.a.x.value,
            line1.b.y.value - line1.a.y.value
        ) - hypot(
            line2.b.x.value - line2.a.x.value,
            line2.b.y.value - line2.a.y.value
        )
        return temp * temp
    }
}

class Horizontal(val line: Line) : Constraint() {
    override fun error(): Double {
        val ody = line.b.y.value - line.a.y.value
        return ody * ody * 1000
    }

    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(line.a.y, line.b.y)
    }

}

class Vertical(val line: Line) : Constraint() {
    override fun error(): Double {
        val ody = line.b.x.value - line.a.x.value
        return ody * ody * 1000
    }

    override fun prune(sketch: Sketch) {
        sketch.paramIsEquals(line.a.x, line.b.x)
    }
}

class Vector(val x: Double, val y: Double) {
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
    override fun error(): Double {
        /*val circleCenter = circle.center.toVector()
        val lineStart = line.a.toVector()
        val lineEnd = line.b.toVector()
        val lineDirection = lineEnd - lineStart

        val distToCenter = circleCenter - lineStart

        val perpendicular = distToCenter - ( distToCenter .. lineDirection ) / ( lineDirection .. lineDirection ) * lineDirection


        val a = (perpendicular.squaredLength() - circle.rad.value.pow(2.0) ).pow(2.0)
        val b = ( perpendicular .. lineDirection ).pow(2.0)


        return a + b*/
        val lineStart = line.a.toVector()
        val lineEnd = line.b.toVector()
        val lineDirection = lineEnd - lineStart

        val hyp = lineDirection.length()
        val hypRad = circle.rad.value / hyp

        val Rx =
            Vector(circle.center.x.value - lineDirection.y * hypRad, circle.center.y.value + lineDirection.x * hypRad)
        val Ry =
            Vector(circle.center.x.value + lineDirection.y * hypRad, circle.center.y.value - lineDirection.x * hypRad)

        val cross = lineStart * lineEnd

        var error1 = (lineDirection * Rx + cross) / hyp
        var error2 = (lineDirection * Ry + cross) / hyp
        error1 *= error1
        error2 *= error2
        return if (error1 < error2) error1 else error2
    }
}

class Perpendicular(val line1: Line, val line2: Line) : Constraint() {
    override fun error(): Double {
        var dx = line1.b.x.value - line1.a.x.value
        var dy = line1.b.y.value - line1.a.y.value
        var dx2 = line2.b.x.value - line2.a.x.value
        var dy2 = line2.b.y.value - line2.a.y.value

        val hyp1 = hypot(dx, dy)
        val hyp2 = hypot(dx2, dy2)

        dx /= hyp1
        dy /= hyp1
        dx2 /= hyp2
        dy2 /= hyp2

        val temp = dx * dx2 + dy * dy2
        return temp * temp
    }
}

class Parallel(val line1: Line, val line2: Line) : Constraint() {
    override fun error(): Double {
        var dx = line1.b.x.value - line1.a.x.value
        var dy = line1.b.y.value - line1.a.y.value
        var dx2 = line2.b.x.value - line2.a.x.value
        var dy2 = line2.b.y.value - line2.a.y.value

        val hyp1 = hypot(dx, dy)
        val hyp2 = hypot(dx2, dy2)

        dx /= hyp1
        dy /= hyp1
        dx2 /= hyp2
        dy2 /= hyp2

        val temp = dy * dx2 - dx * dy2
        return temp * temp
    }
}

class Colinear(val line1: Line, val line2: Line) : Constraint() {
    override fun error(): Double {
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
    }
}

class PointOnCircle(val point: Point, val circle: Circle) : Constraint() {
    override fun error(): Double {
        //see what the current radius to the point is
        val rad1 = hypot(circle.center.x.value - point.x.value, circle.center.y.value - point.y.value)
        //Compare this radius to the radius of the circle, return the error squared
        val temp = rad1 - circle.rad.value
        return temp * temp
    }
}

class Concentric(val circle1: Circle, val circle2: Circle) : Constraint() {
    override fun error(): Double {
        val temp = hypot(
            circle1.center.x.value - circle2.center.x.value,
            circle1.center.y.value - circle2.center.y.value
        )
        return temp * temp
    }
}

class PointOnLineMidpoint(val point: Point, val line: Line) : Constraint() {
    override fun error(): Double {
        val eX = (line.a.x.value + line.b.x.value) / 2
        val eY = (line.a.y.value + line.b.y.value) / 2
        val temp = eX - point.x.value
        val temp2 = eY - point.y.value
        return temp * temp + temp2 * temp2
    }
}

class InternalAngle(val line1: Line, val line2: Line, val angle: Value) : Constraint() {
    override fun error(): Double {
        var dx = line1.b.x.value - line1.a.x.value
        var dy = line1.b.y.value - line1.a.y.value
        var dx2 = line2.b.x.value - line2.a.x.value
        var dy2 = line2.b.y.value - line2.a.y.value

        val hyp1 = hypot(dx, dy)
        val hyp2 = hypot(dx2, dy2)

        dx /= hyp1
        dy /= hyp1
        dx2 /= hyp2
        dy2 /= hyp2

        val temp = dx * dx2 + dy * dy2
        val temp2 = cos(angle.value)
        return (temp + temp2) * (temp + temp2)
    }
}
