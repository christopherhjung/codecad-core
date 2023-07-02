package com.codecad.core.parser.ast.primitive

import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World
import kotlin.math.*

class AbsExpr(world: World, val left: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return abs(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        val derivative = left.derivative(expr)
        return ifExpr(left.smaller(world.ZERO), -derivative, derivative)
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsExpr && left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}

class SignExpr(world: World, val left: Expr) : Expr(world ){
    override fun eval(scope: Scope) : Any {
        return sign(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return world.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is SignExpr && left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}


class PowExpr(world: World, val base: Expr, val exp: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return base.evalDouble(scope).pow(exp.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return if(this.exp is LiteralExpr){
            this.exp * base.pow(this.exp - 1) * base.derivative(expr)
        }else{
            (this.exp.derivative(expr) * log(base) + this.exp / base * base.derivative(expr)) * this
        }
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

class CosExpr( world: World, val left: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return cos(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return -sin(left) * left.derivative(expr)
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

class Asin(world: World, val left: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return asin(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return 1.0/ (1.0 - left.pow(2)).sqrt() * left.derivative(expr)
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

class SinExpr(world: World, val left: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return sin(left.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return cos(left) * left.derivative(expr)
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

class LogExpr(world: World, val left: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return log(left.evalDouble(scope), Math.E)
    }

    override fun derivative(expr: Expr): Expr {
        return left.derivative(expr) / this
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is LogExpr &&
                left === other.left
    }

    override fun hashCode(): Int {
        return 31 * left.hashCode()
    }
}