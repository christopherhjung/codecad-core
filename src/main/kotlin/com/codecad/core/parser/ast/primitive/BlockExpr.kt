package com.codecad.core.parser.ast.primitive

import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class BlockExpr(world: World, private val exprs: Array<Expr>) : Expr(world) {
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
        return BlockExpr(world, newExprs)
    }
}