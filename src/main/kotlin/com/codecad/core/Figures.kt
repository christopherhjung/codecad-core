package com.codecad.core

import com.codecad.common.PointD
import com.codecad.core.parser.ast.primitive.Expr
import com.codecad.core.scope.EmptyScope
import com.codecad.core.scope.Scope

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}

abstract class Figure()

class Segment2(val p0: Vec2, val p1: Vec2) : Figure(){
    val squaredLength : Expr
        get() = ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))

    val length : Expr
        get() = squaredLength.sqrt()

    val midPoint : Vec2
        get() = (p0 + p1) / 2.0

    val difference : Vec2
        get() = p1 - p0

    val direction : Vec2
        get() = difference.normalized()
}

interface CircleLike{
    val center : Vec2
    val radius : Expr
}

open class Circle(override val center: Vec2, override val radius: Expr) : Figure(), CircleLike

class Arc(val p0: Vec2, val p1: Vec2, val h: Expr) : Figure(), CircleLike{

    private val radiusSign: Expr = run{
        val s = (p1 - p0).length()
        (h.pow(2) * 4 + s.pow(2)) / (h * 8)
    }

    override val radius: Expr = run{
        Expr.abs(radiusSign)
    }

    override val center: Vec2 = run{
        val direction = p1 - p0
        val middle = (p0 + p1) * 0.5
        val normalizedDirection = direction.normalized()
        val positive = Vec2(normalizedDirection.y, -normalizedDirection.x)
        middle + positive * (h - radiusSign)
    }
}

class FunctionFigure( val function: (Expr) -> Vec2) : Figure(){

}

class Vec2(val x: Expr, val y: Expr) : Figure() {

    fun absoluteAngle(target: Vec2) : Double{
        val b = (target - this).eval()
        return kotlin.math.atan2(
            b.y.evalDouble(),
            b.x.evalDouble(),
        )
    }

    fun normalized() : Vec2 {
        return this / length()
    }

    fun rotate(center: Vec2, angle: Expr) : Vec2 {
        val a = Expr.sin(angle)
        val b = Expr.cos(angle)

        return Vec2(
            b * ( x - center.x ) - a * ( y - center.y ) + center.x,
            a * ( x - center.x ) + b * ( y - center.y ) + center.y
        )
    }

    fun eval(scope: Scope = EmptyScope): Vec2 {
        return Vec2(x.evalLiteral(scope), y.evalLiteral(scope))
    }

    fun fixed() : PointD {
        return PointD(x.evalDouble(), y.evalDouble())
    }

    fun scalar(other: Vec2) : Expr {
        return x * other.x + y * other.y
    }

    fun cross(other: Vec2) : Expr {
        return x * other.y - y * other.x
    }

    operator fun times(other: Vec2) : Expr {
        return x * other.y - y * other.x
    }

    operator fun times(other: Expr) : Vec2 {
        return Vec2(x * other, y * other)
    }

    operator fun times(other: Double) : Vec2 {
        val value = x.world.literal(other)
        return Vec2(x * value, y * value)
    }

    operator fun div(other: Expr) : Vec2 {
        return Vec2(x / other, y / other)
    }

    operator fun div(other: Double) : Vec2 {
        val value = x.world.literal(other)
        return Vec2(x / value, y / value)
    }

    operator fun plus(right: Vec2) : Vec2 {
        return Vec2(x + right.x, y + right.y)
    }

    operator fun minus(right: Vec2) : Vec2 {
        return Vec2(x - right.x, y - right.y)
    }

    operator fun minus(right: Expr) : Vec2 {
        return Vec2(x - right, y - right)
    }

    fun squaredLength(): Expr {
        return x.pow(2) + y.pow(2)
    }

    fun length(): Expr {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: Vec2): Expr {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun length(other: Vec2): Expr {
        return squaredLength(other).sqrt()
    }

    override fun toString(): String {
        return "com.codecad.core.Point(x=$x, y=$y)"
    }

    companion object{
        fun onCircle(center: Vec2, radius: Expr, angle: Expr) : Vec2 {
            return Vec2(
                (center.x + radius * Expr.cos(angle)),
                (center.y + radius * Expr.sin(angle))
            )
        }

        fun ifExpr(condition: Expr, left: Vec2, right: Vec2) : Vec2 {
            return Vec2(
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
        return Point3(x.evalLiteral(EmptyScope), y.evalLiteral(EmptyScope), z.evalLiteral(EmptyScope))
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

    fun squaredLength(other: Vec2): Expr {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun length(other: Vec2): Expr {
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
