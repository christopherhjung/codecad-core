package com.codecad.core.sketch

import com.codecad.core.LineSegment
import com.codecad.core.Point
import kotlin.math.abs
import kotlin.math.pow

class World {
    val ZERO = Literal(this, 0.0)
    val ONE = Literal(this, 1.0)
    val TWO = Literal(this, 2.0)

    val ORIGIN = Point(ZERO, ZERO)
    val AXIS_X = LineSegment(ORIGIN, Point(ONE, ZERO))
    val AXIS_Y = LineSegment(ORIGIN, Point(ZERO, ONE))

    private val sea = HashMap<Expr, Expr>()

    private fun unify(expr: Expr) : Expr{
        return sea.putIfAbsent(expr, expr) ?: expr
    }

    fun negate(expr : Expr) : Expr {
        return if(expr === ZERO){
            ZERO
        }else if(expr is Literal){
            literal(-expr.evalDouble())
        }else{
            unify(NegExpr(this, expr))
        }
    }

    fun add(left : Expr, right: Expr) : Expr{
        return if(left === ZERO){
            right
        }else if(right === ZERO){
            left
        }else if(Literal.isDouble(left) && Literal.isDouble(right)){
            literal(left.evalDouble() + right.evalDouble())
        }else if(left === right){
            mul(TWO, right)
        }else{
            unify(AddExpr(this, left, right))
        }
    }

    fun sub(left : Expr, right: Expr) : Expr {
        return if (left === ZERO) {
            negate(right)
        } else if (right === ZERO) {
            left
        } else if (left === right) {
            ZERO
        } else if (Literal.isDouble(left) && Literal.isDouble(right)) {
            literal(left.evalDouble() - right.evalDouble())
        } else {
            unify(SubExpr(this,left, right))
        }
    }

    fun mul(left : Expr, right: Expr) : Expr{
        return if(left === ZERO || right === ZERO){
            ZERO
        }else if(left === ONE){
            right
        }else if(right === ONE){
            left
        }else if(Literal.isDouble(left) && Literal.isDouble(right)){
            literal(left.evalDouble() * right.evalDouble())
        }else if(left === right){
            pow(left, TWO)
        }else{
            unify(TimesExpr(this, left, right))
        }
    }

    fun div(left : Expr, right: Expr) : Expr {
        return if (left === ZERO) {
            ZERO
        }else if (right === ONE){
            left
        }else if (left === right){
            ONE
        }else if (Literal.isDouble(left) && Literal.isDouble(right)) {
            literal(left.evalDouble() / right.evalDouble())
        }else {
            unify(DivExpr(this, left, right))
        }
    }

    fun pow(left : Expr, right: Expr) : Expr{
        return if(right === ZERO){
            ONE
        }else if(right === ONE){
            left
        }else if(left === ZERO){
            ZERO
        }else if(Literal.isDouble(left) && Literal.isDouble(right)){
            literal(left.evalDouble().pow(right.evalDouble()))
        }else{
            unify(PowExpr(this, left, right))
        }
    }

    fun lt(left : Expr, right: Expr) : Expr{
        val newVal =  LtExpr(this, left, right)
        if(left is Literal && right is Literal){
            return literal(newVal.evalDouble())
        }

        return unify(newVal)
    }

    fun cos(expr : Expr) : Expr{
        return if(Literal.isDouble(expr)){
            literal(kotlin.math.cos(expr.evalDouble()))
        }else{
            unify(CosExpr(this, expr))
        }
    }

    fun sin(expr : Expr) : Expr{
        return if(Literal.isDouble(expr)){
            literal(kotlin.math.sin(expr.evalDouble()))
        }else{
            unify(SinExpr(this, expr))
        }
    }

    fun asin(expr : Expr) : Expr{
        return if(Literal.isDouble(expr)){
            literal(kotlin.math.asin(expr.evalDouble()))
        }else{
            unify(Asin(this, expr))
        }
    }

    fun log(expr : Expr) : Expr{
        return if(Literal.isDouble(expr)){
            literal(kotlin.math.log(expr.evalDouble(), Math.E))
        }else{
            unify(LogExpr(this, expr))
        }
    }

    fun ifExpr(condition: Expr, left: Expr, right: Expr) : Expr {
        return if(Literal.isBoolean(condition)){
            if(condition.evalBoolean()){
                left
            }else{
                right
            }
        }else if(left is Literal && right is Literal && left == right){
            left
        }else{
            unify(IfExpr(this, condition, left, right))
        }
    }

    fun abs(expr: Expr) : Expr{
        return if(Literal.isDouble(expr)){
            literal(abs(expr.evalDouble()))
        }else{
            unify(AbsExpr(this, expr))
        }
    }

    fun sign(expr: Expr) : Expr {
        return if(Literal.isDouble(expr)){
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
            else -> unify(Literal(this, value))
        }
    }
}