package com.codecad.core.sketch

import kotlin.math.*

abstract class Expr(val world: World){
    abstract fun eval() : Any
    fun evalDouble() : Double{
        return eval() as Double
    }

    fun evalBoolean() : Boolean{
        return eval() == true
    }

    fun evalLiteral() : Expr{
        return world.literal(eval())
    }

    operator fun unaryMinus() : Expr {
        return world.negate(this)
    }

    operator fun minus(right: Expr) : Expr {
        return world.sub(this, right)
    }

    operator fun minus(right: Double) : Expr {
        return world.sub(this, world.literal(right))
    }

    operator fun minus(right: Int) : Expr {
        return minus(right.toDouble())
    }

    operator fun plus(right: Expr) : Expr {
        return world.add(this, right)
    }

    operator fun plus(right: Double) : Expr {
        return world.add(this, world.literal(right))
    }

    operator fun plus(right: Int) : Expr {
        return plus(right.toDouble())
    }

    operator fun times(right: Expr) : Expr {
        return world.mul(this, right)
    }

    operator fun times(right: Double) : Expr {
        return world.mul(this, world.literal(right))
    }

    operator fun times(right: Int) : Expr {
        return times(right.toDouble())
    }

    operator fun div(right: Expr) : Expr {
        return world.div(this, right)
    }

    operator fun div(right: Double) : Expr {
        return world.div(this, world.literal(right))
    }

    operator fun div(right: Int) : Expr {
        return div(right.toDouble())
    }

    fun pow(right : Expr) : Expr {
        return world.pow(this, right)
    }

    fun pow(right : Double) : Expr {
        return world.pow(this, world.literal(right))
    }

    fun pow(right : Int) : Expr {
        return pow(right.toDouble())
    }

    fun sqrt() : Expr {
        return pow(0.5)
    }

    fun smaller(other: Expr) : Expr {
        return world.lt(this, other)
    }

    companion object{
        fun cos(expr: Expr) : Expr {
            return expr.world.cos(expr)
        }

        fun sin(expr: Expr) : Expr {
            return expr.world.sin(expr)
        }

        fun asin(expr: Expr) : Expr {
            return expr.world.asin(expr)
        }

        fun log(expr: Expr) : Expr {
            return expr.world.log(expr)
        }

        fun ifExpr(condition: Expr, left: Expr, right: Expr) : Expr {
            return condition.world.ifExpr(condition, left, right)
        }

        fun min(left: Expr, right: Expr) : Expr {
            return ifExpr(left.smaller(right), left, right)
        }

        fun max(left: Expr, right: Expr) : Expr {
            return ifExpr(left.smaller(right), right, left)
        }

        fun abs(expr: Expr) : Expr {
            return expr.world.abs(expr)
        }

        fun sign(expr: Expr) : Expr {
            return expr.world.sign(expr)
        }
    }

    abstract fun derivative(param: Param) : Expr
}

operator fun Double.minus(right: Expr) : Expr {
    return right.world.literal(this) - right
}

operator fun Double.times(right: Expr) : Expr {
    return right.world.literal(this) * right
}

operator fun Double.div(right: Expr) : Expr {
    return right.world.literal(this) / right
}

fun Double.pow(right: Expr) : Expr {
    return right.world.literal(this).pow(right)
}

class Literal(world: World, val value: Any) : Expr(world) {
    companion object{
        fun isDouble(expr : Expr) : Boolean{
            return expr is Literal && expr.value is Double
        }

        fun isBoolean(expr : Expr) : Boolean{
            return expr is Literal && true == expr.value
        }
    }

    override fun eval(): Any {
        return value
    }

    override fun derivative(param: Param): Expr {
        return world.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is Literal && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}


class Param(world: World, var value : Double) : Expr(world) {
    override fun eval(): Any {
        return value
    }

    override fun derivative(param: Param): Expr {
        return if(this === param){
            world.ONE
        }else{
            world.ZERO
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

class DeriveExpr(world: World, target: Expr, param: Param) : Expr(world){
    var expr: Expr = target.derivative(param)

    override fun eval() : Any {
        return expr.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return DeriveExpr(world, this, param)
    }

    override fun equals(other: Any?): Boolean {
        return false
    }
}

abstract class BinaryExpr(world: World, val left: Expr, val right: Expr) : Expr(world){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryExpr) return false
        if (left !== other.left) return false
        if (right !== other.right) return false
        return true
    }

    override fun hashCode(): Int {
        return left.hashCode() * 31 + right.hashCode()
    }
}

abstract class UnaryExpr(world: World, val left: Expr) : Expr(world){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnaryExpr) return false
        if (left !== other.left) return false
        return true
    }

    override fun hashCode(): Int {
        return left.hashCode()
    }
}


abstract class CommutativeValue(world: World, left: Expr, right: Expr): BinaryExpr(world, left, right){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommutativeValue) return false
        if (left !== other.left) {
            if(left !== other.right){
                return false
            }else if(right !== other.left){
                return false
            }
        }else if(right !== other.right){
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return left.hashCode() xor right.hashCode()
    }
}

class AddExpr(world: World, left: Expr, right: Expr) : CommutativeValue(world, left, right){
    override fun eval() : Any {
        return left.evalDouble() + right.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return left.derivative(param) + right.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is AddExpr && super.equals(other)
    }
}

class TimesExpr(world: World, left: Expr, right: Expr) : CommutativeValue(world, left, right){
    override fun eval() : Any {
        return left.evalDouble() * right.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return left.derivative(param) * right + left * right.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is TimesExpr && super.equals(other)
    }
}

class LtExpr(world: World, left: Expr, right: Expr) : BinaryExpr(world, left, right){
    override fun eval() : Any {
        return left.evalDouble() < right.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return world.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is LtExpr && super.equals(other)
    }
}

class DivExpr(world: World, left: Expr, right: Expr) : BinaryExpr(world, left, right){
    override fun eval() : Any {
        return left.evalDouble() / right.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return (left.derivative(param) * right + left * right.derivative(param)) / right.pow(2)
    }

    override fun equals(other: Any?): Boolean {
        return other is DivExpr && super.equals(other)
    }
}

class SubExpr(world: World, left: Expr, right: Expr) : BinaryExpr(world, left, right){
    override fun eval() : Any {
        return left.evalDouble() - right.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return left.derivative(param) - right.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is SubExpr && super.equals(other)
    }
}

class NegExpr(world: World, val left: Expr) : Expr(world){
    override fun eval() : Any {
        return -left.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return -left.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is NegExpr && super.equals(other)
    }
}

class IfExpr(world: World, val condition: Expr, left: Expr, right: Expr) : BinaryExpr(world, left, right){
    override fun eval() : Any {
        return if(condition.eval() == true) left.evalDouble() else right.evalDouble()
    }

    override fun derivative(param: Param) : Expr {
        return ifExpr(condition, left.derivative(param), right.derivative(param))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IfExpr) return false
        if (left !== other.left) return false
        if (right !== other.right) return false
        if (condition !== other.condition) return false
        return true
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + condition.hashCode();
    }
}

class Tuple(world: World, val values: Array<Expr>) : Expr(world){
    override fun eval() : Any {
        return Array(values.size){ values[it].eval()}
    }

    override fun derivative(param: Param): Expr {
        return Tuple(world, Array(values.size){ values[it].derivative(param)})
    }
}

class AbsExpr(world: World, val left: Expr) : Expr(world){
    override fun eval() : Any {
        return abs(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        val derivative = left.derivative(param)
        return ifExpr(left.smaller(world.ZERO), -derivative, derivative)
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsExpr && super.equals(other)
    }
}

class SignExpr(world: World, val left: Expr) : Expr(world ){
    override fun eval() : Any {
        return sign(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return world.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is SignExpr && super.equals(other)
    }
}


class PowExpr(world: World, left: Expr, right: Expr) : BinaryExpr(world, left, right){
    override fun eval() : Any {
        return left.evalDouble().pow(right.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return if(right is Literal){
            right * left.pow(right - 1) * left.derivative(param)
        }else{
            (right.derivative(param) * log(left) + right / left * left.derivative(param)) * this
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is PowExpr && super.equals(other)
    }
}

class CosExpr( world: World, val left: Expr ) : Expr(world){
    override fun eval() : Any {
        return cos(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return -sin(left) * left.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is CosExpr && super.equals(other)
    }
}

class Asin(world: World, val left: Expr ) : Expr(world){
    override fun eval() : Any {
        return asin(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return 1.0/ (1.0 - left.pow(2)).sqrt() * left.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is Asin && super.equals(other)
    }
}

class SinExpr(world: World, val left: Expr) : Expr(world){
    override fun eval() : Any {
        return sin(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return cos(left) * left.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is SinExpr && super.equals(other)
    }
}

class LogExpr(world: World, val left: Expr) : Expr(world){
    override fun eval() : Any {
        return log(left.evalDouble(), Math.E)
    }

    override fun derivative(param: Param): Expr {
        return left.derivative(param) / this
    }

    override fun equals(other: Any?): Boolean {
        return other is LogExpr && super.equals(other)
    }
}