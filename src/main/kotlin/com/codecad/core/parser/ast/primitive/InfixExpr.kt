package com.codecad.core.parser.ast.primitive

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.Op
import com.codecad.core.scope.Range
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World
import kotlin.math.pow

class InfixExpr(world: World, val lhs: Expr, val rhs: Expr, val op: Op) : Expr(world) {
    private fun compare(lhs: Any?, rhs: Any?): Int {
        if (lhs is String && rhs is String) {
            return lhs.compareTo((rhs as String?)!!)
        } else if (lhs is Int && rhs is Int) {
            return lhs.compareTo((rhs as Int?)!!)
        } else if (lhs is Double && rhs is Double) {
            return lhs.compareTo(rhs)
        }
        throw InterpreterException("Expected two String or Integer for comparison!")
    }

    private fun add(lhs: Any?, rhs: Any?): Any {
        if(lhs is Expr || rhs is Expr){
            val lhs = orLiteral(world, lhs)
            val rhs = orLiteral(world, rhs)
            return world.infix(lhs, rhs, Op.Add)
        }else if (lhs is Double || rhs is Double) {
            return (lhs as Number).toDouble() + (rhs as Number).toDouble()
        } else if (lhs is Int && rhs is Int) {
            return lhs + rhs
        } else if (lhs is String || rhs is String) {
            return lhs.toString() + rhs.toString()
        }

        throw InterpreterException("Expected two Integer or any String for addition!")
    }

    private fun sub(lhs: Any?, rhs: Any?): Any {
        if (lhs is Double && rhs is Double) {
            return lhs - rhs
        }
        if (lhs is Int && rhs is Int) {
            return lhs - rhs
        }
        throw InterpreterException("Expected two Integer for subtraction!")
    }

    private fun mul(lhs: Any?, rhs: Any?): Any {
        if (lhs is Double && rhs is Double) {
            return lhs * rhs
        }
        if (lhs is Int && rhs is Int) {
            return lhs * rhs
        }
        throw InterpreterException("Expected two Integer for multiplication!")
    }

    private fun div(lhs: Any?, rhs: Any?): Any {
        if (lhs is Double && rhs is Double) {
            return lhs / rhs
        }
        if (lhs is Int && rhs is Int) {
            return lhs / rhs
        }
        throw InterpreterException("Expected two Integer for division!")
    }

    private fun pow(lhs: Any?, rhs: Any?): Any {
        if (lhs is Double && rhs is Double) {
            return lhs.pow(rhs)
        }
        if (lhs is Int && rhs is Int) {
            return lhs.toDouble().pow(rhs.toDouble()).toInt()
        }
        throw InterpreterException("Expected two Integer for division!")
    }

    private fun and(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs and rhs
        }
        throw InterpreterException("Expected two Integer for division!")
    }

    private fun or(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs or rhs
        }
        throw InterpreterException("Expected two Integer for division!")
    }

    private fun xor(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs xor rhs
        }
        throw InterpreterException("Expected two Integer for division!")
    }

    private fun range(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return Range(lhs, rhs)
        }
        throw InterpreterException("Expected two Integer for range!")
    }

    override fun eval(scope: Scope): Any? {
        if (op == Op.Assign) {
            val rhsVal = rhs.eval(scope)
            return lhs.assign(scope, rhsVal, false)
        }
        val lhsVal = lhs.eval(scope)
        return when (op) {
            Op.And -> true == lhsVal && rhs.evalBoolean(scope)
            Op.Or -> true == lhsVal || rhs.evalBoolean(scope)
            Op.Nullish -> lhsVal ?: rhs.eval(scope)
            else -> {
                val rhsVal = rhs.eval(scope)
                when (op) {
                    Op.Eq -> lhsVal == rhsVal
                    Op.Ne -> lhsVal != rhsVal
                    Op.Lt -> compare(lhsVal, rhsVal) == -1
                    Op.Le -> compare(lhsVal, rhsVal) != 1
                    Op.Gt -> compare(lhsVal, rhsVal) == 1
                    Op.Ge -> compare(lhsVal, rhsVal) != -1
                    Op.Add -> add(lhsVal, rhsVal)
                    Op.Sub -> sub(lhsVal, rhsVal)
                    Op.Mul -> mul(lhsVal, rhsVal)
                    Op.Div -> div(lhsVal, rhsVal)
                    Op.Pow -> pow(lhsVal, rhsVal)
                    Op.BitAnd -> and(lhsVal, rhsVal)
                    Op.BitOr -> or(lhsVal, rhsVal)
                    Op.BitXor -> xor(lhsVal, rhsVal)
                    Op.Range -> range(lhsVal, rhsVal)
                    Op.AssignAdd -> lhs.assign(scope, add(lhsVal, rhsVal), false)
                    Op.AssignSub -> lhs.assign(scope, sub(lhsVal, rhsVal), false)
                    Op.AssignMul -> lhs.assign(scope, mul(lhsVal, rhsVal), false)
                    Op.AssignDiv -> lhs.assign(scope, div(lhsVal, rhsVal), false)
                    else -> throw InterpreterException("Not implemented $op operation!")
                }
            }
        }
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newLhs = lhs.bind(scope, false)
        val newRhs = rhs.bind(scope, false)
        val newExpr = world.infix(newLhs, newRhs, op)
        if (newLhs is LiteralExpr && rhs is LiteralExpr) {
            val newValue = newExpr.eval(scope)
            return world.literal(newValue)
        }
        return newExpr
    }

    override fun derivative(expr: Expr): Expr {
        return when(op){
            Op.Add -> lhs.derivative(expr) + rhs.derivative(expr)
            Op.Sub -> lhs.derivative(expr) - rhs.derivative(expr)
            Op.Mul -> lhs.derivative(expr) * rhs + lhs * rhs.derivative(expr)
            Op.Div -> (lhs.derivative(expr) * rhs - lhs * rhs.derivative(expr)) / rhs.pow(2)
            else -> throw InterpreterException("Not implemented derivative for $op")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InfixExpr) return false
        return lhs === other.lhs && rhs === other.rhs && op === other.op
    }

    override fun hashCode(): Int {
        var result = lhs.hashCode()
        result = 31 * result + rhs.hashCode()
        result = 31 * result + op.hashCode()
        return result
    }
}