package com.codecad.core

import com.codecad.common.PointD
import com.codecad.core.sketch.Expr

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}

abstract class Figure()

class Segment2(val p0: Point2, val p1: Point2) : Figure(){
    val squaredLength : Expr
        get() = ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))

    val length : Expr
        get() = squaredLength.sqrt()

    val midPoint : Point2
        get() = (p0 + p1) / 2.0

    val difference : Point2
        get() = p1 - p0

    val direction : Point2
        get() = difference.normalized()
}

open class Circle(val center: Point2, val radius: Expr) : Figure()

class Arc(val p0: Point2, val p1: Point2, val helper: Expr) : Circle( centerFunction(p0,p1,helper), (p0 -  centerFunction(p0,p1,helper)).length()){
    companion object{
        private fun centerFunction(p0: Point2, p1: Point2, arcRadius: Expr): Point2 {
            val direction = p1 - p0
            val half = direction / 2.0
            val middle = p0 + half
            val normalizedDirection = direction.normalized()
            val positive = Point2(-normalizedDirection.y, normalizedDirection.x)
            return middle + positive * arcRadius
        }
    }
}

class FunctionFigure( val function: (Expr) -> Point2) : Figure(){

}

class Point2(val x: Expr, val y: Expr) : Figure() {

    fun absoluteAngle(target: Point2) : Double{
        val a = target.x.world.AXIS_X.p1
        val b = target - this
        return kotlin.math.atan2((a.x * b.y - a.y * b.x).evalDouble(), (a.x * b.x + a.y * b.y).evalDouble())
    }

    fun normalized() : Point2 {
        return this / length()
    }

    fun rotate(center: Point2, angle: Expr) : Point2 {
        val a = Expr.sin(angle)
        val b = Expr.cos(angle)

        return Point2(
            b * ( x - center.x ) - a * ( y - center.y ) + center.x,
            a * ( x - center.x ) + b * ( y - center.y ) + center.y
        )
    }

    fun copy(): Point2 {
        return Point2(x.evalLiteral(), y.evalLiteral())
    }

    fun fixed() : PointD {
        return PointD(x.evalDouble(), y.evalDouble())
    }

    fun scalar(other: Point2) : Expr {
        return x * other.x + y * other.y
    }

    fun cross(other: Point2) : Expr {
        return x * other.y - y * other.x
    }

    operator fun times(other: Point2) : Expr {
        return x * other.y - y * other.x
    }

    operator fun times(other: Expr) : Point2 {
        return Point2(x * other, y * other)
    }

    operator fun times(other: Double) : Point2 {
        val value = x.world.literal(other)
        return Point2(x * value, y * value)
    }

    operator fun div(other: Expr) : Point2 {
        return Point2(x / other, y / other)
    }

    operator fun div(other: Double) : Point2 {
        val value = x.world.literal(other)
        return Point2(x / value, y / value)
    }

    operator fun plus(right: Point2) : Point2 {
        return Point2(x + right.x, y + right.y)
    }

    operator fun minus(right: Point2) : Point2 {
        return Point2(x - right.x, y - right.y)
    }

    operator fun minus(right: Expr) : Point2 {
        return Point2(x - right, y - right)
    }

    fun squaredLength(): Expr {
        return x.pow(2) + y.pow(2)
    }

    fun length(): Expr {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: Point2): Expr {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun length(other: Point2): Expr {
        return squaredLength(other).sqrt()
    }

    override fun toString(): String {
        return "com.codecad.core.Point(x=$x, y=$y)"
    }

    companion object{
        fun onCircle(center: Point2, radius: Expr, angle: Expr) : Point2 {
            return Point2(
                (center.x + radius * Expr.cos(angle)),
                (center.y + radius * Expr.sin(angle))
            )
        }

        fun ifExpr(condition: Expr, left: Point2, right: Point2) : Point2 {
            return Point2(
                Expr.ifExpr(condition, left.x, right.x),
                Expr.ifExpr(condition, left.y, right.y)
            )
        }
    }
}

class Point3(val x: Expr, val y: Expr, val z: Expr) : Figure() {

    fun normalized() : Point3 {
        return this / length()
    }

    fun copy(): Point3 {
        return Point3(x.evalLiteral(), y.evalLiteral(), z.evalLiteral())
    }

    fun dot(other: Point3) : Expr {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: Point3): Point3 {
        val x = y * other.z - z * other.y
        val y = z * other.x - this.x * other.z
        return Point3(this.x * other.y - this.y * other.x, x, y)
    }

    operator fun times(other: Expr) : Point3 {
        return Point3(x * other, y * other, z * other)
    }

    operator fun times(other: Double) : Point3 {
        val value = x.world.literal(other)
        return Point3(x * value, y * value, z * value)
    }

    operator fun div(other: Expr) : Point3 {
        return Point3(x / other, y / other, z / other)
    }

    operator fun div(other: Double) : Point3 {
        val value = x.world.literal(other)
        return Point3(x / value, y / value, z / value)
    }

    operator fun plus(right: Point3) : Point3 {
        return Point3(x + right.x, y + right.y, z + right.z)
    }

    operator fun minus(right: Point3) : Point3 {
        return Point3(x - right.x, y - right.y, z - right.z)
    }

    operator fun minus(right: Expr) : Point3 {
        return Point3(x - right, y - right, z - right)
    }

    fun squaredLength(): Expr {
        return x.pow(2) + y.pow(2) + z.pow(2)
    }

    fun length(): Expr {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: Point2): Expr {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun length(other: Point2): Expr {
        return squaredLength(other).sqrt()
    }

    override fun toString(): String {
        return "com.codecad.core.Point(x=$x, y=$y)"
    }

    companion object{
        operator fun Double.times(point: Point3): Point3 {
            return Point3(point.x * this, point.y * this, point.z * this)
        }
        operator fun Expr.times(point: Point3): Point3 {
            return Point3(point.x * this, point.y * this, point.z * this)
        }
    }
}
