package com.codecad.core.sketch

import com.codecad.core.Plane
import com.codecad.core.Vec2
import com.codecad.core.Point3
import com.codecad.core.Segment2
import com.codecad.core.parser.Op
import com.codecad.core.parser.ast.primitive.*
import com.codecad.core.scope.EmptyScope
import kotlin.math.abs
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
        }else if(lhs is LiteralExpr && rhs is LiteralExpr){
            literal(lhs.evalDouble() + rhs.evalDouble())
        }else if(lhs === rhs){
            mul(TWO, rhs)
        }else{
            unify(InfixExpr(this, lhs, rhs, Op.Add))
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
        }else if(lhs is LiteralExpr && rhs is LiteralExpr){
            literal(lhs.evalDouble() * rhs.evalDouble())
        }else if(lhs === rhs){
            pow(lhs, TWO)
        }else{
            unify(InfixExpr(this, lhs, rhs, Op.Mul))
        }
    }

    fun div(lhs : Expr, rhs: Expr) : Expr {
        return if (lhs === ZERO) {
            ZERO
        }else if (rhs === ONE){
            lhs
        }else if (lhs === rhs){
            ONE
        }else if (lhs is LiteralExpr && rhs is LiteralExpr) {
            literal(lhs.evalDouble() / rhs.evalDouble())
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

    fun pow(lhs : Expr, rhs: Expr) : Expr {
        return if(rhs === ZERO){
            ONE
        }else if(rhs === ONE){
            lhs
        }else if(lhs === ZERO){
            ZERO
        }else if (lhs is LiteralExpr && rhs is LiteralExpr) {
            literal(lhs.evalDouble().pow(rhs.evalDouble()))
        }else{
            unify(PowExpr(this, lhs, rhs))
        }
    }

    fun cos(expr : Expr) : Expr {
        return if(expr is LiteralExpr){
            literal(kotlin.math.cos(expr.evalDouble()))
        }else{
            unify(CosExpr(this, expr))
        }
    }

    fun sin(expr : Expr) : Expr {
        return if(expr is LiteralExpr){
            literal(kotlin.math.sin(expr.evalDouble()))
        }else{
            unify(SinExpr(this, expr))
        }
    }

    fun asin(expr : Expr) : Expr {
        return if(expr is LiteralExpr){
            literal(kotlin.math.asin(expr.evalDouble()))
        }else{
            unify(Asin(this, expr))
        }
    }

    fun log(expr : Expr) : Expr {
        return if(expr is LiteralExpr){
            literal(kotlin.math.log(expr.evalDouble(), Math.E))
        }else{
            unify(LogExpr(this, expr))
        }
    }

    fun abs(expr: Expr) : Expr {
        return if(expr is LiteralExpr){
            literal(abs(expr.evalDouble()))
        }else{
            unify(AbsExpr(this, expr))
        }
    }

    fun sign(expr: Expr) : Expr {
        return if(expr is LiteralExpr){
            literal(kotlin.math.sign(expr.evalDouble()))
        }else{
            unify(SignExpr(this, expr))
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