package com.codecad.core.brep.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.curve.BSplineControlPoint

abstract class ElementarySurface(val workplane: Workplane) : Surface
class PlaneSurface(workplane: Workplane) : ElementarySurface(workplane)
class CylindricalSurface(workplane: Workplane, val radius: Double) : ElementarySurface(workplane)
class ConicalSurface(workplane: Workplane, val radius: Double, val angle: Double) : ElementarySurface(workplane)
class ToroidalSurface(workplane: Workplane, val major: Double, val minor: Double) : ElementarySurface(workplane)
class DegenerateToroidalSurface(workplane: Workplane, val major: Double, val minor: Double, val outer: Boolean) : ElementarySurface(workplane)
class SphericalSurface(workplane: Workplane, val radius: Double) : ElementarySurface(workplane)


class BSplineSurface(val uDegree: Int,
                     val vDegree: Int,
                     val controlPoints : Array<Array<BSplineControlPoint>>
                     ) : Surface