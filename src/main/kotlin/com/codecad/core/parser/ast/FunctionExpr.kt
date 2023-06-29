package com.codecad.core.parser.ast

import com.codecad.core.parser.controlflow.ReturnException
import com.codecad.core.scope.*

class FunctionExpr(var name: String, var params: Expr?, var body: Expr?) : Expr {

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
        return ScopedExpr(scope, this)
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
        val newFunctionExpr = FunctionExpr(name, null, null)
        scope.setObject(name, newFunctionExpr, true)
    }
}