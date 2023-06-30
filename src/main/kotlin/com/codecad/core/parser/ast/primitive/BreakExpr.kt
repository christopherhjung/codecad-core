package com.codecad.core.parser.ast.primitive

import com.codecad.core.parser.controlflow.BreakException
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class BreakExpr(world: World, private val label: String?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        throw BreakException(label)
    }
}