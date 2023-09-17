package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope
import java.util.*
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.pow
import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double)
{
    fun dot(other: Vec3) : Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: Vec3): Vec3 {
        val x = this.y * other.z - this.z * other.y
        val y = this.z * other.x - this.x * other.z
        val z = this.x * other.y - this.y * other.x
        return Vec3(x, y, z)
    }

    operator fun times(value: Double) : Vec3 {
        return Vec3(x * value, y * value, z * value)
    }

    operator fun div(right: Double) : Vec3 {
        return Vec3(x / right, y / right, z / right)
    }

    operator fun plus(right: Vec3) : Vec3 {
        return Vec3(x + right.x, y + right.y, z + right.z)
    }

    operator fun minus(right: Vec3) : Vec3 {
        return Vec3(x - right.x, y - right.y, z - right.z)
    }

    operator fun minus(right: Double) : Vec3 {
        return Vec3(x - right, y - right, z - right)
    }

    fun squaredLength(): Double {
        return x.pow(2) + y.pow(2) + z.pow(2)
    }

    fun length(): Double {
        return sqrt(squaredLength())
    }

    fun squaredDistance(other: Vec3): Double {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun distanceTo(other: Vec3): Double {
        return sqrt(squaredDistance(other))
    }

    fun toExpr(world: World) : Vec3Expr{
        return world.vec3(world.literal(x), world.literal(y), world.literal(z))
    }

    fun negate() : Vec3{
        return Vec3(-x,-y,-z)
    }

    fun scaleTo(expr : Double) : Vec3{
        return normalized() * expr
    }

    fun normalized() : Vec3 {
        return this / length()
    }

    override fun toString(): String {
        return "Vec3(${"%.2f".format(Locale.US, x)}, ${"%.2f".format(Locale.US, y)}, ${"%.2f".format(Locale.US, z)})"
    }

    companion object{
        val ZERO = Vec3(0.0,0.0,0.0)
        val DirectionX = Vec3(1.0, 0.0, 0.0)
        val DirectionY = Vec3(0.0, 1.0, 0.0)
        val DirectionZ = Vec3(0.0, 0.0, 1.0)

        fun angleWithDot(lhs: Vec3, rhs: Vec3) : Double{
            return acos(lhs.normalized().dot(rhs.normalized()))
        }

        fun angle(lhs: Vec3, rhs: Vec3) : Double{
            return asin(lhs.normalized().cross(rhs.normalized()).length())
        }

        fun midpoint(lhs: Vec3, rhs: Vec3) : Vec3{
            return (lhs + rhs) / 2.0
        }

        //projects lhs onto rhs
        fun project(lhs: Vec3, rhs: Vec3) : Vec3{
            return rhs * ( lhs.dot(rhs) / rhs.squaredLength() )
        }

        operator fun Double.times(point: Vec3): Vec3 {
            return Vec3(point.x * this, point.y * this, point.z * this)
        }
    }
}

operator fun Double.times(rhs : Vec3) : Vec3{
    return Vec3(this * rhs.x, this * rhs.y, this * rhs.z )
}

class Vec3Expr(world: World, val x: Expr, val y: Expr, val z: Expr) : Expr(world){

    override fun eval(scope: Scope): Vec3 {
        return Vec3(x.evalDouble(scope), y.evalDouble(scope), z.evalDouble(scope))
    }

    fun nan() : Boolean{
        return x == world.NaN || y == world.NaN || z == world.NaN
    }

    fun scaleTo(expr : Expr) : Vec3Expr{
        return this.normalized() * expr
    }

    fun normalized() : Vec3Expr {
        return when (this.opt) {
            NormalizedVec -> this
            else -> this / length()
        }
    }

    fun copy(): Vec3Expr {
        return world.vec3(x.evalLiteral(), y.evalLiteral(), z.evalLiteral())
    }

    fun dot(other: Vec3Expr) : Expr {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: Vec3Expr): Vec3Expr {
        val x = this.y * other.z - this.z * other.y
        val y = this.z * other.x - this.x * other.z
        val z = this.x * other.y - this.y * other.x
        val result = world.vec3(x, y, z)
        /*if(type == NormalizedVec && other.type == NormalizedVec){
            result.type = NormalizedVec
        }*/
        return result
    }

    override operator fun times(right: Expr) : Vec3Expr {
        return world.vec3(x * right, y * right, z * right)
    }

    override operator fun times(right: Double) : Vec3Expr {
        val value = world.literal(right)
        return world.vec3(x * value, y * value, z * value)
    }

    override operator fun div(right: Expr) : Vec3Expr {
        return world.vec3(x / right, y / right, z / right)
    }

    override operator fun div(right: Double) : Vec3Expr {
        val value = world.literal(right)
        return world.vec3(x / value, y / value, z / value)
    }

    operator fun plus(right: Vec3Expr) : Vec3Expr {
        return world.vec3(x + right.x, y + right.y, z + right.z)
    }

    operator fun minus(right: Vec3Expr) : Vec3Expr {
        return world.vec3(x - right.x, y - right.y, z - right.z)
    }

    override operator fun minus(right: Expr) : Vec3Expr {
        return world.vec3(x - right, y - right, z - right)
    }

    fun negate() : Vec3Expr{
        return world.vec3(-x,-y,-z)
    }

    fun squaredLength(): Expr {
        return when (opt) {
            NormalizedVec -> world.One
            else -> x.pow(2) + y.pow(2) + z.pow(2)
        }
    }

    fun length(): Expr {
        return when (opt) {
            NormalizedVec -> world.One
            else -> squaredLength().sqrt()
        }
    }

    fun squaredDistanceTo(other: Vec3Expr): Expr {
        return ( this - other ).squaredLength()
    }

    fun distanceTo(other: Vec3Expr): Expr {
        return squaredDistanceTo(other).sqrt()
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is Vec3Expr &&
                x == other.x &&
                y == other.y &&
                z == other.z
    }

    override fun hashCode(): Int {
        var hash = 31 * x.hashCode()
        hash = 31 * y.hashCode() + 11 * hash
        hash = 31 * z.hashCode() + 11 * hash
        return hash
    }

    override fun toString(): String {
        return "Vec3(x=$x, y=$y, z=$z)"
    }

    companion object{

        fun angleWithDot(lhs: Vec3Expr, rhs: Vec3Expr) : Expr{
            return acos(lhs.normalized().dot(rhs.normalized()))
        }

        fun angle(lhs: Vec3Expr, rhs: Vec3Expr) : Expr{
            return asin(lhs.normalized().cross(rhs.normalized()).length())
        }

        fun midpoint(lhs: Vec3Expr, rhs: Vec3Expr) : Vec3Expr{
            return (lhs + rhs) / 2.0
        }

        //projects lhs onto rhs
        fun project(lhs: Vec3Expr, rhs: Vec3Expr) : Vec3Expr{
            return rhs * ( lhs.dot(rhs) / rhs.squaredLength() )
        }

        operator fun Double.times(point: Vec3Expr): Vec3Expr {
            val world = point.world
            return world.vec3(point.x * this, point.y * this, point.z * this)
        }
    }
}
