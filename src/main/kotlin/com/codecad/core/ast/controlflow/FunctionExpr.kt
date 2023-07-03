package com.codecad.core.ast.controlflow

import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.World
import com.codecad.core.ast.controlflow.exception.ReturnException
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.ScopedExpr

class FunctionExpr(
    world: World,
    var name: String,
    var params: Expr?,
    var body: Expr?) : Expr(world) {

    override fun call(scope: Scope, args: Array<Any?>): Any? {
        val mutual = MutualScope()
        params!!.assign(mutual, args, true)
        return try {
            body!!.eval(mutual)
        } catch (e: ReturnException) {
            e.returnValue
        }
    }

    override fun eval(scope: Scope): Any {
        return ScopedExpr(world, scope, this)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newFunctionExpr = scope.getObject(name) as FunctionExpr
        val fnScope: Scope = NestedScope.mutual(scope)
        val newParams = params!!.bind(fnScope, true)
        val newBody = body!!.bind(fnScope, false)
        newFunctionExpr.params = newParams
        newFunctionExpr.body = newBody
        return newFunctionExpr
    }

    fun bindFunction(scope: Scope) {
        val newFunctionExpr = FunctionExpr(world, name, null, null)
        scope.setObject(name, newFunctionExpr, true)
    }
}