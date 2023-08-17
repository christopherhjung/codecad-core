package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope
import kotlin.math.pow
import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double)
{
    operator fun plus(rhs : Vec3) : Vec3{
        return Vec3(x + rhs.x, y + rhs.y, z + rhs.z )
    }

    operator fun minus(rhs : Vec3) : Vec3{
        return Vec3(x - rhs.x, y - rhs.y, z - rhs.z )
    }

    fun dot(other: Vec3) : Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: Vec3): Vec3 {
        val x = y * other.z - z * other.y
        val y = z * other.x - this.x * other.z
        return Vec3(this.x * other.y - this.y * other.x, x, y)
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

    fun distance(other: Vec3): Double {
        return sqrt(squaredDistance(other))
    }

    companion object{
        val ZERO = Vec3(0.0,0.0,0.0)
    }
}

operator fun Double.times(rhs : Vec3) : Vec3{
    return Vec3(this * rhs.x, this * rhs.y, this * rhs.z )
}

class Vec3Expr(world: World, val x: Expr, val y: Expr, val z: Expr) : Expr(world){

    override fun eval(scope: Scope): Vec3 {
        return Vec3(x.evalDouble(scope), y.evalDouble(scope), z.evalDouble(scope))
    }

    fun normalized() : Vec3Expr {
        return this / length()
    }

    fun copy(): Vec3Expr {
        return world.vec3(x.evalLiteral(), y.evalLiteral(), z.evalLiteral())
    }

    fun dot(other: Vec3Expr) : Expr {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: Vec3Expr): Vec3Expr {
        val x = y * other.z - z * other.y
        val y = z * other.x - this.x * other.z
        return world.vec3(this.x * other.y - this.y * other.x, x, y)
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

    fun squaredLength(): Expr {
        return x.pow(2) + y.pow(2) + z.pow(2)
    }

    fun length(): Expr {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: Vec2Expr): Expr {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    fun length(other: Vec2Expr): Expr {
        return squaredLength(other).sqrt()
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
        return "Vec3(x=$x, y=$y)"
    }

    companion object{
        operator fun Double.times(point: Vec3Expr): Vec3Expr {
            val world = point.world
            return world.vec3(point.x * this, point.y * this, point.z * this)
        }
        operator fun Expr.times(point: Vec3Expr): Vec3Expr {
            return world.vec3(point.x * this, point.y * this, point.z * this)
        }
    }
}
