package com.codecad.core.ast.primitive

import com.codecad.core.World
import com.codecad.core.scope.Scope

class LetExpr(world: World, private val ptrn: Expr, private val init: Expr?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        var initVal: Any? = null
        if (init != null) {
            initVal = init.eval(scope)
        }
        ptrn.assign(scope, initVal, true)
        return initVal
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newInit = init?.bind(scope, false)
        return LetExpr(world, ptrn.bind(scope, true), newInit)
    }
}