package com.codecad.core.ast.controlflow

import com.codecad.core.scope.Scope
import com.codecad.core.World
import com.codecad.core.ast.controlflow.exception.ReturnException
import com.codecad.core.ast.primitive.Expr

class ReturnExpr(world: World, private val expr: Expr?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        var returnValue: Any? = null
        if (expr != null) {
            returnValue = expr.eval(scope)
        }
        throw ReturnException(returnValue)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newExpr = expr?.bind(scope, define)
        return ReturnExpr(world, newExpr)
    }
}