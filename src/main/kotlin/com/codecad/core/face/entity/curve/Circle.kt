package com.codecad.core.face.entity.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.face.entity.AxisPlacement

open class Circle(axisPlacement: AxisPlacement, val radius: Expr) : Conic(axisPlacement) {

}