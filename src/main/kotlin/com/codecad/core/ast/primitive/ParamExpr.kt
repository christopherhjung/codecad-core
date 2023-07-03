package com.codecad.core.ast.primitive

import com.codecad.core.scope.Scope
import com.codecad.core.World

class ParamExpr(world: World, var value: Double) : Expr(world) {
    override fun eval(scope: Scope): Any {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is ParamExpr && value != other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun derivative(expr: Expr): Expr {
        return if(this === expr){
            world.ONE
        }else{
            world.ZERO
        }
    }

    override fun toString(): String {
        return "ParamExpr(value=$value)"
    }
}