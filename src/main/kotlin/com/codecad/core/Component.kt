package com.codecad.core

import kotlin.math.*

abstract class Expr{
    abstract fun eval() : Any
    fun evalDouble() : Double{
        return eval() as Double
    }
    fun evalBoolean() : Boolean{
        return eval() == true
    }

    operator fun unaryMinus() : Expr {
        return if(hasConstValue(this, 0.0)){
            Literal.ZERO
        }else if(this is Literal && value is Double){
            Expr.const(-value)
        }else{
            Expr.cached {
                NegExpr(this)
            }
        }
    }

    operator fun minus(right: Expr) : Expr {
        return if(hasConstValue(this, 0.0)){
            right.unaryMinus()
        }else if(hasConstValue(right, 0.0)){
            this
        }else if(hasConstValue(this, 1.0) && hasConstValue(right, 1.0)){
            Literal.ZERO
        }else if(Literal.isDouble(this) && Literal.isDouble(right)){
            Expr.const(evalDouble() - right.evalDouble())
        }else if(this === right){
            Literal.ZERO
        }else{
            Expr.cached {
                SubExpr(this, right)
            }
        }
    }

    operator fun minus(right: Double) : Expr {
        return if(Literal.isDouble(this)){
            Expr.const(evalDouble() - right)
        }else{
            Expr.cached {
                SubExpr(this, Expr.const(right))
            }
        }
    }

    operator fun minus(right: Int) : Expr {
        return minus(right.toDouble())
    }

    operator fun plus(right: Expr) : Expr {
        return if(hasConstValue(this, 0.0)){
            right
        }else if(hasConstValue(right, 0.0)){
            this
        }else if(Literal.isDouble(this) && Literal.isDouble(right)){
            Expr.const(evalDouble() + right.evalDouble())
        }else if(this === right){
            Expr.cached {
                TimesExpr(Expr.const(2.0), right)
            }
        }else{
            Expr.cached {
                AddExpr(this, right)
            }
        }
    }

    operator fun plus(right: Double) : Expr {
        return if(Literal.isDouble(this)){
            Expr.const(evalDouble() + right)
        }else{
            Expr.cached {
                AddExpr(this, Expr.const(right))
            }
        }
    }

    operator fun plus(right: Int) : Expr {
        return plus(right.toDouble())
    }

    fun hasConstValue(value: Expr, expect: Double) : Boolean{
        return value is Literal && value.value == expect
    }

    operator fun times(right: Expr) : Expr {
        return if(hasConstValue(this, 0.0) || hasConstValue(right, 0.0)){
            Literal.ZERO
        }else if(hasConstValue(this, 1.0)){
            right
        }else if(hasConstValue(right, 1.0)){
            this
        }else if(Literal.isDouble(this) && Literal.isDouble(right)){
            Expr.const(evalDouble() * right.evalDouble())
        }else if(this === right){
            Expr.cached {
                PowExpr(this, Expr.const(2.0))
            }
        }else{
            Expr.cached {
                TimesExpr(this, right)
            }
        }
    }

    operator fun times(right: Double) : Expr {
        return if(right == 0.0) {
            Literal.ZERO
        }else if(right == 1.0) {
            this
        }else if(Literal.isDouble(this)){
            Expr.const(evalDouble() * right)
        }else{
            Expr.cached {
                TimesExpr(this, Expr.const(right))
            }
        }
    }

    operator fun times(right: Int) : Expr {
        return times(right.toDouble())
    }

    operator fun div(right: Expr) : Expr {
        return if(this is Literal && value == 0.0){
            Literal.ZERO
        }else if(right is Literal && right.value == 1.0){
            this
        }else if(Literal.isDouble(this) && Literal.isDouble(right)){
            Expr.const(evalDouble() / right.evalDouble())
        }else if(this === right){
            Literal.ONE
        }else{
            Expr.cached {
                DivExpr(this, right)
            }
        }
    }

    operator fun div(right: Double) : Expr {
        return if(right == 1.0){
            this
        }else if(Literal.isDouble(this)){
            Expr.const(evalDouble() / right)
        }else{
            Expr.cached {
                DivExpr(this, Literal(right))
            }
        }
    }

    operator fun div(right: Int) : Expr {
        return div(right.toDouble())
    }

    fun pow(right : Expr) : Expr {
        return if(hasConstValue(right, 0.0)){
            Literal.ONE
        }else if(hasConstValue(right, 1.0)){
            this
        }else if(hasConstValue(this, 0.0)){
            Literal.ZERO
        }else if(Literal.isDouble(this) && Literal.isDouble(right)){
            Expr.const(evalDouble().pow(right.evalDouble()))
        }else{
            Expr.cached { PowExpr(this, right) }
        }

    }

    fun pow(right : Double) : Expr {
        return if(right == 0.0){
            Literal.ONE
        }else if(right == 1.0){
            this
        }else if(Literal.isDouble(this)){
            Expr.const(evalDouble().pow(right))
        }else{
            Expr.cached {
                PowExpr(this, Expr.const(right))
            }
        }
    }

    fun pow(right : Int) : Expr {
        return pow(right.toDouble())
    }

    fun sqrt() : Expr {
        return pow(0.5)
    }

    fun smaller(other: Expr) : Expr {
        val newVal =  LtExpr(this, other)
        if(this is Literal && other is Literal){
            return Expr.const(newVal.evalDouble())
        }

        return Expr.cached {
            newVal
        }
    }

    companion object{
        private val repeatCache = HashMap<Expr, Expr>()

        fun const(value: Double) : Literal {
            return cached {
                Literal(value)
            }
        }

        fun <T> cached(block: () -> T) : T where T : Expr {
            val newVal = block()
            return repeatCache.computeIfAbsent(newVal) {
                newVal
            } as T
        }

        fun cos(value: Expr) : Expr {
            return if(Literal.isDouble(value)){
                const(cos(value.evalDouble()))
            }else{
                cached {
                    CosExpr(value)
                }
            }
        }

        fun sin(value: Expr) : Expr {
            return if(Literal.isDouble(value)){
                const(sin(value.evalDouble()))
            }else{
                cached {
                    SinExpr(value)
                }
            }
        }

        fun log(value: Expr) : Expr {
            return if(Literal.isDouble(value)){
                const(log(value.evalDouble(), Math.E))
            }else{
                cached {
                    LogExpr(value)
                }
            }
        }

        fun conditional(condition: Expr, left: Expr, right: Expr) : Expr {
            return if(Literal.isBoolean(condition)){
                if(condition.evalBoolean()){
                    left
                }else{
                    right
                }
            }else if(left is Literal && right is Literal && left == right){
                left
            }else{
                cached {
                    IfExpr(condition, left, right)
                }
            }
        }

        fun min(left: Expr, right: Expr) : Expr {
            return conditional(left.smaller(right), left, right)
        }

        fun max(left: Expr, right: Expr) : Expr {
            return conditional(left.smaller(right), right, left)
        }

        fun abs(other: Expr) : Expr {
            return if(Literal.isDouble(other)){
                const(abs(other.evalDouble()))
            }else{
                cached {
                    AbsExpr(other)
                }
            }
        }

        fun sign(other: Expr) : Expr {
            return if(Literal.isDouble(other)){
                const(sign(other.evalDouble()))
            }else{
                cached {
                    SignExpr(other)
                }
            }
        }
    }

    abstract fun derivative(param: Param) : Expr
}

operator fun Double.minus(right: Expr) : Expr {
    return Expr.const(this) - right
}

operator fun Double.times(right: Expr) : Expr {
    return Expr.const(this) * right
}

operator fun Double.div(right: Expr) : Expr {
    return Expr.const(this) / right
}

fun Double.pow(right: Expr) : Expr {
    return Expr.const(this).pow(right)
}



class Literal(val value: Any) : Expr() {
    companion object{
        val ZERO = Literal(0.0)
        val ONE = Literal(1.0)

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
        return ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is Literal && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}


class Param(var value : Double) : Expr() {
    override fun eval(): Any {
        return value
    }

    override fun derivative(param: Param): Expr {
        return if(this === param){
            Literal.ONE
        }else{
            Literal.ZERO
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

class DeriveExpr(target: Expr, param: Param) : Expr(){
    var expr: Expr = target.derivative(param)

    override fun eval() : Any {
        return expr.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return DeriveExpr(this, param)
    }

    override fun equals(other: Any?): Boolean {
        return false
    }
}

abstract class BinaryExpr(val left: Expr, val right: Expr) : Expr(){
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

abstract class UnaryExpr(val left: Expr) : Expr(){
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

class PowExpr(left: Expr, right: Expr) : BinaryExpr(left, right){
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

class CosExpr(left: Expr ) : UnaryExpr(left){
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

class ArcSinExpr(left: Expr ) : UnaryExpr(left){
    override fun eval() : Any {
        return asin(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return 1.0/ (1.0 - left.pow(2)).sqrt() * left.derivative(param)
    }

    override fun equals(other: Any?): Boolean {
        return other is ArcSinExpr && super.equals(other)
    }
}

class SinExpr(left: Expr) : UnaryExpr(left){
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

class LogExpr(left: Expr) : UnaryExpr(left){
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

abstract class CommutativeValue(left: Expr, right: Expr): BinaryExpr(left, right){
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

class AddExpr(left: Expr, right: Expr) : CommutativeValue(left, right){
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

class TimesExpr(left: Expr, right: Expr) : CommutativeValue(left, right){
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

class LtExpr(left: Expr, right: Expr) : BinaryExpr(left, right){
    override fun eval() : Any {
        return left.evalDouble() < right.evalDouble()
    }

    override fun derivative(param: Param): Expr {
        return Literal.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is LtExpr && super.equals(other)
    }
}

class DivExpr(left: Expr, right: Expr) : BinaryExpr(left, right){
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

class SubExpr(left: Expr, right: Expr) : BinaryExpr(left, right){
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

class NegExpr(left: Expr) : UnaryExpr(left){
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

class IfExpr(val condition: Expr, left: Expr, right: Expr) : BinaryExpr(left, right){
    override fun eval() : Any {
        return if(condition.eval() == true) left.evalDouble() else right.evalDouble()
    }

    override fun derivative(param: Param) : Expr {
        return conditional(condition, left.derivative(param), right.derivative(param))
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

class AbsExpr(left: Expr) : UnaryExpr(left){
    override fun eval() : Any {
        return abs(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        val derivative = left.derivative(param)
        return conditional(left.smaller(Literal.ZERO), -derivative, derivative)
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsExpr && super.equals(other)
    }
}

class SignExpr(left: Expr) : UnaryExpr(left){
    override fun eval() : Any {
        return sign(left.evalDouble())
    }

    override fun derivative(param: Param): Expr {
        return Literal.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is SignExpr && super.equals(other)
    }
}

class Tuple(val values: Array<Expr>) : Expr(){
    override fun eval() : Any {
        return Array(values.size){ values[it].eval()}
    }

    override fun derivative(param: Param): Expr {
        return Tuple(Array(values.size){ values[it].derivative(param)})
    }
}
