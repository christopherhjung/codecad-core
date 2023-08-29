package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.brep.WorkplaneExpr

open class Circle(workplane: WorkplaneExpr, val radius: Expr) : Conic(workplane) {

}