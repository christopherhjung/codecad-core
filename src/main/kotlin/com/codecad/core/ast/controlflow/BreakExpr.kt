package com.codecad.core.ast.controlflow

import com.codecad.core.scope.Scope
import com.codecad.core.World
import com.codecad.core.ast.controlflow.exception.BreakException
import com.codecad.core.ast.primitive.Expr

class BreakExpr(world: World, private val label: String?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        throw BreakException(label)
    }
}