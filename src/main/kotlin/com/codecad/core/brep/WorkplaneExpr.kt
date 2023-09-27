package com.codecad.core.brep

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.rollover
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

    fun intersect(rhs : Line<Vec3>) : Vec3? {
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

        fun intersect(lhs : Plane, rhs : Plane) : Line<Vec3> {
            val direction = lhs.normal.cross(rhs.normal).normalized()
            val origin = projectPlane(lhs, rhs) + projectPlane(rhs, lhs)
            return Line(origin, direction)
        }

        fun from(origin: Vec3, normal: Vec3) : Plane{
            val distance = Vec3.project(origin, normal).length()
            return Plane(normal, distance)
        }
    }
}


class WorkplaneExpr(val origin: Vec3Expr, val normal: Vec3Expr, val x: Vec3Expr) : Expr(origin.world){
    val y get() = normal.cross(x).normalized()

    init {
        if(kotlin.math.abs(normal.length().evalDouble() - 1.0) > 1e-5){
            throw RuntimeException("Check")
        }
        if(kotlin.math.abs(x.length().evalDouble() - 1.0) > 1e-5){
            throw RuntimeException("Check")
        }
    }

    override fun eval(scope: Scope): Workplane<Vec3> {
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



fun Workplane<Vec3>.withOrigin(origin: Vec3) : Workplane<Vec3> {
    return Workplane(origin, normal, x)
}

fun Workplane<Vec3>.withNormal(normal: Vec3) : Workplane<Vec3> {
    return Workplane(origin, normal, x)
}



fun Workplane<Vec3>.unproject(point: Vec2) : Vec3 {
    return unproject(point.x, point.y)
}

fun Workplane<Vec3>.unprojectDir(point: Vec2) : Vec3 {
    return unprojectDir(point.x, point.y)
}

fun Workplane<Vec3>.unproject(x: Double, y: Double) : Vec3 {
    return unprojectDir(x, y) + origin
}

fun Workplane<Vec3>.unprojectDir(x: Double, y: Double) : Vec3 {
    val yAxis = normal.cross(this.x)
    return this.x * x + yAxis * y
}

fun Workplane<Vec3>.project2d(point: Vec3) : Vec2 {
    val fromOrigin = point - origin
    return projectDir2d(fromOrigin)
}

fun Workplane<Vec3>.projectDir2d(dir: Vec3) : Vec2 {
    val x = x.dot(dir) / x.squaredLength()
    val yAxis = normal.cross(this.x)
    val y = yAxis.dot(dir) / yAxis.squaredLength()
    return Vec2(x,y)
}

fun Workplane<Vec3>.project3d(point: Vec3) : Vec3 {
    return toPlane().project(point)
}

fun Workplane<Vec3>.toPlane() : Plane{
    return Plane.from(origin, normal)
}

fun Workplane<Vec3>.distanceTo(p: Vec3) : Double{
    return toPlane().distanceTo(p)
}

class Workplane<T : Vec<T>>(val origin: T, val normal: T, val x: T){

    init {
        assert(abs(normal.squaredLength() - 1.0) < 1e-5)
        assert(abs(x.squaredLength() - 1.0) < 1e-5)
    }

    fun move(offset: T) : Workplane<T>{
        return Workplane(origin + offset, normal, x)
    }

    fun invert() : Workplane<T>{
        return Workplane(origin, normal.negate(), x)
    }

    companion object{
        val XY = Workplane(Vec3.Zero, Vec3.DirectionZ, Vec3.DirectionX)
        val YZ = Workplane(Vec3.Zero, Vec3.DirectionX, Vec3.DirectionY)
        val ZX = Workplane(Vec3.Zero, Vec3.DirectionY, Vec3.DirectionZ)

        fun intersect(lhs : Workplane<Vec3>, rhs : Workplane<Vec3>) : Line<Vec3> {
            return Plane.intersect(lhs.toPlane(), rhs.toPlane())
        }

        fun normal(vararg points : Vec3) : Vec3{
            return normal(points.toList())
        }

        fun normal(points : Iterable<Vec3>) : Vec3{
            return points.rollover()
                .map { it.second - it.first }
                .rollover()
                .map { it.first.cross(it.second) }
                .maxBy { it.length() }
                .normalized()
        }
    }
}