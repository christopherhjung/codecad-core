package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.EmptyScope
import com.codecad.core.scope.Scope

class Vec3(val x: Double, val y: Double, val z: Double)

class Vec3Expr(world: World, val x: Expr, val y: Expr, val z: Expr) : Expr(world){

    override fun eval(scope: Scope): Vec3 {
        return Vec3(x.evalDouble(scope), y.evalDouble(scope), z.evalDouble(scope))
    }

    fun normalized() : Vec3Expr {
        return this / length()
    }

    fun copy(): Vec3Expr {
        return world.vec3(x.evalLiteral(EmptyScope), y.evalLiteral(EmptyScope), z.evalLiteral(EmptyScope))
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
        val value = x.world.literal(right)
        return world.vec3(x * value, y * value, z * value)
    }

    override operator fun div(right: Expr) : Vec3Expr {
        return world.vec3(x / right, y / right, z / right)
    }

    override operator fun div(right: Double) : Vec3Expr {
        val value = x.world.literal(right)
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
