package com.codecad.core.brep.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.brep.WorkplaneExpr

class PlaneSurface(workplane: WorkplaneExpr) : Surface(workplane)
class CylindricalSurface(workplane: WorkplaneExpr, val radius: Expr) : Surface(workplane)