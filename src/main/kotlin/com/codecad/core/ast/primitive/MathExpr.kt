package com.codecad.core.ast.primitive

import com.codecad.core.scope.Scope
import com.codecad.core.World
import kotlin.math.*

abstract class MathExpr(world: World) : Expr(world){
    abstract fun arg() : Expr
}

class AbsExpr(world: World, val left: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return abs(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        val derivative = left.derivative(expr)
        return ifExpr(left.smaller(world.ZERO), -derivative, derivative)
    }

    override fun arg(): Expr {
        return left
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsExpr && left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}

class SignExpr(world: World, val left: Expr) : MathExpr(world ){
    override fun eval(scope: Scope) : Any {
        return sign(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return world.ZERO
    }

    override fun arg(): Expr {
        return left
    }

    override fun equals(other: Any?): Boolean {
        return other is SignExpr && left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}


class PowExpr(world: World, val base: Expr, val exp: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return base.evalDouble(scope).pow(exp.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return if(exp is LiteralExpr){
            exp * base.pow(exp - 1) * base.derivative(expr)
        }else{
            world.exp(world.log(exp) * base).derivative(expr)
        }
    }

    override fun arg(): Expr {
        return world.tuple(base, exp)
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
            other is PowExpr
                && base === other.base
                && exp === other.exp
    }

    override fun hashCode(): Int {
        val result = 31 * base.hashCode()
        return 31 * result + exp.hashCode()
    }
}

class CosExpr(world: World, val left: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return cos(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return -sin(left) * left.derivative(expr)
    }

    override fun arg(): Expr {
        return left
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is CosExpr &&
                left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}

class Asin(world: World, val left: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return asin(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return 1.0 / (1.0 - left.pow(2)).sqrt() * left.derivative(expr)
    }

    override fun arg(): Expr {
        return left
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is Asin &&
                left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}

class SinExpr(world: World, val left: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return sin(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return cos(left) * left.derivative(expr)
    }

    override fun arg(): Expr {
        return left
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is SinExpr &&
                left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}

class ExpExpr(world: World, val expr: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return exp(expr.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return this
    }

    override fun arg(): Expr {
        return expr
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is ExpExpr &&
                expr === other.expr
    }

    override fun hashCode(): Int {
        return 31 * expr.hashCode()
    }
}

class LogExpr(world: World, val expr: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return log(expr.evalDouble(scope), Math.E)
    }

    override fun derivative(expr: Expr): Expr {
        return this.expr.derivative(expr) / this
    }

    override fun arg(): Expr {
        return expr
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is LogExpr &&
                expr === other.expr
    }

    override fun hashCode(): Int {
        return 31 * expr.hashCode()
    }
}