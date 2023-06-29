package com.codecad.core.parser.ast

import com.codecad.core.scope.EmptyScope
import com.codecad.core.scope.Scope

class IfExpr(private val condition: Expr?, private val trueBranch: Expr?, private val falseBranch: Expr?) : Expr {
    override fun eval(scope: Scope): Any? {
        if (condition!!.evalBoolean(scope)) {
            return trueBranch!!.eval(scope)
        } else if (falseBranch != null) {
            return falseBranch.eval(scope)
        }
        return null
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newCondition = condition!!.bind(scope, false)
        val newTrueBranch = trueBranch!!.bind(scope, false)
        val newFalseBranch = falseBranch?.bind(scope, false)
        return if (newCondition is LiteralExpr) {
            if (newCondition.evalBoolean(EmptyScope)) {
                newTrueBranch
            } else {
                newFalseBranch!!
            }
        } else IfExpr(newCondition, newTrueBranch, newFalseBranch)
    }
}