package com.codecad.core.parser.ast

import com.codecad.core.parser.controlflow.BreakException
import com.codecad.core.scope.Scope

class BreakExpr(private val label: String?) : Expr {
    override fun eval(scope: Scope): Any? {
        throw BreakException(label)
    }
}