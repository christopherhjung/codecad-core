package com.codecad.core.parser.ast

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.Op
import com.codecad.core.scope.Range
import com.codecad.core.scope.Scope

class InfixExpr(private val lhs: Expr?, private val rhs: Expr?, private val op: Op) : Expr {
    private fun compare(lhs: Any?, rhs: Any?): Int {
        if (lhs is String && rhs is String) {
            return lhs.compareTo((rhs as String?)!!)
        } else if (lhs is Int && rhs is Int) {
            return lhs.compareTo((rhs as Int?)!!)
        }
        throw InterpreterException("Expected two String or Integer for comparison!")
    }

    private fun add(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs + rhs
        } else if (lhs is String) {
            return lhs.toString() + rhs.toString()
        } else if (rhs is String) {
            return lhs.toString() + rhs.toString()
        }
        throw InterpreterException("Expected two Integer or any String for addition!")
    }

    private fun sub(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs - rhs
        }
        throw InterpreterException("Expected two Integer for subtraction!")
    }

    private fun mul(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs * rhs
        }
        throw InterpreterException("Expected two Integer for multiplication!")
    }

    private fun div(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return lhs / rhs
        }
        throw InterpreterException("Expected two Integer for division!")
    }

    private fun pow(lhs: Any?, rhs: Any?): Any {
        if (lhs is Int && rhs is Int) {
            return Math.pow(lhs.toDouble(), rhs.toDouble()).toInt()
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
            val rhsVal = rhs!!.eval(scope)
            return lhs!!.assign(scope, rhsVal, false)
        }
        val lhsVal = lhs!!.eval(scope)
        when (op) {
            Op.And -> return java.lang.Boolean.TRUE == lhsVal && java.lang.Boolean.TRUE == rhs!!.eval(scope)
            Op.Or -> return java.lang.Boolean.TRUE == lhsVal || java.lang.Boolean.TRUE == rhs!!.eval(scope)
            Op.Nullish -> return lhsVal ?: rhs!!.eval(scope)
        }
        val rhsVal = rhs!!.eval(scope)
        when (op) {
            Op.Eq -> return lhsVal == rhsVal
            Op.Ne -> return lhsVal != rhsVal
            Op.Lt -> return compare(lhsVal, rhsVal) == -1
            Op.Le -> return compare(lhsVal, rhsVal) != 1
            Op.Gt -> return compare(lhsVal, rhsVal) == 1
            Op.Ge -> return compare(lhsVal, rhsVal) != -1
            Op.Add -> return add(lhsVal, rhsVal)
            Op.Sub -> return sub(lhsVal, rhsVal)
            Op.Mul -> return mul(lhsVal, rhsVal)
            Op.Div -> return div(lhsVal, rhsVal)
            Op.Pow -> return pow(lhsVal, rhsVal)
            Op.BitAnd -> return and(lhsVal, rhsVal)
            Op.BitOr -> return or(lhsVal, rhsVal)
            Op.BitXor -> return xor(lhsVal, rhsVal)
            Op.Range -> return range(lhsVal, rhsVal)
            Op.AssignAdd -> return lhs.assign(scope, add(lhsVal, rhsVal), false)
            Op.AssignSub -> return lhs.assign(scope, sub(lhsVal, rhsVal), false)
            Op.AssignMul -> return lhs.assign(scope, mul(lhsVal, rhsVal), false)
            Op.AssignDiv -> return lhs.assign(scope, div(lhsVal, rhsVal), false)
        }
        throw InterpreterException("Not implemented $op operation!")
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newLhs = lhs!!.bind(scope, false)
        val newRhs = rhs!!.bind(scope, false)
        val newExpr = InfixExpr(newLhs, newRhs, op)
        if (newLhs is LiteralExpr && rhs is LiteralExpr) {
            val newValue = newExpr.eval(scope)
            return LiteralExpr(newValue)
        }
        return newExpr
    }
}