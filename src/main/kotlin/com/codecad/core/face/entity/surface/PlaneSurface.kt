package com.codecad.core.face.entity.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.face.entity.WorkplaneExpr

class PlaneSurface(val workplane: WorkplaneExpr) : Surface
class CylindricalSurface(val workplane: WorkplaneExpr, val radius: Expr) : Surface