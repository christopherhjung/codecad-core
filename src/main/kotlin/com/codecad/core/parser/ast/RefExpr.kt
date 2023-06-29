package com.codecad.core.parser.ast

import com.codecad.core.scope.Scope
import com.codecad.core.scope.Slot

class RefExpr(private val slot: Slot) : Expr {
    override fun eval(scope: Scope): Any? {
        return slot.value
    }

    override fun assign(scope: Scope, obj: Any?, define: Boolean): Any? {
        slot.value = obj
        return obj
    }
}