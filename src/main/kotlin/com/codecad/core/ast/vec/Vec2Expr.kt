package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope
import kotlin.math.*

data class Vec2(val x: Double, val y: Double){
    companion object{
        val ZERO = Vec2(0.0, 0.0)
    }

    fun absoluteAngle() : Double{
        return atan2(y, x)
    }

    fun rotate(center: Vec2, angle: Double) : Vec2 {
        return rotate(this - center, angle) + center
    }

    fun rotate(angle: Double) : Vec2 {
        val s = sin(angle)
        val c = cos(angle)

        return Vec2(
            c * x - s * y,
            s * x + c * y
        )
    }

    fun dot(right: Vec2) : Double {
        return x * right.x + y * right.y
    }

    fun crossZ(right: Vec2) : Double {
        return x * right.y - y * right.x
    }

    operator fun times(value: Double) : Vec2 {
        return Vec2(x * value, y * value)
    }

    operator fun div(right: Double) : Vec2 {
        return Vec2(x / right, y / right)
    }

    operator fun plus(right: Vec2) : Vec2 {
        return Vec2(x + right.x, y + right.y)
    }

    operator fun minus(right: Vec2) : Vec2 {
        return Vec2(x - right.x, y - right.y)
    }

    operator fun minus(right: Double) : Vec2 {
        return Vec2(x - right, y - right)
    }

    fun squaredLength(): Double {
        return x.pow(2) + y.pow(2)
    }

    fun length(): Double {
        return sqrt(squaredLength())
    }

    fun squaredDistance(other: Vec2): Double {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun distance(other: Vec2): Double {
        return sqrt(squaredDistance(other))
    }

    fun normalized() : Vec2 {
        return this / length()
    }

    fun toExpr(world: World) : Vec2Expr{
        return world.vec2(world.literal(x), world.literal(y))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }


}

class Vec2Expr(world : World, val x: Expr, val y: Expr) : Expr(world) {

    fun absoluteAngle() : Expr{
        return atan2(y, x)
    }

    fun normalized() : Vec2Expr {
        return when (this.opt) {
            NormalizedVec -> this
            else -> {
                val result = this / length()
                result.opt = NormalizedVec
                result
            }
        }
    }

    fun rotate(center: Vec2Expr, angle: Expr) : Vec2Expr {
        return rotate(this - center, angle) + center
    }

    fun rotate(angle: Expr) : Vec2Expr {
        val s = sin(angle)
        val c = cos(angle)

        return world.vec2(
            c * x - s * y,
            s * x + c * y
        )
    }

    override fun eval(scope: Scope): Vec2 {
        return Vec2(x.evalDouble(scope), y.evalDouble(scope))
    }

    fun dot(right: Vec2Expr) : Expr {
        return x * right.x + y * right.y
    }

    fun crossZ(right: Vec2Expr) : Expr {
        return x * right.y - y * right.x
    }

    operator fun times(right: Vec2Expr) : Expr {
        return x * right.y - y * right.x
    }

    override operator fun times(right: Expr) : Vec2Expr {
        return world.vec2(x * right, y * right)
    }

    override operator fun times(right: Double) : Vec2Expr {
        val value = x.world.literal(right)
        return world.vec2(x * value, y * value)
    }

    override operator fun div(right: Expr) : Vec2Expr {
        return world.vec2(x / right, y / right)
    }

    override operator fun div(right: Double) : Vec2Expr {
        val value = x.world.literal(right)
        return world.vec2(x / value, y / value)
    }

    operator fun plus(right: Vec2Expr) : Vec2Expr {
        return world.vec2(x + right.x, y + right.y)
    }

    operator fun minus(right: Vec2Expr) : Vec2Expr {
        return world.vec2(x - right.x, y - right.y)
    }

    override operator fun minus(right: Expr) : Vec2Expr {
        return world.vec2(x - right, y - right)
    }

    fun squaredLength(): Expr {
        return when (opt) {
            NormalizedVec -> world.One
            else -> x.pow(2) + y.pow(2)
        }
    }

    fun length(): Expr {
        return when (opt) {
            NormalizedVec -> world.One
            else -> squaredLength().sqrt()
        }
    }

    fun squaredDistance(right: Vec2Expr): Expr {
        return ( x - right.x ).pow(2) + ( y - right.y ).pow(2)
    }

    fun distance(right: Vec2Expr): Expr {
        return squaredDistance(right).sqrt()
    }

    override fun equals(right: Any?): Boolean {
        return this === right ||
                right is Vec2Expr &&
                x == right.x &&
                y == right.y
    }

    override fun hashCode(): Int {
        var hash = 31 * x.hashCode()
        hash = 31 * y.hashCode() + 11 * hash
        return hash
    }

    override fun toString(): String {
        return "Vec2(x=$x, y=$y)"
    }
}