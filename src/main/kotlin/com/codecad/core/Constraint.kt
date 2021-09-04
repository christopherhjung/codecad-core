package com.codecad.core

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

//class com.codecad.core.Arc(val center: com.codecad.core.Point, val rad: com.codecad.core.Value, val start: com.codecad.core.Value, val end: com.codecad.core.Value) : Element

class PointOnPoint(val a: Point, val b: Point) : Constraint() {


    override fun equationImpl() : Value {
        return ((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
    }

    override fun prune(sketch: Sketch) {
        sketch.merge(a.x, b.x)
        sketch.merge(a.y, b.y)
    }
}

class PointToPointDistance(val a: Point, val b: Point, val distance: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (a.x - b.x).pow(2) + (a.y - b.y).pow(2) - distance.pow(2)
    }
}
/*
class com.codecad.core.PointOnLine(val point: com.codecad.core.Point, val line: com.codecad.core.Line) : com.codecad.core.Constraint() {
    /*override fun error(): Double {
        val dx = line.b.x.value - line.a.x.value
        val dy = line.b.y.value - line.a.y.value

        val m = dy / dx
        val n = dx / dy

        return if (m <= 1 && m >= -1) {
            //Calculate the expected y point given the x coordinate of the point
            val Ey = line.a.y.value + m * (point.x.value - line.a.x.value)
            (Ey - point.y.value).com.codecad.core.pow(2)
        } else {
            //Calculate the expected x point given the y coordinate of the point
            val Ex = line.a.x.value + n * (point.y.value - line.a.y.value)
            (Ex - point.x.value).com.codecad.core.pow(2)
        }
    }*/

    override fun equationImpl(): com.codecad.core.Value {
        TODO("Not yet implemented")
    }
}
*/
class LineLength(val line: LineSegment, val length: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (line.length - length).pow(2)
    }
}

class EqualLength(val line1: LineSegment, val line2: LineSegment) : Constraint() {
    override fun equationImpl(): Value {
        return (line1.length - line2.length).pow(2)
    }
}

class Horizontal(val line: LineSegment) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(line.p0.y, line.p1.y)
    }

    override fun equationImpl(): Value {
        val direction = line.p1 - line.p0
        val angle = ArcSinValue(direction.y / direction.length())
        return angle.pow(2)
    }
}

class Vertical(val line: LineSegment) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(line.p0.x, line.p1.x)
    }

    override fun equationImpl(): Value {
        val direction = line.p1 - line.p0
        val angle = ArcSinValue(direction.x / direction.length())
        return angle.pow(2)
    }
}

class CircleTangent(val circle: Circle, val line: LineSegment) : Constraint() {
    override fun equationImpl(): Value {
        val direction = line.p1 - line.p0
        val distanceCenter = line.p0 - circle.center
        val offset = Value.abs(direction.cross(distanceCenter)) / direction.length()
        val error = (offset - circle.radius).pow(2)
        return error
    }
}

fun pointAlongLine(line: LineSegment, r: Value): Point {
    return line.p0 + line.difference * r
}

fun projectionFactorBetween(line: LineSegment, point: Point): Value {
    val dx = line.p0.x - line.p1.x
    val dy = line.p0.y - line.p1.y
    val len2 = dx * dx + dy * dy;
    return -((point.x - line.p0.x) * dx + (point.y - line.p0.y) * dy) / len2
}

fun projectOntoLine(line: LineSegment, point: Point): Point {
    val r = projectionFactorBetween(line, point)
    return pointAlongLine(line, r)
}

fun distanceBetweenPoints(p0: Point, p1: Point): Value {
    val dx = p0.x - p1.x;
    val dy = p0.y - p1.y;
    return (dx * dx + dy * dy).sqrt();
}

class Perpendicular(val line1: LineSegment, val line2: LineSegment) : Constraint() {
    override fun equationImpl(): Value {
        return line1.direction.scalar(line2.direction).pow(2)
    }
}
/*
fun lineCross(line1: com.codecad.core.Line, line2: com.codecad.core.Line, cross: Boolean = true): com.codecad.core.Value {
    var dx = line1.p1.x - line1.p0.x
    var dy = line1.p1.y - line1.p0.y
    var dx2 = line2.p1.x - line2.p0.x
    var dy2 = line2.p1.y - line2.p0.y

    val hyp1 = line1.length
    val hyp2 = line2.length

    dx /= hyp1
    dy /= hyp1
    dx2 /= hyp2
    dy2 /= hyp2

    return if (cross) {
        dx * dy2 - dy * dx2
    } else {
        dx * dx2 + dy * dy2
    }
}*/

class Parallel(val line1: LineSegment, val line2: LineSegment) : Constraint() {
    override fun equationImpl(): Value {
        return line1.direction.cross(line2.direction).pow(2)
    }
}

class Colinear(val line1: LineSegment, val line2: LineSegment) : Constraint() {
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
            error += (Ey - line2.a.y.value).com.codecad.core.pow(2.0)

            Ey = line1.a.y.value + m * (line2.b.x.value - line1.a.x.value)
            error += (Ey - line2.b.y.value).com.codecad.core.pow(2.0)
        } else {
            //Calculate the expected x point given the y coordinate of the point
            var Ex = line1.a.x.value + n * (line2.a.y.value - line1.a.y.value)
            error += (Ex - line2.a.x.value).com.codecad.core.pow(2.0)

            Ex = line1.a.x.value + n * (line2.b.y.value - line1.a.y.value)
            error += (Ex - line2.b.x.value).com.codecad.core.pow(2.0)
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
        return (rad1 - circle.radius).pow(2)
    }
}

class PointOnLine(val point: Point, val line: LineSegment) : Constraint() {

    override fun equationImpl(): Value {
        return (line.p0 - point).normalized().cross(line.direction).pow(2)
    }
}

class Concentric(val circle1: Circle, val circle2: Circle) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(circle1.radius, circle2.radius)
    }

    override fun equationImpl(): Value {
        return circle1.center.squaredLength(circle2.center)
    }
}

class PointOnLineMidpoint(val point: Point, val line: LineSegment) : Constraint() {
    override fun equationImpl(): Value {
        return (line.midPoint - point).squaredLength()
    }
}

class InternalAngle(val line1: LineSegment, val line2: LineSegment, val angle: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (line1.direction.scalar(line2.direction) - CosValue(angle)).pow(2)
    }
}

class Radius(val circle: Circle, val radius: Value) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(circle.radius, radius)
    }

    override fun equationImpl(): Value {
        return (radius - circle.radius).pow(2)
    }
}

class Equals(val left: Value, val right: Value) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(left, right)
    }

    override fun equationImpl(): Value {
        return (left - right).pow(2)
    }
}
