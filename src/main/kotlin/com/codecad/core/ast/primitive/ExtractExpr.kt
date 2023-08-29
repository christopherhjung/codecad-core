package com.codecad.core.ast.primitive

import com.codecad.core.Utils
import com.codecad.core.World
import com.codecad.core.exception.InterpreterException
import com.codecad.core.scope.Scope
import java.util.function.Consumer

class ExtractExpr(world: World, val tuple: Expr, val index: Expr) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        val arr = tuple.eval(scope) as Array<*>
        val indexValue = index.eval(scope) as Int
        return arr[indexValue]
    }
}