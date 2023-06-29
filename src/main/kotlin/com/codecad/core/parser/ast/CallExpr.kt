package com.codecad.core.parser.ast

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.ObjectFunction
import com.codecad.core.scope.Scope

class CallExpr(
    private val callee: Expr,
    private val arg: Expr,
    private val optional: Boolean = false
) : Expr {
    override fun eval(scope: Scope): Any? {
        val callee = callee.eval(scope)
        if (callee == null) {
            if (optional) return null
            throw InterpreterException("Null pointer exception")
        }
        val arg = arg.eval(scope) as Array<Any?>
        if (callee is Expr) {
            return callee.call(scope, arg)
        } else if (callee is ObjectFunction) {
            return callee.call(arg)
        }
        throw InterpreterException("is not callable!")
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newCallee = callee.bind(scope, define)
        val newArg = arg.bind(scope, define)
        return CallExpr(newCallee, newArg, optional)
    }
}