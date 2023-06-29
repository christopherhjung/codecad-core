package com.codecad.core

import com.codecad.common.PointD
import com.codecad.core.sketch.Expr

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}

abstract class Figure(var type: LineType = LineType.Normal){
}

class LineSegment(val p0: Point, val p1: Point, type: LineType = LineType.Normal) : Figure(type){

    val squaredLength : Expr
        get() = ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))

    val length : Expr
        get() = squaredLength.sqrt()

    val midPoint : Point
        get() = (p0 + p1) / 2.0

    val difference : Point
        get() = p1 - p0

    val direction : Point
        get() = difference.normalized()
}

open class Circle(val center: Point, val radius: Expr) : Figure()

class Arc(val p0: Point, val p1: Point, val helper: Expr) : Circle( centerFunction(p0,p1,helper), (p0 -  centerFunction(p0,p1,helper)).length()){
    companion object{
        private fun centerFunction(p0: Point, p1: Point, arcRadius: Expr): Point {
            val direction = p1 - p0
            val half = direction / 2.0
            val middle = p0 + half
            val normalizedDirection = direction.normalized()
            val positive = Point(-normalizedDirection.y, normalizedDirection.x)
            return middle + positive * arcRadius
        }
    }
}

class FunctionFigure( val function: (Expr) -> Point) : Figure(){

}

class Point(val x: Expr, val y: Expr, type: LineType = LineType.Normal) : Figure(type) {
    companion object{
        fun onCircle(center: Point, radius: Expr, angle: Expr) : Point {
            return Point(
                (center.x + radius * Expr.cos(angle)),
                (center.y + radius * Expr.sin(angle))
            )
        }

        fun ifExpr(condition: Expr, left: Point, right: Point) : Point {
            return Point(
                Expr.ifExpr(condition, left.x, right.x),
                Expr.ifExpr(condition, left.y, right.y)
            )
        }
    }

    fun absoluteAngle(target: Point) : Double{
        val a = target.x.world.AXIS_X.p1
        val b = target - this
        return kotlin.math.atan2((a.x * b.y - a.y * b.x).evalDouble(), (a.x * b.x + a.y * b.y).evalDouble())
    }

    fun normalized() : Point {
        return this / length()
    }

    fun rotate(center: Point, angle: Expr) : Point {
        val a = Expr.sin(angle)
        val b = Expr.cos(angle)

        return Point(
            b * ( x - center.x) - a * (y - center.y) + center.x,
            a * ( x - center.x) + b * ( y - center.y) + center.y
        )
    }

    fun copy(): Point {
        return Point(x.evalLiteral(), y.evalLiteral())
    }

    fun fixed() : PointD {
        return PointD(x.evalDouble(), y.evalDouble())
    }

    fun scalar(other: Point) : Expr {
        return x * other.x + y * other.y
    }

    fun cross(other: Point) : Expr {
        return x * other.y - y * other.x
    }

    operator fun times(other: Point) : Expr {
        return x * other.y - y * other.x
    }

    operator fun times(other: Expr) : Point {
        return Point(x * other, y * other)
    }

    operator fun times(other: Double) : Point {
        val value = x.world.literal(other)
        return Point(x * value, y * value)
    }

    operator fun div(other: Expr) : Point {
        return Point(x / other, y / other)
    }

    operator fun div(other: Double) : Point {
        val value = x.world.literal(other)
        return Point(x / value, y / value)
    }

    operator fun plus(right: Point) : Point {
        return Point(x + right.x, y + right.y)
    }

    operator fun minus(right: Point) : Point {
        return Point(x - right.x, y - right.y)
    }

    operator fun minus(right: Expr) : Point {
        return Point(x - right, y - right)
    }

    fun squaredLength(): Expr {
        return x.pow(2) + y.pow(2)
    }

    fun length(): Expr {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: Point): Expr {
        return (x - other.x).pow(2) + (y-other.y).pow(2)
    }

    fun length(other: Point): Expr {
        return squaredLength(other).sqrt()
    }

    override fun toString(): String {
        return "com.codecad.core.Point(x=$x, y=$y)"
    }
}
