package com.codecad.core.ast.primitive

import com.codecad.core.Utils
import com.codecad.core.exception.InterpreterException
import com.codecad.core.scope.EmptyScope
import com.codecad.core.scope.Scope
import com.codecad.core.World
import java.util.function.Consumer

abstract class Expr(val world: World) {
    abstract fun eval(scope: Scope = EmptyScope): Any?

    fun evalBoolean(scope: Scope = EmptyScope): Boolean {
        return true == eval(scope)
    }

    fun evalDouble(scope: Scope = EmptyScope): Double {
        return eval(scope) as Double
    }

    fun evalLiteral(scope: Scope = EmptyScope): Expr {
        return world.literal(evalDouble(scope))
    }

    open fun call(scope: Scope, args: Array<Any?>): Any? {
        return call(scope, args, false)
    }

    open fun call(scope: Scope, args: Array<Any?>, optional: Boolean): Any? {
        return CallExpr(world, this, LiteralExpr(world, args), optional)
            .eval(scope)
    }

    open fun assign(scope: Scope, obj: Any?, define: Boolean): Any? {
        throw InterpreterException("Assign not implemented for " + javaClass.simpleName)
    }

    open fun spread(scope: Scope, sink: Consumer<Any?>) {
        Utils.getIterator(eval(scope)).forEachRemaining(sink)
    }

    open fun collect(scope: Scope, sink: Consumer<Any?>) {
        sink.accept(eval(scope))
    }

    open fun bind(scope: Scope, define: Boolean): Expr {
        return this
    }

    open fun derivative(expr: Expr) : Expr {
        throw RuntimeException("not implemented")
    }







    operator fun unaryMinus() : Expr {
        return world.negate(this)
    }

    operator fun minus(right: Expr) : Expr {
        return world.sub(this, right)
    }

    operator fun minus(right: Double) : Expr {
        return world.sub(this, world.literal(right))
    }

    operator fun minus(right: Int) : Expr {
        return minus(right.toDouble())
    }

    operator fun plus(right: Expr) : Expr {
        return world.add(this, right)
    }

    operator fun plus(right: Double) : Expr {
        return world.add(this, world.literal(right))
    }

    operator fun plus(right: Int) : Expr {
        return plus(right.toDouble())
    }

    operator fun times(right: Expr) : Expr {
        return world.mul(this, right)
    }

    operator fun times(right: Double) : Expr {
        return world.mul(this, world.literal(right))
    }

    operator fun times(right: Int) : Expr {
        return times(right.toDouble())
    }

    operator fun div(right: Expr) : Expr {
        return world.div(this, right)
    }

    operator fun div(right: Double) : Expr {
        return world.div(this, world.literal(right))
    }

    operator fun div(right: Int) : Expr {
        return div(right.toDouble())
    }

    fun pow(right : Expr) : Expr {
        return world.pow(this, right)
    }

    fun pow(right : Double) : Expr {
        return world.pow(this, world.literal(right))
    }

    fun pow(right : Int) : Expr {
        return pow(right.toDouble())
    }

    fun sqrt() : Expr {
        return pow(0.5)
    }

    fun smaller(other: Expr) : Expr {
        return world.lt(this, other)
    }

    companion object{
        fun cos(expr: Expr) : Expr {
            return expr.world.cos(expr)
        }

        fun sin(expr: Expr) : Expr {
            return expr.world.sin(expr)
        }

        fun asin(expr: Expr) : Expr {
            return expr.world.asin(expr)
        }

        fun log(expr: Expr) : Expr {
            return expr.world.log(expr)
        }

        fun ifExpr(condition: Expr, left: Expr, right: Expr) : Expr {
            return condition.world.ifExpr(condition, left, right)
        }

        fun min(left: Expr, right: Expr) : Expr {
            return ifExpr(left.smaller(right), left, right)
        }

        fun max(left: Expr, right: Expr) : Expr {
            return ifExpr(left.smaller(right), right, left)
        }

        fun abs(expr: Expr) : Expr {
            return expr.world.abs(expr)
        }

        fun sign(expr: Expr) : Expr {
            return expr.world.sign(expr)
        }

        fun orLiteral(world: World, any: Any?) : Expr{
            return any as? Expr ?: world.literal(any)
        }
    }
}

operator fun Double.minus(right: Expr) : Expr {
    return right.world.literal(this) - right
}

operator fun Double.times(right: Expr) : Expr {
    return right.world.literal(this) * right
}

operator fun Double.div(right: Expr) : Expr {
    return right.world.literal(this) / right
}

fun Double.pow(right: Expr) : Expr {
    return right.world.literal(this).pow(right)
}