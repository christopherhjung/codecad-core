package com.codecad.core.brep.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.curve.BSplineControlPoint

abstract class ElementarySurface(val workplane: Workplane<Vec3>) : Surface
class PlaneSurface(workplane: Workplane<Vec3>) : ElementarySurface(workplane)
class CylindricalSurface(workplane: Workplane<Vec3>, val radius: Double) : ElementarySurface(workplane)
class ConicalSurface(workplane: Workplane<Vec3>, val radius: Double, val angle: Double) : ElementarySurface(workplane)
class ToroidalSurface(workplane: Workplane<Vec3>, val major: Double, val minor: Double) : ElementarySurface(workplane)
class DegenerateToroidalSurface(workplane: Workplane<Vec3>, val major: Double, val minor: Double, val outer: Boolean) : ElementarySurface(workplane)
class SphericalSurface(workplane: Workplane<Vec3>, val radius: Double) : ElementarySurface(workplane)


class BSplineSurface(val uDegree: Int,
                     val vDegree: Int,
                     val controlPoints : Array<Array<BSplineControlPoint<Vec3>>>
                     ) : Surface