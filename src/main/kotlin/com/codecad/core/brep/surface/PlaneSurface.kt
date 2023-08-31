package com.codecad.core.brep.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.curve.BSplineControlPoint

class PlaneSurface(workplane: WorkplaneExpr) : Surface(workplane)
class CylindricalSurface(workplane: WorkplaneExpr, val radius: Expr) : Surface(workplane)
class ToroidalSurface(workplane: WorkplaneExpr, val major: Expr, val minor: Expr) : Surface(workplane)

class BSplineSurface(workplane: WorkplaneExpr,
                     val uDegree: Int,
                     val vDegree: Int,
                     val controlPoints : Array<Array<BSplineControlPoint>>
                     ) : Surface(workplane)