package com.codecad.core.sketch

import com.codecad.core.Plane
import com.codecad.core.Point2
import com.codecad.core.Point3
import com.codecad.core.Segment2
import com.codecad.core.parser.Op
import com.codecad.core.parser.ast.*
import com.codecad.core.scope.EmptyScope
import kotlin.math.abs
import kotlin.math.pow

class World {
    val ZERO = LiteralExpr(this, 0.0)
    val ONE = LiteralExpr(this, 1.0)
    val TWO = LiteralExpr(this, 2.0)

    val ORIGIN = Point2(ZERO, ZERO)
    val AXIS_X = Segment2(ORIGIN, Point2(ONE, ZERO))
    val AXIS_Y = Segment2(ORIGIN, Point2(ZERO, ONE))

    val XY = Plane(Point3(ZERO,ZERO,ONE), ZERO)
    val YZ = Plane(Point3(ONE,ZERO,ZERO), ZERO)
    val ZX = Plane(Point3(ZERO,ONE,ZERO), ZERO)

    private val sea = HashMap<Expr, Expr>()

    private fun unify(expr: Expr) : Expr{
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

    fun infix(left : Expr, right: Expr, op : Op) : Expr{
        return when(op){
            Op.Add -> add(left, right)
            Op.Sub -> sub(left, right)
            Op.Mul -> mul(left, right)
            Op.Div -> div(left, right)
            else -> throw NotImplementedError()
        }
    }

    fun prefix(left : Expr, op : Op) : Expr{
        return when(op){
            Op.Sub -> negate(left)
            else -> throw NotImplementedError()
        }
    }

    fun add(left : Expr, right: Expr) : Expr{
        return if(left === ZERO){
            right
        }else if(right === ZERO){
            left
        }else if(left is LiteralExpr && right is LiteralExpr){
            literal(left.evalDouble() + right.evalDouble())
        }else if(left === right){
            mul(TWO, right)
        }else{
            unify(InfixExpr(this, left, right, Op.Add))
        }
    }

    fun sub(left : Expr, right: Expr) : Expr {
        return if (left === ZERO) {
            negate(right)
        } else if (right === ZERO) {
            left
        } else if (left === right) {
            ZERO
        }else if(left is LiteralExpr && right is LiteralExpr){
            literal(left.evalDouble() - right.evalDouble())
        } else {
            unify(InfixExpr(this, left, right, Op.Sub))
        }
    }

    fun mul(left : Expr, right: Expr) : Expr{
        return if(left === ZERO || right === ZERO){
            ZERO
        }else if(left === ONE){
            right
        }else if(right === ONE){
            left
        }else if(left is LiteralExpr && right is LiteralExpr){
            literal(left.evalDouble() * right.evalDouble())
        }else if(left === right){
            pow(left, TWO)
        }else{
            unify(InfixExpr(this, left, right, Op.Mul))
        }
    }

    fun div(left : Expr, right: Expr) : Expr {
        return if (left === ZERO) {
            ZERO
        }else if (right === ONE){
            left
        }else if (left === right){
            ONE
        }else if (left is LiteralExpr && right is LiteralExpr) {
            literal(left.evalDouble() / right.evalDouble())
        }else {
            unify(InfixExpr(this, left, right, Op.Div))
        }
    }

    fun lt(left : Expr, right: Expr) : Expr{
        val newVal = InfixExpr(this, left, right, Op.Lt)
        if (left is LiteralExpr && right is LiteralExpr) {
            return literal(newVal.evalDouble())
        }

        return unify(newVal)
    }

    fun ifExpr(condition: Expr, left: Expr, right: Expr) : Expr {
        return if(condition is LiteralExpr){
            if(condition.evalBoolean(EmptyScope)){
                left
            }else{
                right
            }
        }else if(left is LiteralExpr && right is LiteralExpr && left === right){
            left
        }else{
            unify(IfExpr(this, condition, left, right))
        }
    }

    fun pow(left : Expr, right: Expr) : Expr{
        return if(right === ZERO){
            ONE
        }else if(right === ONE){
            left
        }else if(left === ZERO){
            ZERO
        }else if (left is LiteralExpr && right is LiteralExpr) {
            literal(left.evalDouble().pow(right.evalDouble()))
        }else{
            unify(PowExpr(this, left, right))
        }
    }

    fun cos(expr : Expr) : Expr{
        return if(expr is LiteralExpr){
            literal(kotlin.math.cos(expr.evalDouble()))
        }else{
            unify(CosExpr(this, expr))
        }
    }

    fun sin(expr : Expr) : Expr{
        return if(expr is LiteralExpr){
            literal(kotlin.math.sin(expr.evalDouble()))
        }else{
            unify(SinExpr(this, expr))
        }
    }

    fun asin(expr : Expr) : Expr{
        return if(expr is LiteralExpr){
            literal(kotlin.math.asin(expr.evalDouble()))
        }else{
            unify(Asin(this, expr))
        }
    }

    fun log(expr : Expr) : Expr{
        return if(expr is LiteralExpr){
            literal(kotlin.math.log(expr.evalDouble(), Math.E))
        }else{
            unify(LogExpr(this, expr))
        }
    }

    fun abs(expr: Expr) : Expr{
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

    fun literal(value: Any) : Expr{
        return when(value){
            0.0 -> ZERO
            1.0 -> ONE
            2.0 -> TWO
            else -> unify(LiteralExpr(this, value))
        }
    }
}