package com.codecad.core.ast.primitive

import com.codecad.core.World
import com.codecad.core.scope.Scope

class LiteralExpr(world: World, val value: Any?) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        return value
    }

    override fun derivative(expr: Expr): Expr {
        return world.Zero
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is LiteralExpr && value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "LiteralExpr(value=$value)"
    }
}