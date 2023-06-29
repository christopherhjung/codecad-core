package com.codecad.core.parser.ast

import com.codecad.core.scope.*

class BlockExpr(private val exprs: Array<Expr?>) : Expr {
    override fun eval(scope: Scope): Any? {
        var scope = scope
        scope = NestedScope.mutual(scope)
        var result = null as Any?
        for (expr in exprs) {
            result = expr!!.eval(scope)
        }
        return result
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        var scope = scope
        scope = NestedScope.mutual(scope)
        val newExprs = arrayOfNulls<Expr>(
            exprs.size
        )
        var idx = 0
        for (expr in exprs) {
            newExprs[idx++] = expr!!.bind(scope, false)
        }
        return BlockExpr(newExprs)
    }
}