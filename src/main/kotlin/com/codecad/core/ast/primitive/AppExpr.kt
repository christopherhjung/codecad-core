package com.codecad.core.ast.primitive

import com.codecad.core.World
import com.codecad.core.scope.Scope

class AppExpr(world: World, val fn: Expr, val arg : TupleExpr) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        return null
    }

}