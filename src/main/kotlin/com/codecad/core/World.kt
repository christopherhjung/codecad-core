package com.codecad.core

import com.codecad.core.Plane
import com.codecad.core.Point3
import com.codecad.core.Segment2
import com.codecad.core.Vec2
import com.codecad.core.ast.primitive.*
import com.codecad.core.parser.Op
import com.codecad.core.scope.EmptyScope
import kotlin.math.pow

class World {
    val ZERO = LiteralExpr(this, 0.0)
    val ONE = LiteralExpr(this, 1.0)
    val TWO = LiteralExpr(this, 2.0)

    val ORIGIN = Vec2(ZERO, ZERO)
    val AXIS_X = Segment2(ORIGIN, Vec2(ONE, ZERO))
    val AXIS_Y = Segment2(ORIGIN, Vec2(ZERO, ONE))

    val XY = Plane(Point3(ZERO,ZERO,ONE), ZERO)
    val YZ = Plane(Point3(ONE,ZERO,ZERO), ZERO)
    val ZX = Plane(Point3(ZERO,ONE,ZERO), ZERO)

    private val sea = HashMap<Expr, Expr>()

    private fun unify(expr: Expr) : Expr {
        return sea.putIfAbsent(expr, expr) ?: expr
    }

    fun negate(expr : Expr) : Expr {
        return if(expr === ZERO){
            ZERO
        }else if(expr is LiteralExpr){
            literal(-expr.evalDouble())
        }else{
            unify(PrefixExpr(this, expr, Op.Sub))
        }
    }

    fun infix(lhs : Expr, rhs: Expr, op : Op) : Expr {
        return when(op){
            Op.Add -> add(lhs, rhs)
            Op.Sub -> sub(lhs, rhs)
            Op.Mul -> mul(lhs, rhs)
            Op.Div -> div(lhs, rhs)
            Op.Assign -> unify(InfixExpr(this, lhs, rhs, op))
            else -> throw NotImplementedError()
        }
    }

    fun prefix(lhs : Expr, op : Op) : Expr {
        return when(op){
            Op.Sub -> negate(lhs)
            else -> throw NotImplementedError()
        }
    }

    fun add(lhs : Expr, rhs: Expr) : Expr {
        return if(lhs === ZERO){
            rhs
        }else if(rhs === ZERO){
            lhs
        }else if(lhs === rhs){
            mul(TWO, rhs)
        }else if(rhs is LiteralExpr){
            if(lhs is LiteralExpr){
                literal(lhs.evalDouble() + rhs.evalDouble())
            }else if(lhs is InfixExpr && lhs.op == Op.Add && lhs.rhs is LiteralExpr){
                add(lhs.lhs, add(lhs.rhs, rhs))
            }else{
                unify(InfixExpr(this, lhs, rhs, Op.Add))
            }
        }else{
            unify(InfixExpr(this, rhs, lhs, Op.Add))
        }
    }

    fun sub(lhs : Expr, rhs: Expr) : Expr {
        return if (lhs === ZERO) {
            negate(rhs)
        } else if (rhs === ZERO) {
            lhs
        } else if (lhs === rhs) {
            ZERO
        }else if(lhs is LiteralExpr && rhs is LiteralExpr){
            literal(lhs.evalDouble() - rhs.evalDouble())
        } else {
            unify(InfixExpr(this, lhs, rhs, Op.Sub))
        }
    }

    fun mul(lhs : Expr, rhs: Expr) : Expr {
        return if(lhs === ZERO || rhs === ZERO){
            ZERO
        }else if(lhs === ONE){
            rhs
        }else if(rhs === ONE){
            lhs
        }else if(lhs === rhs){
            pow(lhs, TWO)
        }else if(rhs is LiteralExpr){
            if(lhs is LiteralExpr){
                literal(lhs.evalDouble() * rhs.evalDouble())
            }else if(lhs is InfixExpr && lhs.op == Op.Mul && lhs.rhs is LiteralExpr){
                mul(lhs.lhs,  mul(lhs.rhs, rhs))
            }else{
                unify(InfixExpr(this, lhs, rhs, Op.Mul))
            }
        }else{
            unify(InfixExpr(this, rhs, lhs, Op.Mul))
        }
    }

    fun div(lhs : Expr, rhs: Expr) : Expr {
        return if (lhs === ZERO) {
            ZERO
        }else if (rhs === ONE){
            lhs
        }else if (lhs === rhs){
            ONE
        }else if (rhs is LiteralExpr) {
            if(lhs is LiteralExpr){
                literal(lhs.evalDouble() / rhs.evalDouble())
            }else{
                mul(lhs, literal(1.0 / rhs.evalDouble()))
            }
        }else {
            unify(InfixExpr(this, lhs, rhs, Op.Div))
        }
    }

    fun lt(lhs : Expr, rhs: Expr) : Expr {
        val newVal = InfixExpr(this, lhs, rhs, Op.Lt)
        if (lhs is LiteralExpr && rhs is LiteralExpr) {
            return literal(newVal.evalDouble())
        }

        return unify(newVal)
    }

    fun ifExpr(condition: Expr, lhs: Expr, rhs: Expr) : Expr {
        return if(condition is LiteralExpr){
            if(condition.evalBoolean(EmptyScope)){
                lhs
            }else{
                rhs
            }
        }else if(lhs is LiteralExpr && rhs is LiteralExpr && lhs === rhs){
            lhs
        }else{
            unify(IfExpr(this, condition, lhs, rhs))
        }
    }

    fun pow(base : Expr, exp: Expr) : Expr {
        return if(exp === ZERO){
            ONE
        }else if(exp === ONE){
            base
        }else if(base === ZERO){
            ZERO
        }else if(base is PowExpr){
            pow(base.base, mul(base.exp, exp))
        }else if (base is LiteralExpr && exp is LiteralExpr) {
            literal(base.evalDouble().pow(exp.evalDouble()))
        }else{
            unify(PowExpr(this, base, exp))
        }
    }

    fun cos(expr : Expr) : Expr {
        val cosExpr = CosExpr(this, expr)
        return if(expr is LiteralExpr){
            cosExpr.evalLiteral()
        }else{
            unify(cosExpr)
        }
    }

    fun sin(expr : Expr) : Expr {
        val sinExpr = SinExpr(this, expr)
        return if(expr is LiteralExpr){
            sinExpr.evalLiteral()
        }else{
            unify(sinExpr)
        }
    }

    fun asin(expr : Expr) : Expr {
        val asin = Asin(this, expr)
        return if(expr is LiteralExpr){
            asin.evalLiteral()
        }else{
            unify(asin)
        }
    }

    fun log(expr : Expr) : Expr {
        val log = LogExpr(this, expr)
        return if(expr is LiteralExpr){
            log.evalLiteral()
        }else if(expr is ExpExpr){
            expr.expr
        }else{
            unify(log)
        }
    }

    fun exp(expr : Expr) : Expr {
        val exp = ExpExpr(this, expr)
        return if(expr is LiteralExpr){
            exp.evalLiteral()
        }else if(expr is LogExpr){
            expr.expr
        }else{
            unify(exp)
        }
    }

    fun abs(expr: Expr) : Expr {
        val abs = AbsExpr(this, expr)
        return if(expr is LiteralExpr){
            abs.evalLiteral()
        }else{
            unify(abs)
        }
    }

    fun sign(expr: Expr) : Expr {
        val sign = SignExpr(this, expr)
        return if(expr is LiteralExpr){
            sign.evalLiteral()
        }else{
            unify(sign)
        }
    }

    fun literal(value: Any?) : Expr {
        return when(value){
            0.0 -> ZERO
            1.0 -> ONE
            2.0 -> TWO
            else -> unify(LiteralExpr(this, value))
        }
    }
}