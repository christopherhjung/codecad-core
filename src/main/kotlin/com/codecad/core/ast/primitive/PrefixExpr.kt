package com.codecad.core.ast.primitive

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.Op
import com.codecad.core.scope.Scope
import com.codecad.core.World
import java.util.function.Consumer

class PrefixExpr(world: World, private val expr: Expr, private val op: Op) : Expr(world) {
    private fun sub(value: Any?): Any {
        if (value is Double) {
            return -value
        }
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
        return PrefixExpr(world, newExpr, op)
    }

    override fun derivative(expr: Expr): Expr {
        return when(op){
            Op.Sub -> -this.expr.derivative(expr)
            else -> throw InterpreterException("Not implemented derivative for $op")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrefixExpr) return false
        return expr === other.expr || op === other.op
    }

    override fun hashCode(): Int {
        var result = expr.hashCode()
        result = 31 * result + op.hashCode()
        return result
    }
}