package com.codecad.core.ast.primitive

import com.codecad.core.World
import com.codecad.core.scope.Scope
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
        return ifExpr(arg.lt(world.Zero), -derivative, derivative)
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
        return world.Zero
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
            world.exp(world.log(base) * exp).derivative(expr)
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
        var result = base.hashCode()
        result = 31 * result + exp.hashCode()
        return result
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

class ASinExpr(world: World, val arg: Expr) : MathExpr(world){
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
                other is ASinExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class ACosExpr(world: World, val arg: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return acos(arg.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return -1.0 / (1.0 - arg.pow(2)).sqrt() * arg.derivative(expr)
    }

    override fun arg(): Expr {
        return arg
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is ACosExpr &&
                arg === other.arg
    }

    override fun hashCode(): Int {
        return 31 * arg.hashCode()
    }
}

class ATan2Expr(world: World, val lhs: Expr, val rhs: Expr) : MathExpr(world){
    override fun eval(scope: Scope) : Any {
        return atan2(lhs.evalDouble(scope), rhs.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        val denom = 1.0 / (lhs.pow(2) + rhs.pow(2))
        return world.tuple(rhs * denom, - lhs * denom)
    }

    override fun arg(): Expr {
        return world.tuple(lhs, rhs)
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                other is ATan2Expr &&
                lhs === other.lhs &&
                rhs === other.rhs
    }

    override fun hashCode(): Int {
        return (31 * lhs.hashCode() + 11) * rhs.hashCode()
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