package com.codecad.core.parser.ast

import com.codecad.core.parser.controlflow.ReturnException
import com.codecad.core.scope.Scope

class ReturnExpr(private val expr: Expr?) : Expr {
    override fun eval(scope: Scope): Any? {
        var returnValue: Any? = null
        if (expr != null) {
            returnValue = expr.eval(scope)
        }
        throw ReturnException(returnValue)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newExpr = expr!!.bind(scope, define)
        return ReturnExpr(newExpr)
    }
}