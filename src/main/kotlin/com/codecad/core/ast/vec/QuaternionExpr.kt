package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.div
import com.codecad.core.ast.primitive.minus
import com.codecad.core.ast.primitive.times
import com.codecad.core.scope.Scope
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class Quaternion(val w: Double = 0.0, val x:Double = 0.0, val y:Double = 0.0, val z:Double = 0.0){

    fun rotate(center: Vec3, point : Vec3) : Vec3{
        return rotate(point - center) + center
    }

    fun rotate(point : Vec3) : Vec3{
        val w2 = w.pow(2)
        val vx2 = x.pow(2)
        val vy2 = y.pow(2)
        val vz2 = z.pow(2)

        val invSqrSum = 1.0 / (w2 + vx2 + vy2 + vz2)
        val m = point.x
        val n = point.y
        val o = point.z

        val tx = ( (w2+vx2-vy2-vz2)*m + 2.0*(w*z + x*y)*n  + 2.0*(x*z-w*y)*o )
        val ty = ( 2.0*(x*y - w*z)*m  + (w2-vx2+vy2-vz2)*n + 2.0*(w*x+y*z)*o )
        val tz = ( 2.0*(w*y + x*z)*m  + 2.0*(y*z - w*x)*n  + (w2-vx2-vy2+vz2)*o )

        return Vec3(tx,ty,tz) * invSqrSum
    }

    fun dotProduct(other: Quaternion): Double {
        return w * other.w + x * other.x + y * other.y + z * other.z
    }

    fun inverse() : Quaternion{
        val sqrSum = w.pow(2) + x.pow(2) + y.pow(2) + z.pow(2)
        val minSqrSum = - sqrSum
        return Quaternion(w / sqrSum, x / minSqrSum, y / minSqrSum, z / minSqrSum )
    }

    fun negate(): Quaternion {
        return Quaternion(-w, -x, -y, -z)
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is Quaternion &&
                w == other.w &&
                x == other.x &&
                y == other.y &&
                z == other.z
    }

    override fun hashCode(): Int {
        var hash = 31 * w.hashCode()
        hash = 31 * x.hashCode() + 11 * hash
        hash = 31 * y.hashCode() + 11 * hash
        hash = 31 * z.hashCode() + 11 * hash
        return hash
    }

    override fun toString(): String {
        return "Quaternion(w=$w, x=$x, y=$y, z=$z)"
    }

    companion object{
        fun fromAxis(axis : Vec3, theta: Double ) : Quaternion{
            val thetaHalf = theta * 0.5
            val scaledAxis = axis.scaleTo(sin(thetaHalf))
            return Quaternion(cos(thetaHalf), scaledAxis.x, scaledAxis.y, scaledAxis.z)
        }

        fun slerp(q1: Quaternion, q2: Quaternion, t: Double): Quaternion {
            val dot = q1.dotProduct(q2)
            val theta = acos(dot)
            val sinTheta = sin(theta)

            val weight1 = sin((1.0 - t) * theta) / sinTheta
            val weight2 = sin(t * theta) / sinTheta

            return Quaternion(
                w = weight1 * q1.w + weight2 * q2.w,
                x = weight1 * q1.x + weight2 * q2.x,
                y = weight1 * q1.y + weight2 * q2.y,
                z = weight1 * q1.z + weight2 * q2.z
            )
        }
    }
}


class QuaternionExpr(world: World, val w: Expr, val x:Expr, val y:Expr, val z:Expr) : Expr(world){

    override fun eval(scope: Scope): Quaternion {
        return Quaternion(
            w.evalDouble(scope),
            x.evalDouble(scope),
            y.evalDouble(scope),
            z.evalDouble(scope)
        )
    }

    fun rotate(center: Vec3Expr, point : Vec3Expr) : Vec3Expr{
        return rotate(point - center) + center
    }

    fun rotate(point : Vec3Expr) : Vec3Expr{
        val w2 = w.pow(2)
        val vx2 = x.pow(2)
        val vy2 = y.pow(2)
        val vz2 = z.pow(2)

        val invSqrSum = 1.0 / (w2 + vx2 + vy2 + vz2)
        val m = point.x
        val n = point.y
        val o = point.z

        val tx = ( (w2+vx2-vy2-vz2)*m + 2.0*(w*z + x*y)*n  + 2.0*(x*z-w*y)*o )
        val ty = ( 2.0*(x*y - w*z)*m  + (w2-vx2+vy2-vz2)*n + 2.0*(w*x+y*z)*o )
        val tz = ( 2.0*(w*y + x*z)*m  + 2.0*(y*z - w*x)*n  + (w2-vx2-vy2+vz2)*o )

        return world.vec3(tx,ty,tz) * invSqrSum
    }

    fun dotProduct(other: QuaternionExpr): Expr {
        return w * other.w + x * other.x + y * other.y + z * other.z
    }

    fun inverse() : QuaternionExpr{
        val sqrSum = w.pow(2) + x.pow(2) + y.pow(2) + z.pow(2)
        val minSqrSum = - sqrSum
        return QuaternionExpr(world, w / sqrSum, x / minSqrSum, y / minSqrSum, z / minSqrSum )
    }

    fun negate(): QuaternionExpr {
        return QuaternionExpr(world, -w, -x, -y, -z)
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
            other is QuaternionExpr &&
            w == other.w &&
            x == other.x &&
            y == other.y &&
            z == other.z
    }

    override fun hashCode(): Int {
        var hash = 31 * w.hashCode()
        hash = 31 * x.hashCode() + 11 * hash
        hash = 31 * y.hashCode() + 11 * hash
        hash = 31 * z.hashCode() + 11 * hash
        return hash
    }

    override fun toString(): String {
        return "Quaternion(w=$w, x=$x, y=$y, z=$z)"
    }

    companion object{
        fun fromAxis(axis : Vec3Expr, theta: Expr ) : QuaternionExpr{
            val thetaHalf = theta * 0.5
            val scaledAxis = axis.scaleTo(sin(thetaHalf))
            return QuaternionExpr(axis.world, cos(thetaHalf), scaledAxis.x, scaledAxis.y, scaledAxis.z)
        }

        fun slerp(q1: QuaternionExpr, q2: QuaternionExpr, t: Expr): QuaternionExpr {
            val dot = q1.dotProduct(q2)
            val theta = acos(dot)
            val sinTheta = sin(theta)

            val weight1 = sin((1.0 - t) * theta) / sinTheta
            val weight2 = sin(t * theta) / sinTheta

            return QuaternionExpr(
                q1.world,
                w = weight1 * q1.w + weight2 * q2.w,
                x = weight1 * q1.x + weight2 * q2.x,
                y = weight1 * q1.y + weight2 * q2.y,
                z = weight1 * q1.z + weight2 * q2.z
            )
        }
    }
}
