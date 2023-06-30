package com.codecad.core.parser.ast.primitive

import com.codecad.core.parser.controlflow.ReturnException
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class LambdaExpr(world: World, private val param: Expr?, private val body: Expr?) : Expr(world) {
    override fun call(scope: Scope, args: Array<Any?>): Any? {
        val nestedScope = NestedScope.mutual(scope)
        param!!.assign(nestedScope, args, true)
        return try {
            body!!.eval(nestedScope)
        } catch (e: ReturnException) {
            e.returnValue
        }
    }

    override fun eval(scope: Scope): Any {
        return ScopedExpr(world, scope, this)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        var nestedScope = NestedScope.readonly(scope)
        nestedScope = NestedScope.mutual(nestedScope)
        val newParam = param!!.bind(nestedScope, true)
        val newBody = body!!.bind(nestedScope, false)
        return LambdaExpr(world, newParam, newBody)
    }
}