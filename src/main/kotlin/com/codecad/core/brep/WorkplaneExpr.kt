package com.codecad.core.brep

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.curve.Line
import com.codecad.core.scope.Scope
import kotlin.math.abs
import kotlin.math.pow

class Plane(val normal: Vec3, val distance: Double){
    fun distanceTo(point: Vec3) : Double{
        return normal.dot(point) - distance
    }

    fun project(point: Vec3) : Vec3 {
        return point - normal * distanceTo(point)
    }

    fun intersect(rhs : Line) : Vec3? {
        val p0 = rhs.origin
        val dir = rhs.origin

        val denominator = normal.dot(dir)

        if(denominator == 0.0){
            return null
        }

        val t = (distance - normal.dot(p0)) / denominator
        return p0 + dir * t
    }

    companion object{
        private fun projectPlane(a: Plane, b: Plane) : Vec3 {
            val factor = (a.distance - b.distance * ( a.normal.dot(b.normal) ))/
                    (1.0 - a.normal.dot(b.normal).pow(2.0))
            return a.normal * factor
        }

        fun intersect(lhs : Plane, rhs : Plane) : Line {
            val direction = lhs.normal.cross(rhs.normal)
            val origin = projectPlane(lhs, rhs) + projectPlane(rhs, lhs)
            return Line(origin, direction)
        }
    }
}


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

    fun project2d(point: Vec3Expr) : Vec2Expr {
        val fromOrigin = point - origin
        val x = x.dot(fromOrigin) / x.squaredLength()
        val y = y.dot(fromOrigin) / y.squaredLength()
        return origin.world.vec2(x,y)
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


    init {
        if(abs(normal.length() - 1.0) >= 1e-5){
            println("sss")
        }
        assert(abs(normal.length() - 1.0) < 1e-5)
        assert(abs(x.length() - 1.0) < 1e-5)
    }

    fun withOrigin(origin: Vec3) : Workplane {
        return Workplane(origin, normal, x)
    }

    fun withNormal(normal: Vec3) : Workplane {
        return Workplane(origin, normal, x)
    }

    fun unproject(point: Vec2) : Vec3 {
        return unproject(point.x, point.y)
    }

    fun unproject(x: Double, y: Double) : Vec3 {
        return this.x * x + this.y * y + origin
    }

    fun project2d(point: Vec3) : Vec2 {
        val fromOrigin = point - origin
        val x = x.dot(fromOrigin) / x.squaredLength()
        val y = y.dot(fromOrigin) / y.squaredLength()
        return Vec2(x,y)
    }

    fun project3d(point: Vec3) : Vec3 {
        return toPlane().project(point)
    }

    fun toPlane() : Plane{
        val distance = Vec3.project(origin, normal).length()
        return Plane(normal, distance)
    }

    fun distanceTo(p: Vec3) : Double{
        return toPlane().distanceTo(p)
    }

    fun move(offset: Vec3) : Workplane{
        return Workplane(origin + offset, normal, x)
    }

    fun invert() : Workplane{
        return Workplane(origin, normal.negate(), x)
    }

    companion object{
        val XY = Workplane(Vec3.ZERO, Vec3.DirectionZ, Vec3.DirectionX)
        val YZ = Workplane(Vec3.ZERO, Vec3.DirectionX, Vec3.DirectionY)
        val ZX = Workplane(Vec3.ZERO, Vec3.DirectionY, Vec3.DirectionX)

        fun intersect(lhs : Workplane, rhs : Workplane) : Line {
            return Plane.intersect(lhs.toPlane(), rhs.toPlane())
        }
    }
}