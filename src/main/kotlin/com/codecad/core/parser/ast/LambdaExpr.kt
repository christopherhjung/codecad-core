package com.codecad.core.parser.ast

import com.codecad.core.parser.controlflow.ReturnException
import com.codecad.core.scope.*

class LambdaExpr(private val param: Expr?, private val body: Expr?) : Expr {
    override fun call(scope: Scope, args: Array<Any?>): Any? {
        var scope = scope
        scope = NestedScope.mutual(scope)
        param!!.assign(scope, args, true)
        return try {
            body!!.eval(scope)
        } catch (e: ReturnException) {
            e.returnValue
        }
    }

    override fun eval(scope: Scope): Any {
        return ScopedExpr(scope, this)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        var scope = scope
        scope = NestedScope.readonly(scope)
        scope = NestedScope.mutual(scope)
        val newParam = param!!.bind(scope, true)
        val newBody = body!!.bind(scope, false)
        return LambdaExpr(newParam, newBody)
    }
}