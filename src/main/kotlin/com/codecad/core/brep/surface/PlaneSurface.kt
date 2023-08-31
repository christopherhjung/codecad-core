package com.codecad.core.brep.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.curve.BSplineControlPoint

abstract class ElementarySurface(val workplane: WorkplaneExpr) : Surface
class PlaneSurface(workplane: WorkplaneExpr) : ElementarySurface(workplane)
class CylindricalSurface(workplane: WorkplaneExpr, val radius: Expr) : ElementarySurface(workplane)
class ConicalSurface(workplane: WorkplaneExpr, val radius: Expr, val angle: Expr) : ElementarySurface(workplane)
class ToroidalSurface(workplane: WorkplaneExpr, val major: Expr, val minor: Expr) : ElementarySurface(workplane)
class SphericalSurface(workplane: WorkplaneExpr, val radius: Expr) : ElementarySurface(workplane)


class BSplineSurface(val uDegree: Int,
                     val vDegree: Int,
                     val controlPoints : Array<Array<BSplineControlPoint>>
                     ) : Surface