package com.codecad.core.mesh

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.project2d
import kotlin.math.atan2

interface Projector {
    fun project(point : Vec3) : Vec2
}

class PlaneProjector(val workplane: Workplane<Vec3>) : Projector{
    override fun project(point : Vec3) : Vec2{
        return workplane.project2d(point)
    }
}

class CylinderProjector(val workplane: Workplane<Vec3>, val radius: Double) : Projector{
    override fun project(point : Vec3) : Vec2 {
        val offset = workplane.project2d(point)
        val origin = workplane.project2d(workplane.origin)
        val diff = offset - origin

        val theta = atan2(diff.y, diff.x)

        val test = point - workplane.origin
        val axisUp = workplane.normal

        val axisOffset = test.dot(axisUp) // vector projection optimized
        return Vec2(theta, axisOffset)
    }
}