package com.codecad.core.constraint

import com.codecad.core.SketchConic
import com.codecad.core.SketchSegment
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr

abstract class Constraint {
    var cache: Expr? = null

    protected abstract fun equationImpl() : Expr

    val equation: Expr
        get() {
            return cache ?: run{
                val eq = equationImpl()
                cache = eq
                eq
            }
        }
}

class Minimize(val expr: Expr) : Constraint() {
    override fun equationImpl() : Expr {
        return expr
    }
}

class PointOnPoint(val a: Vec2Expr, val b: Vec2Expr) : Constraint() {
    override fun equationImpl() : Expr {
        return ((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
    }
}

class PointToPointDistance(val a: Vec2Expr, val b: Vec2Expr, val distance: Expr) : Constraint() {
    override fun equationImpl(): Expr {
        return (a.x - b.x).pow(2) + (a.y - b.y).pow(2) - distance.pow(2)
    }
}

class Horizontal(val line: SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        val direction = line.p1 - line.p0
        val angle = Expr.asin(direction.y / direction.length())
        return angle.pow(2.0)
    }
}

class Vertical(val line: SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        val direction = line.p1 - line.p0
        val angle = Expr.asin(direction.x / direction.length())
        return angle.pow(2.0)
    }
}

class CircleTangent(val circle: SketchConic, val line : SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        val lineDirection = line.p1 - line.p0
        val centerLineDirection = circle.center - line.p0
        val offset = Expr.abs(lineDirection.crossZ(centerLineDirection)) / lineDirection.length()
        return (offset - circle.radius).pow(2.0)
    }
}

fun pointAlongLine(line: SketchSegment, r: Expr): Vec2Expr {
    return line.p0 + line.difference * r
}

fun projectionFactorBetween(line: SketchSegment, point: Vec2Expr): Expr {
    val dx = line.p0.x - line.p1.x
    val dy = line.p0.y - line.p1.y
    val len2 = dx * dx + dy * dy
    return -((point.x - line.p0.x) * dx + (point.y - line.p0.y) * dy) / len2
}

fun projectOntoLine(line: SketchSegment, point: Vec2Expr): Vec2Expr {
    val r = projectionFactorBetween(line, point)
    return pointAlongLine(line, r)
}

fun distanceBetweenPoints(p0: Vec2Expr, p1: Vec2Expr): Expr {
    val dx = p0.x - p1.x
    val dy = p0.y - p1.y
    return (dx * dx + dy * dy).sqrt()
}

class Perpendicular(val line1: SketchSegment, val line2: SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        return (line1.direction.normalized().dot(line2.direction.normalized())).pow(2)
    }
}

class Parallel(val line1: SketchSegment, val line2: SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        return line1.direction.crossZ(line2.direction).pow(2)
    }
}

class Colinear(val line1: SketchSegment, val line2: SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        return line1.direction.crossZ(line2.p0 - line2.p0).pow(2) +
                line2.direction.crossZ(line1.p1 - line1.p1).pow(2)
    }
}

class PointOnCircle(val point: Vec2Expr, val circle: SketchConic) : Constraint() {

    override fun equationImpl(): Expr {
        val rad1 = circle.center.distance(point)
        return (rad1 - circle.radius).pow(2)
    }
}

class PointOnLine(val point: Vec2Expr, val line: SketchSegment) : Constraint() {

    override fun equationImpl(): Expr {
        return (line.p0 - point).normalized().crossZ(line.direction).pow(2)
    }
}

class Concentric(val circle1: SketchConic, val circle2: SketchConic) : Constraint() {
    override fun equationImpl(): Expr {
        return circle1.center.squaredDistance(circle2.center)
    }
}

class PointOnLineMidpoint(val point: Vec2Expr, val line: SketchSegment) : Constraint() {
    override fun equationImpl(): Expr {
        return (line.midPoint - point).squaredLength()
    }
}

class InternalAngle(val line1: SketchSegment, val line2: SketchSegment, val angle: Expr) : Constraint() {
    override fun equationImpl(): Expr {
        return (line1.direction.dot(line2.direction) - Expr.cos(angle)).pow(2)
    }
}

class Equals(val left: Expr, val right: Expr) : Constraint() {
    override fun equationImpl(): Expr {
        return (left - right).pow(2)
    }
}
