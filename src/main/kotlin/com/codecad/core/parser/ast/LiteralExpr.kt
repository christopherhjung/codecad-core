package com.codecad.core.parser.ast

import com.codecad.core.scope.Scope

class LiteralExpr(private val value: Any?) : Expr {
    override fun eval(scope: Scope): Any? {
        return value
    }
}