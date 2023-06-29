package com.codecad.core.parser.ast

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.Op
import com.codecad.core.scope.Scope
import java.util.function.Consumer

class PrefixExpr(private val expr: Expr, private val op: Op) : Expr {
    private fun sub(value: Any?): Any {
        if (value is Int) {
            return -value
        }
        throw InterpreterException("Expected integer for unary sub!")
    }

    override fun eval(scope: Scope): Any? {
        val value = expr.eval(scope)
        return when (op) {
            Op.Sub -> sub(value)
            Op.Add -> value
            Op.Not -> false == value
            else -> throw InterpreterException("Not implemented $op operation!")
        }
    }

    override fun collect(scope: Scope, sink: Consumer<Any?>) {
        if (op == Op.Spread) {
            expr.spread(scope, sink)
        } else {
            super.collect(scope, sink)
        }
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newExpr = expr.bind(scope, false)
        return PrefixExpr(newExpr, op)
    }
}