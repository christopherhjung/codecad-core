package com.codecad.core.ast.primitive

import com.codecad.core.scope.Scope
import com.codecad.core.scope.Slot
import com.codecad.core.World

class RefExpr(world: World, val slot: Slot) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        return slot.value
    }

    override fun assign(scope: Scope, obj: Any?, define: Boolean): Any? {
        slot.value = obj
        return obj
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is RefExpr && slot == other.slot
    }

    override fun hashCode(): Int {
        return System.identityHashCode(slot)
    }
}