package com.codecad.core.parser.ast

import com.codecad.core.scope.*

class BlockExpr(private val exprs: Array<Expr>) : Expr {
    override fun eval(scope: Scope): Any? {
        var nestedScope = NestedScope.mutual(scope)
        var result = null as Any?
        for (expr in exprs) {
            result = expr.eval(nestedScope)
        }
        return result
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val nestedScope = NestedScope.mutual(scope)
        val newExprs = Array(exprs.size){
            exprs[it].bind(nestedScope, false)
        }
        return BlockExpr(newExprs)
    }
}