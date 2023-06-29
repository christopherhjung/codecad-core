package com.codecad.core.parser.ast

import com.codecad.core.parser.controlflow.ContinueException
import com.codecad.core.scope.Scope

class ContinueExpr(private val label: String?) : Expr {
    override fun eval(scope: Scope): Any? {
        throw ContinueException(label)
    }
}