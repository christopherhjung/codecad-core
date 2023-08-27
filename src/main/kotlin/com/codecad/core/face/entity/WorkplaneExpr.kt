package com.codecad.core.face.entity

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.scope.Scope

class WorkplaneExpr(val origin : Vec3Expr, val axisA: Vec3Expr, val axisB : Vec3Expr) : Expr(origin.world){
    val axisUp get() = axisA.cross(axisB)

    override fun eval(scope: Scope): Workplane {
        return Workplane(origin.eval(), axisA.eval(), axisB.eval())
    }

    fun withOrigin(origin: Vec3Expr) : WorkplaneExpr{
        return WorkplaneExpr(origin, axisA, axisB)
    }

    fun unproject(point: Vec2Expr) : Vec3Expr {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Expr, y: Expr) : Vec3Expr {
        return axisA * x + axisB * y + origin
    }

    fun project(point: Vec3Expr) : Vec2Expr {
        val world = origin.world
        val fromOrigin = point - origin
        val x = axisA.dot(fromOrigin) / axisA.squaredLength()
        val y = axisB.dot(fromOrigin) / axisB.squaredLength()
        return world.vec2(x,y)
    }
}

class Workplane(val origin : Vec3, val axisA: Vec3, val axisB : Vec3){
    val axisUp get() = axisA.cross(axisB)

    fun unproject(point: Vec2) : Vec3 {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Double, y: Double) : Vec3 {
        return axisA * x + axisB * y + origin
    }

    fun project(point: Vec3) : Vec2 {
        val fromOrigin = point - origin
        val x = axisA.dot(fromOrigin) / axisA.squaredLength()
        val y = axisB.dot(fromOrigin) / axisB.squaredLength()
        return Vec2(x,y)
    }
}