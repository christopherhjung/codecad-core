package com.codecad.core.parser.ast

import com.codecad.core.scope.EmptyScope
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class IfExpr(
    world: World,
    private val condition: Expr,
    private val trueBranch: Expr,
    private val falseBranch: Expr?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        if (condition.evalBoolean(scope)) {
            return trueBranch.eval(scope)
        }

        return falseBranch?.eval(scope)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newCondition = condition.bind(scope, false)
        val newTrueBranch = trueBranch.bind(scope, false)
        val newFalseBranch = falseBranch?.bind(scope, false)
        return if (newCondition is LiteralExpr) {
            if (newCondition.evalBoolean(EmptyScope)) {
                newTrueBranch
            } else {
                newFalseBranch!!
            }
        } else IfExpr(world, newCondition, newTrueBranch, newFalseBranch)
    }

    override fun derivative(expr: Expr): Expr {
        return world.ifExpr(condition, trueBranch.derivative(expr), falseBranch?.derivative(expr)!!)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IfExpr) return false
        return condition === other.condition && trueBranch === other.trueBranch && falseBranch === other.falseBranch
    }

    override fun hashCode(): Int {
        var result = condition.hashCode()
        result = 31 * result + trueBranch.hashCode()
        result = 31 * result + (falseBranch?.hashCode() ?: 0)
        return result
    }
}