package com.codecad.core.ast.primitive

import com.codecad.core.parser.Op
import com.codecad.core.scope.Scope
import com.codecad.core.World

class PostfixExpr(world: World, private val expr: Expr, private val op: Op) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        val value = expr.eval(scope)
        if (value is Int) {
            var valInt = value
            if (op == Op.Inc) {
                valInt++
            } else if (op == Op.Dec) {
                valInt--
            }
            expr.assign(scope, valInt, false)
        }
        return value
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newExpr = expr.bind(scope, false)
        return PostfixExpr(world, newExpr, op)
    }
}