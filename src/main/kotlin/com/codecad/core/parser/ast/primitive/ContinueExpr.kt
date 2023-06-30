package com.codecad.core.parser.ast.primitive

import com.codecad.core.parser.controlflow.ContinueException
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class ContinueExpr(world: World, private val label: String?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        throw ContinueException(label)
    }
}