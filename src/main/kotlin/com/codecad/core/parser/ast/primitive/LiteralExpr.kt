package com.codecad.core.parser.ast.primitive

import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class LiteralExpr(world: World, val value: Any?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        return value
    }

    override fun derivative(expr: Expr): Expr {
        return world.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is LiteralExpr && value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }
}