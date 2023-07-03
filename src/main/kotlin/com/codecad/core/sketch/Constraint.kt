package com.codecad.core.sketch

import com.codecad.core.Circle
import com.codecad.core.CircleLike
import com.codecad.core.Segment2
import com.codecad.core.Vec2
import com.codecad.core.ast.primitive.Expr

abstract class Constraint {
    var cache: Expr? = null

    protected abstract fun equationImpl() : Expr

    val equation: Expr
        get() {
            if(cache == null) {
                cache = equationImpl()
            }

            return cache!!
        }
}

//class com.codecad.core.Arc(val center: com.codecad.core.Point, val rad: com.codecad.core.Expr, val start: com.codecad.core.Expr, val end: com.codecad.core.Expr) : Element

class PointOnPoint(val a: Vec2, val b: Vec2) : Constraint() {
    override fun equationImpl() : Expr {
        return ((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
    }
}

class PointToPointDistance(val a: Vec2, val b: Vec2, val distance: Expr) : Constraint() {
    override fun equationImpl(): Expr {
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

    override fun equationImpl(): com.codecad.core.Expr {
        TODO("Not yet implemented")
    }
}
*/
/*
class LineLength(val line: Segment2, val length: Expr) : Constraint() {
    override fun equationImpl(): Expr {
        return Expr.abs(line.length - length)
    }
}

class EqualLength(val line1: Segment2, val line2: Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        return Expr.abs(line1.length - line2.length)
    }
}*/

class Horizontal(val line: Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        val direction = line.p1 - line.p0
        val angle = Expr.asin(direction.y / direction.length())
        return Expr.abs(angle)
    }
}

class Vertical(val line: Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        val direction = line.p1 - line.p0
        val angle = Expr.asin(direction.x / direction.length())
        return Expr.abs(angle)
    }
}

class CircleTangent(val circle: CircleLike, val line : Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        val lineDirection = line.p1 - line.p0
        val centerLineDirection = circle.center - line.p0
        val offset = Expr.abs(lineDirection.cross(centerLineDirection)) / lineDirection.length()
        val test = Expr.abs(offset - circle.radius)

        return test
    }
}

fun pointAlongLine(line: Segment2, r: Expr): Vec2 {
    return line.p0 + line.difference * r
}

fun projectionFactorBetween(line: Segment2, point: Vec2): Expr {
    val dx = line.p0.x - line.p1.x
    val dy = line.p0.y - line.p1.y
    val len2 = dx * dx + dy * dy
    return -((point.x - line.p0.x) * dx + (point.y - line.p0.y) * dy) / len2
}

fun projectOntoLine(line: Segment2, point: Vec2): Vec2 {
    val r = projectionFactorBetween(line, point)
    return pointAlongLine(line, r)
}

fun distanceBetweenPoints(p0: Vec2, p1: Vec2): Expr {
    val dx = p0.x - p1.x
    val dy = p0.y - p1.y
    return (dx * dx + dy * dy).sqrt()
}

class Perpendicular(val line1: Segment2, val line2: Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        return Expr.abs(line1.direction.normalized().scalar(line2.direction.normalized()))
    }
}
/*
fun lineCross(line1: com.codecad.core.Line, line2: com.codecad.core.Line, cross: Boolean = true): com.codecad.core.Expr {
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

class Parallel(val line1: Segment2, val line2: Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        return line1.direction.cross(line2.direction).pow(2)
    }
}

class Colinear(val line1: Segment2, val line2: Segment2) : Constraint() {
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

    override fun equationImpl(): Expr {
        TODO("Not yet implemented")
    }
}

class PointOnCircle(val point: Vec2, val circle: Circle) : Constraint() {

    override fun equationImpl(): Expr {
        val rad1 = circle.center.length(point)
        return (rad1 - circle.radius).pow(2)
    }
}

class PointOnLine(val point: Vec2, val line: Segment2) : Constraint() {

    override fun equationImpl(): Expr {
        return (line.p0 - point).normalized().cross(line.direction).pow(2)
    }
}

class Concentric(val circle1: Circle, val circle2: Circle) : Constraint() {
    override fun equationImpl(): Expr {
        return circle1.center.squaredLength(circle2.center)
    }
}

class PointOnLineMidpoint(val point: Vec2, val line: Segment2) : Constraint() {
    override fun equationImpl(): Expr {
        return (line.midPoint - point).squaredLength()
    }
}

class InternalAngle(val line1: Segment2, val line2: Segment2, val angle: Expr) : Constraint() {
    override fun equationImpl(): Expr {
        return (line1.direction.scalar(line2.direction) - Expr.cos(angle)).pow(2)
    }
}

class Equals(val left: Expr, val right: Expr) : Constraint() {
    override fun equationImpl(): Expr {
        return (left - right).pow(2)
    }
}
