package com.codecad.core.mesh

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.face.entity.Workplane
import com.codecad.core.face.entity.WorkplaneExpr
import kotlin.math.atan2

interface Projector {
    fun project(point : Vec3) : Vec2
}

class PlaneProjector(val workplane: Workplane) : Projector{
    override fun project(point : Vec3) : Vec2{
        return workplane.project(point)
    }
}

class CylinderProjector(val workplane: Workplane, val radius: Expr) : Projector{
    override fun project(point : Vec3) : Vec2 {
        val offset = workplane.project(point)
        val origin = workplane.project(workplane.origin)
        val diff = offset - origin

        val theta = atan2(diff.y, diff.x)

        val test = point - workplane.origin
        val axisUp = workplane.axisUp

        val axisOffset = test.dot(axisUp) // vector projection optimized
        return Vec2(theta, axisOffset)
    }
}