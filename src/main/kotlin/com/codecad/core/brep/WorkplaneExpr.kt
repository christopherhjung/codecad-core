package com.codecad.core.brep

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.scope.Scope

class WorkplaneExpr(val origin: Vec3Expr, val normal: Vec3Expr, val x: Vec3Expr) : Expr(origin.world){
    val y get() = normal.cross(x)

    init {
        if(kotlin.math.abs(normal.length().evalDouble() - 1.0) > 1e-5){
            throw RuntimeException("Check")
        }
        if(kotlin.math.abs(x.length().evalDouble() - 1.0) > 1e-5){
            throw RuntimeException("Check")
        }
    }

    override fun eval(scope: Scope): Workplane {
        return Workplane(origin.eval(), normal.eval(), x.eval())
    }

    fun withOrigin(origin: Vec3Expr) : WorkplaneExpr {
        return WorkplaneExpr(origin, normal, x)
    }

    fun withNormal(normal: Vec3Expr) : WorkplaneExpr {
        return WorkplaneExpr(origin, normal, x)
    }

    fun unproject(point: Vec2Expr) : Vec3Expr {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Expr, y: Expr) : Vec3Expr {
        return this.x * x + this.y * y + origin
    }

    fun project(point: Vec3Expr) : Vec2Expr {
        val fromOrigin = point - origin
        val x = x.dot(fromOrigin) / x.squaredLength()
        val y = y.dot(fromOrigin) / y.squaredLength()
        return origin.world.vec2(x,y)
    }

    fun distanceTo(point: Vec3Expr) : Expr{
        return origin.distanceTo(point)
    }

    fun move(offset: Vec3Expr) : WorkplaneExpr{
        return WorkplaneExpr(origin + offset, normal, x)
    }

    fun invert() : WorkplaneExpr{
        return WorkplaneExpr(origin, normal.negate(), x)
    }
}

class Workplane(val origin: Vec3, val normal: Vec3, val x: Vec3){
    val y get() = normal.cross(x)

    fun unproject(point: Vec2) : Vec3 {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Double, y: Double) : Vec3 {
        return this.x * x + this.y * y + origin
    }

    fun project(point: Vec3) : Vec2 {
        val fromOrigin = point - origin
        val x = x.dot(fromOrigin) / x.squaredLength()
        val y = y.dot(fromOrigin) / y.squaredLength()
        return Vec2(x,y)
    }
}