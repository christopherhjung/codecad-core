package com.codecad.core.parser.ast

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.Op
import com.codecad.core.scope.Scope
import java.util.function.Consumer

class PrefixExpr(private val expr: Expr?, private val op: Op) : Expr {
    private fun sub(value: Any?): Any {
        if (value is Int) {
            return -(value as Int?)!!
        }
        throw InterpreterException("Expected integer for unary sub!")
    }

    override fun eval(scope: Scope): Any? {
        val value = expr!!.eval(scope)
        when (op) {
            Op.Sub -> return sub(value)
            Op.Add -> return value
            Op.Not -> return java.lang.Boolean.FALSE == value
        }
        throw InterpreterException("Not implemented $op operation!")
    }

    override fun collect(scope: Scope, sink: Consumer<Any?>) {
        if (op == Op.Spread) {
            expr!!.spread(scope, sink)
        } else {
            super.collect(scope, sink)
        }
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newExpr = expr!!.bind(scope, false)
        return PrefixExpr(newExpr, op)
    }
}