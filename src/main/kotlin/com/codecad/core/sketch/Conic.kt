package com.codecad.core.sketch

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr

abstract class Conic  {
    abstract val position: Vec2Expr
    abstract val radius: Expr
}