package com.codecad.core.ast.primitive

import com.codecad.core.scope.Scope
import com.codecad.core.World
import kotlin.math.*

abstract class MathExpr(world: World) : Expr(world){
    abstract fun arg() : Expr
}

class AbsExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return abs(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        val derivative = arg.derivative(expr)
        return ifExpr(arg.smaller(world.ZERO), -derivative, derivative)
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsExpr && arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class SignExpr(world: World, val arg: Expr) : MathExpr(world ){
    override fun eval(scope: Scope) : Any {
        return sign(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return world.ZERO
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other is SignExpr && arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
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

class CosExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return cos(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return -sin(arg) * arg.derivative(expr)
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is CosExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class AsinExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return asin(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return 1.0 / (1.0 - arg.pow(2)).sqrt() * arg.derivative(expr)
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is AsinExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class SinExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return sin(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return cos(arg) * arg.derivative(expr)
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is SinExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class ExpExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return exp(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return this
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is ExpExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class LogExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return log(arg.evalDouble(scope), Math.E)
    }

    override fun derivative(expr: Expr): Expr {
        return this.arg.derivative(expr) / this
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is LogExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}