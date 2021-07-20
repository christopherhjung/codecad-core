import Value.Companion.modCounter
import kotlin.math.*

class CachedValue(val ref: Value) : RawValue(){
    var cachedModCounter = -1
    var cache: Double = 0.0

    override var value: Double
        get() {
            if(cachedModCounter != modCounter){
                cachedModCounter = modCounter
                cache = ref.calc()
            }

            return cache
        }
        set(value) {throw RuntimeException()}

}

abstract class RawValue{
    abstract var value: Double

    open fun isOne(): Boolean {
        return value == 1.0
    }

    open fun isZero(): Boolean {
        return value == 0.0
    }

    open fun isConst(): Boolean {
        return false
    }
}

operator fun Double.minus(right: Value) : Value{
    return if(this == 0.0){
        right.unaryMinus()
    }else if(right.isZero()){
        Const(this)
    }else if(this == 1.0 && right.isOne()){
        Const.ZERO
    }else if( right.isConst()){
        Const(this - right.value)
    }else{
        MinusValue(Const(this), right)
    }
}

operator fun Double.div(right: Value) : Value{
    return if(this == 0.0){
        Const.ZERO
    }else if(this == 1.0 && right.isOne()){
        Const.ONE
    }else if( right.isConst()){
        Const(this - right.value)
    }else{
        DivValue(Const(this), right)
    }
}

abstract class Value : RawValue(){
    abstract val proxyChildren: Set<ProxyValue>

    fun detach() : Const{
        return Const(value)
    }

    override fun toString(): String {
        return value.toString()
    }

    operator fun unaryMinus() : Value{
        return if(isZero()){
            Const.ZERO
        }else if(isConst()){
            Const(-value)
        }else{
            NegativeValue(this)
        }
    }

    operator fun minus(right: Value) : Value{
        return if(isZero()){
            right.unaryMinus()
        }else if(right.isZero()){
            this
        }else if(isOne() && right.isOne()){
            Const.ZERO
        }else if(isConst() && right.isConst()){
            Const(value - right.value)
        }else{
            MinusValue(this, right)
        }
    }

    operator fun minus(right: Double) : Value{
        return if(isConst()){
            Const(value - right)
        }else{
            MinusValue(this, Const(right))
        }
    }

    operator fun minus(right: Int) : Value{
        return minus(right.toDouble())
    }

    operator fun plus(right: Value) : Value{
        return if(isZero()){
            right
        }else if(right.isZero()){
            this
        }else if(isConst() && right.isConst()){
            Const(value + right.value)
        }else{
            AddValue(this, right)
        }
    }

    operator fun plus(right: Double) : Value{
        return if(isConst()){
            Const(value + right)
        }else{
            AddValue(this, Const(right))
        }
    }

    operator fun plus(right: Int) : Value{
        return plus(right.toDouble())
    }

    operator fun times(right: Value) : Value{
        return if(isZero() || right.isZero()){
            Const.ZERO
        }else if(isOne()){
            right
        }else if(right.isOne()){
            this
        }else if(isConst() && right.isConst()){
            Const(value * right.value)
        }else{
            TimesValue(this, right)
        }
    }

    operator fun times(right: Double) : Value{
        return if(right == 0.0) {
            Const.ZERO
        }else if(right == 1.0) {
            this
        }else if(isConst()){
            Const(value * right)
        }else{
            TimesValue(this, Const(right))
        }
    }

    operator fun times(right: Int) : Value{
        return times(right.toDouble())
    }

    operator fun div(right: Value) : Value{
        return if(isZero()){
            Const.ZERO
        }else if(right.isOne()){
            this
        }else if(isConst() && right.isConst()){
            return Const(value / right.value)
        }else{
            return DivValue(this, right)
        }
    }

    operator fun div(right: Double) : Value{
        return if(right == 1.0){
            this
        }else if(isConst()){
            Const(value / right)
        }else{
            DivValue(this, Const(right))
        }
    }

    operator fun div(right: Int) : Value{
        return div(right.toDouble())
    }

    fun pow(right : Value) : Value{
        return if(right.isZero()){
            Const.ONE
        }else if(isZero()){
            Const.ZERO
        }else if(right.isOne()){
            this
        }else if(isConst() && right.isConst()){
            return Const(value.pow(right.value))
        }else{
            return PowValue(this, right)
        }
    }

    fun pow(right : Double) : Value{
        return if(right == 0.0){
            Const.ONE
        }else if(right == 1.0){
            this
        }else if(isConst()){
            Const(value.pow(right))
        }else{
            PowValue(this, Const(right))
        }
    }

    fun pow(right : Int) : Value{
        return pow(right.toDouble())
    }

    fun sqrt() : Value{
        return pow(0.5)
    }

    fun smaller(other: Value) : Value{
        return SmallerValue(this, other)
    }

    companion object{
        fun cos(value: Value) : Value{
            return if(value.isConst()){
                Const(cos(value.value))
            }else{
                CosValue(value)
            }
        }

        fun sin(value: Value) : Value{
            return if(value.isConst()){
                Const(sin(value.value))
            }else{
                SinValue(value)
            }
        }

        fun conditional(condition: Value, left: Value, right: Value) : Value{
            return if(condition.isConst()){
                if(condition.value > 0.5){
                    left
                }else{
                    right
                }
            }else if(left.isConst() && right.isConst() && left == right){
                left
            }else{
                ConditionalValue(condition, left, right)
            }
        }

        fun min(left: Value, right: Value) : Value{
            return conditional(left.smaller(right), left, right)
        }

        fun abs(other: Value) : Value{
            return if(other.isConst()){
                Const(abs(other.value))
            }else{
                AbsValue(other)
            }
        }

        var modCounter = 0
    }

    abstract fun derivative(parameter: Parameter) : Value
    open fun calc() : Double{
        return value
    }
}

class Const(_value: Double) : Value() {
    override var value: Double = _value
    override val proxyChildren: Set<ProxyValue> = emptySet()

    companion object{
        val ZERO: Const = Const(0.0)
        val ONE: Const = Const(1.0)
    }

    override fun derivative(parameter: Parameter): Value {
        return ZERO
    }

    override fun isConst(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if(other is Const){
            return value == other.value
        }else if(other is ProxyValue){
            return other == this
        }
        return super.equals(other)
    }
}


class Parameter(_value: Double) : Value() {
    override val proxyChildren: Set<ProxyValue> = emptySet()
    override var value: Double = _value
        set(value){
            field = value
            Value.modCounter++
        }

    override fun derivative(parameter: Parameter): Value {
        return if(this === parameter){
            Const.ONE
        }else{
            Const.ZERO
        }
    }

    override fun isZero(): Boolean {
        return false
    }

    override fun isOne(): Boolean {
        return false
    }

    override fun isConst(): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        if(other is ProxyValue){
            return other === this
        }
        return this === other
    }
}

class DerivativeValue(val target: Value, val param: Parameter) : Value(){
    var cache: Value = target.derivative(param)

    override var value: Double
        get() = cache.value
        set(value) {
            throw RuntimeException("not possible to set value")
        }

    override val proxyChildren: Set<ProxyValue> = emptySet()

    override fun derivative(parameter: Parameter): Value {
        return DerivativeValue(this, parameter)
    }

    override fun isOne(): Boolean {
        return false
    }

    override fun isZero(): Boolean {
        return false
    }

    override fun isConst(): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        return false
    }
}

class ProxyValue(_ref: Value) : Value() {
    var ref: Value = _ref
        set(value) {
            Value.modCounter++
            field = value
        }

    override val proxyChildren: Set<ProxyValue> = setOf(this)

    override var value: Double
        get() {
            return ref.value
        }
        set(value) {
            ref.value = value
        }

    override fun derivative(parameter: Parameter): Value {
        return ref.derivative(parameter)
    }

    override fun isOne(): Boolean {
        return ref.isOne()
    }

    override fun isZero(): Boolean {
        return ref.isZero()
    }

    override fun isConst(): Boolean {
        return ref.isConst()
    }

    override fun equals(other: Any?): Boolean {
        return ref === this
    }
}


abstract class BinaryValue(left: Value, val right: Value) : UnaryValue(left){
    override val proxyChildren: Set<ProxyValue> = super.proxyChildren + right.proxyChildren
}


class NotCachedValue(val ref: Value) : RawValue(){
    override var value: Double
        get() = ref.calc()
        set(value) {}
}

abstract class UnaryValue(val left: Value, val cached: Boolean = true) : Value(){
    override val proxyChildren: Set<ProxyValue> = left.proxyChildren
    private val proxy = CachedValue(this)//if(cached) CachedValue(this) else NotCachedValue(this)

    override var value: Double
        get() {
            return proxy.value
        }
        set(value) {throw RuntimeException()}

    override fun isConst(): Boolean {
        return left.isConst()
    }
}

class PowValue(left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double =  left.value.pow(right.value)

    override fun derivative(parameter: Parameter): Value {
        return right * left.pow(right - 1) * left.derivative(parameter)
    }

    override fun isZero(): Boolean {
        return left.isZero()
    }

    override fun isOne(): Boolean {
        return right.isZero() || left.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst() || right.isZero() || left.isOne()
    }
}

class CosValue( left: Value) : UnaryValue(left){
    override fun calc(): Double = cos(left.value)

    override fun derivative(parameter: Parameter): Value {
        return -sin(left) * left.derivative(parameter)
    }

    override fun isOne(): Boolean {
        return left.isZero()
    }

    override fun isZero(): Boolean {
        return false
    }

    override fun isConst(): Boolean {
        return left.isConst()
    }
}

class ArcSinValue( left: Value) : UnaryValue(left){
    override fun calc(): Double = asin(left.value)

    override fun derivative(parameter: Parameter): Value {
        return 1.0/ (1.0 - left.pow(2)).sqrt() * left.derivative(parameter)
    }

    override fun isOne(): Boolean {
        return false
    }

    override fun isZero(): Boolean {
        return false
    }
}

class SinValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = sin(left.value)

    override fun derivative(parameter: Parameter): Value {
        return cos(left) * left.derivative(parameter)
    }

    override fun isOne(): Boolean {
        return false
    }

    override fun isZero(): Boolean {
        return left.isZero()
    }

    override fun isConst(): Boolean {
        return left.isConst()
    }
}


class AddValue( left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = left.value + right.value

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) + right.derivative(parameter)
    }

    override fun isZero(): Boolean {
        return left.isZero() && right.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne() && right.isZero() || left.isZero() && right.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst()
    }
}

class TimesValue(left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = left.value * right.value

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) * right + left * right.derivative(parameter)
    }

    override fun isZero(): Boolean {
        return left.isZero() || right.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne() && right.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst() || left.isZero() || right.isZero()
    }
}


class SmallerValue(left: Value,  right: Value) : BinaryValue(left, right){
    override fun calc(): Double = if(left.value < right.value) 1.0 else 0.0

    override fun derivative(parameter: Parameter): Value {
        return Const.ZERO
    }

    override fun isZero(): Boolean {
        return right.isZero() && left.isOne()
    }

    override fun isOne(): Boolean {
        return left.isZero() && right.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst()
    }
}

class DivValue(left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = left.value / right.value

    override fun derivative(parameter: Parameter): Value {
        return (left.derivative(parameter) * right + left * right.derivative(parameter)) / right.pow(2)
    }

    override fun isZero(): Boolean {
        return left.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne() && right.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst() || left.isZero()
    }
}

class MinusValue(left: Value,  right: Value) : BinaryValue(left, right){
    override fun calc(): Double = left.value - right.value

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) - right.derivative(parameter)
    }

    override fun isZero(): Boolean {
        return left.isZero() && right.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne() && right.isZero()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst()
    }
}

class NegativeValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = -left.value

    override fun derivative(parameter: Parameter): Value {
        return -left.derivative(parameter)
    }

    override fun isZero(): Boolean {
        return left.isZero()
    }

    override fun isOne(): Boolean {
        return false
    }

    override fun isConst(): Boolean {
        return left.isConst()
    }
}

class ConditionalValue(val condition: Value, left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = if(condition.value > 0.5) left.value else right.value

    override fun derivative(parameter: Parameter): Value {
        return conditional(condition, left.derivative(parameter), right.derivative(parameter))
    }

    override fun isZero(): Boolean {
        return left.isZero() && right.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne() && right.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst() && right.isConst() || condition.isOne() && left.isConst() || condition.isZero() && right.isConst()
    }
}

class AbsValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = abs(left.value)

    override fun derivative(parameter: Parameter): Value {
        val derivative = left.derivative(parameter)
        return conditional(left.smaller(Const.ZERO), -derivative, derivative)
    }

    override fun isZero(): Boolean {
        return left.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne()
    }

    override fun isConst(): Boolean {
        return left.isConst()
    }
}

class Vector(vararg val values: Value){
    fun squaredLength(){

    }
}
