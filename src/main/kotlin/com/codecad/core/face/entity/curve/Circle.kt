package com.codecad.core.face.entity.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.face.entity.WorkplaneExpr

open class Circle(workplane: WorkplaneExpr, val radius: Expr) : Conic(workplane) {

}