package com.codecad.core.parser.ast

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


class PowExpr(world: World, val left: Expr, val right: Expr) : Expr(world){
    override fun eval(scope: Scope) : Any {
        return left.evalDouble(scope).pow(right.evalDouble(scope))
    }

    override fun derivative(expr: Expr): Expr {
        return if(right is LiteralExpr){
            right * left.pow(right - 1) * left.derivative(expr)
        }else{
            (right.derivative(expr) * log(left) + right / left * left.derivative(expr)) * this
        }
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
            other is PowExpr
                && left === other.left
                && right === other.right
    }

    override fun hashCode(): Int {
        val result = 31 * left.hashCode()
        return 31 * result + right.hashCode()
    }
}

class CosExpr( world: World, val left: Expr ) : Expr(world){
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

class Asin(world: World, val left: Expr ) : Expr(world){
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