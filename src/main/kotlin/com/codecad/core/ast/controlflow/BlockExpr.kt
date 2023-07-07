package com.codecad.core.ast.controlflow

import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr

class BlockExpr(world: World, val exprs: Array<Expr>) : Expr(world) {
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