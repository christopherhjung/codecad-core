package com.codecad.core.ast.vec

import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.div
import com.codecad.core.ast.primitive.times
import com.codecad.core.scope.Scope

data class Quaternion(val w: Double = 0.0, val vx:Double = 0.0, val vy:Double = 0.0, val vz:Double = 0.0)


class QuaternionExpr(world: World, val w: Expr, val vx:Expr, val vy:Expr, val vz:Expr) : Expr(world){

    override fun eval(scope: Scope): Quaternion {
        return Quaternion(
            w.evalDouble(scope),
            vx.evalDouble(scope),
            vy.evalDouble(scope),
            vz.evalDouble(scope)
        )
    }

    fun rotate(center: Vec3Expr, point : Vec3Expr) : Vec3Expr{
        val w2 = w.pow(2)
        val vx2 = vx.pow(2)
        val vy2 = vy.pow(2)
        val vz2 = vz.pow(2)

        val sqrSum = w2 + vx2 + vy2 + vz2
        val invSqrSum = 1.0 / sqrSum
        val offset = point - center

        val m = offset.x
        val n = offset.y
        val o = offset.z

        val x = center.x + invSqrSum * ( (w2+vx2-vy2-vz2)*m + 2.0*(w*vz+vx*vy)*n + 2.0*(vx*vz-w*vy)*o )
        val y = center.y + invSqrSum * ( 2.0*(vx*vy-w*vz)*m + (w2-vx2+vy2-vz2)*n + 2.0*(w*vx+vy*vz)*o )
        val z = center.z + invSqrSum * ( 2.0*(w*vy+vx*vz)*m + 2.0*(vy*vz-w*vx)*n + (w2-vx2-vy2+vz2)*o )

        return world.vec3(x,y,z)
    }

    fun inverse() : QuaternionExpr{
        val sqrSum = w.pow(2) + vx.pow(2) + vy.pow(2) + vz.pow(2)
        val minSqrSum = - sqrSum
        return QuaternionExpr(world, w / sqrSum, vx / minSqrSum, vy / minSqrSum, vz / minSqrSum )
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is QuaternionExpr &&
                w == other.w &&
                vx == other.vx &&
                vy == other.vy &&
                vz == other.vz
    }

    override fun hashCode(): Int {
        var hash = 31 * w.hashCode()
        hash = 31 * vx.hashCode() + 11 * hash
        hash = 31 * vy.hashCode() + 11 * hash
        hash = 31 * vz.hashCode() + 11 * hash
        return hash
    }

    override fun toString(): String {
        return "Quaternion(w=$w, vx=$vx, vy=$vy, vz=$vz)"
    }

    companion object{
        fun fromNAxis(axis : Vec3Expr, theta: Expr ) : QuaternionExpr{
            val thetaHalf = theta / 2
            val scaledAxis = axis * sin(thetaHalf)
            return QuaternionExpr(axis.world, cos(thetaHalf), scaledAxis.x, scaledAxis.y, scaledAxis.z)
        }
    }
}
