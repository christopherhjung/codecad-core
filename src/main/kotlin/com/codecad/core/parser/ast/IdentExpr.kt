package com.codecad.core.parser.ast

import com.codecad.core.exception.InterpreterException
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class IdentExpr(
    world: World,
    private val key: String
) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        return scope.getObject(key)
    }

    override fun assign(scope: Scope, obj: Any?, define: Boolean): Any? {
        if (!scope.setObject(key, obj, define)) {
            throw InterpreterException("Assign was not successful")
        }
        return obj
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        if (define) {
            scope.setObject(key, null, true)
        }
        val value = scope.getValue(key) ?: return this
        val content = value.value
        return if (content is FunctionExpr) {
            LiteralExpr(world, content)
        } else RefExpr(world, value)
    }

    override fun derivative(expr: Expr): Expr {
        return if(this === expr){
            world.ONE
        }else{
            world.ZERO
        }
    }
}