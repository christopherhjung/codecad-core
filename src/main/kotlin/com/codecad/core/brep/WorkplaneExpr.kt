package com.codecad.core.brep

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.scope.Scope

class WorkplaneExpr(val origin: Vec3Expr, val normal: Vec3Expr, val xAxis: Vec3Expr) : Expr(origin.world){
    val yAxis get() = normal.cross(xAxis)

    override fun eval(scope: Scope): Workplane {
        return Workplane(origin.eval(), normal.eval(), xAxis.eval())
    }

    fun withOrigin(origin: Vec3Expr) : WorkplaneExpr {
        return WorkplaneExpr(origin, normal, xAxis)
    }

    fun unproject(point: Vec2Expr) : Vec3Expr {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Expr, y: Expr) : Vec3Expr {
        return xAxis * x + yAxis * y + origin
    }

    fun project(point: Vec3Expr) : Vec2Expr {
        val fromOrigin = point - origin
        val x = xAxis.dot(fromOrigin) / xAxis.squaredLength()
        val y = yAxis.dot(fromOrigin) / yAxis.squaredLength()
        return origin.world.vec2(x,y)
    }

    fun move(offset: Vec3Expr) : WorkplaneExpr{
        return WorkplaneExpr(origin + offset, normal, xAxis)
    }

    fun invert() : WorkplaneExpr{
        return WorkplaneExpr(origin, normal.negate(), xAxis)
    }
}

class Workplane(val origin: Vec3, val normal: Vec3, val axisX: Vec3){
    val axisY get() = normal.cross(axisX)

    fun unproject(point: Vec2) : Vec3 {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Double, y: Double) : Vec3 {
        return axisX * x + axisY * y + origin
    }

    fun project(point: Vec3) : Vec2 {
        val fromOrigin = point - origin
        val x = axisX.dot(fromOrigin) / axisX.squaredLength()
        val y = axisY.dot(fromOrigin) / axisY.squaredLength()
        return Vec2(x,y)
    }
}