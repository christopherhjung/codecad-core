package com.codecad.core.face.entity.surface

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.face.entity.Workplane

class PlaneSurface(val workplane: Workplane) : Surface
class CylindricalSurface(val workplane: Workplane, val radius: Expr) : Surface