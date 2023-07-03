package com.codecad.core.ast.primitive

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.ObjectFunction
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.World

class CallExpr(world: World,
               private val callee: Expr,
               private val arg: Expr,
               private val optional: Boolean = false
) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        val callee = callee.eval(scope)
        if (callee == null) {
            if (optional) return null
            throw InterpreterException("Null pointer exception")
        }
        val nestedScope = NestedScope.mutual(scope)
        val arg = arg.eval(scope) as Array<Any?>
        if (callee is Expr) {
            return callee.call(nestedScope, arg)
        } else if (callee is ObjectFunction) {
            return callee.call(nestedScope, arg)
        }
        throw InterpreterException("is not callable!")
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newCallee = callee.bind(scope, define)
        val newArg = arg.bind(scope, define)
        return CallExpr(world, newCallee, newArg, optional)
    }
}