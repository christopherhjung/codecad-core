package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope
import kotlin.math.atan2

class Vec2(val x: Double, val y: Double)

class Vec2Expr(world : World, val x: Expr, val y: Expr) : Expr(world) {

    fun absoluteAngle(target: Vec2Expr) : Double{
        val b = (target - this).eval()
        return atan2(
            b.y,
            b.x,
        )
    }

    fun normalized() : Vec2Expr {
        return this / length()
    }

    fun rotate(center: Vec2Expr, angle: Expr) : Vec2Expr {
        val a = sin(angle)
        val b = cos(angle)

        return world.vec2(
            b * (x - center.x) - a * (y - center.y) + center.x,
            a * (x - center.x) + b * (y - center.y) + center.y
        )
    }

    override fun eval(scope: Scope): Vec2 {
        return Vec2(x.evalDouble(scope), y.evalDouble(scope))
    }

    fun scalar(right: Vec2Expr) : Expr {
        return x * right.x + y * right.y
    }

    fun cross(right: Vec2Expr) : Expr {
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
        return x.pow(2) + y.pow(2)
    }

    fun length(): Expr {
        return squaredLength().sqrt()
    }

    fun squaredDistance(right: Vec2Expr): Expr {
        return ( x - right.x ).pow(2) + ( y - right.y ).pow(2)
    }

    fun distance(right: Vec2Expr): Expr {
        return squaredDistance(right).sqrt()
    }

    override fun equals(right: Any?): Boolean {
        return this === right ||
                right is Vec3Expr &&
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