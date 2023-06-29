package com.codecad.core.parser.ast

import com.codecad.core.scope.Scope

class LetExpr(private val ptrn: Expr, private val init: Expr?) : Expr {
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
        return LetExpr(ptrn.bind(scope, true), newInit)
    }
}