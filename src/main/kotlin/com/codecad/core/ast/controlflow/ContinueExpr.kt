package com.codecad.core.ast.controlflow

import com.codecad.core.World
import com.codecad.core.ast.controlflow.exception.ContinueException
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope

class ContinueExpr(world: World, private val label: String?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        throw ContinueException(label)
    }
}