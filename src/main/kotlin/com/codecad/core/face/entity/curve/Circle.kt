package com.codecad.core.face.entity.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.face.entity.Workplane

open class Circle(workplane: Workplane, val radius: Expr) : Conic(workplane) {

}