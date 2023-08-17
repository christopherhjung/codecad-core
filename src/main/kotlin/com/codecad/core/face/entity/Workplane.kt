package com.codecad.core.face.entity

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3Expr

class Workplane(val origin : Vec3Expr, val axisA: Vec3Expr, val axisB : Vec3Expr){
    fun projectTo(point: Vec2Expr) : Vec3Expr {
        return projectTo(point.x, point.y)
    }

    fun projectTo(x: Expr, y: Expr) : Vec3Expr {
        return axisA * x + axisB * y + origin
    }

    fun unproject(point: Vec3Expr) : Vec2Expr {
        val world = origin.world
        val fromOrigin = point - origin
        val x = axisA.dot(fromOrigin) / axisA.squaredLength()
        val y = axisB.dot(fromOrigin) / axisB.squaredLength()
        return world.vec2(x,y)
    }
}