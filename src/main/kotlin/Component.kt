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
/*
    open fun isOne(): Boolean {
        return value == 1.0
    }

    open fun isZero(): Boolean {
        return value == 0.0
    }*/

    /*
    open fun isConst(): Boolean {
        return false
    }*/
}

operator fun Double.minus(right: Value) : Value{
    return Value.const(this) - right
}

operator fun Double.times(right: Value) : Value{
    return Value.const(this) * right
}

operator fun Double.div(right: Value) : Value{
    return Value.const(this) / right
}

fun Double.pow(right: Value) : Value{
    return Value.const(this).pow(right)
}

abstract class Value : RawValue(){
    abstract val proxyChildren: Set<ProxyValue>

    fun detach() : Const{
        return const(value)
    }

    override fun toString(): String {
        return value.toString()
    }

    operator fun unaryMinus() : Value{
        return if(hasConstValue(this, 0.0)){
            Const.ZERO
        }else if(this is Const){
            const(-value)
        }else{
            cached{
                NegativeValue(this)
            }
        }
    }

    operator fun minus(right: Value) : Value{
        return if(hasConstValue(this, 0.0)){
            right.unaryMinus()
        }else if(hasConstValue(right, 0.0)){
            this
        }else if(hasConstValue(this, 1.0) && hasConstValue(right, 1.0)){
            Const.ZERO
        }else if(this is Const && right is Const){
            const(value - right.value)
        }else if(this === right){
            Const.ZERO
        }else{
            cached{
                MinusValue(this, right)
            }
        }
    }

    operator fun minus(right: Double) : Value{
        return if(this is Const){
            const(value - right)
        }else{
            cached{
                MinusValue(this, const(right))
            }
        }
    }

    operator fun minus(right: Int) : Value{
        return minus(right.toDouble())
    }

    operator fun plus(right: Value) : Value{
        return if(hasConstValue(this, 0.0)){
            right
        }else if(hasConstValue(right, 0.0)){
            this
        }else if(this is Const && right is Const){
            const(value + right.value)
        }else if(this === right){
            cached{
                TimesValue(const(2.0), right)
            }
        }else{
            cached{
                AddValue(this, right)
            }
        }
    }

    operator fun plus(right: Double) : Value{
        return if(this is Const){
            const(value + right)
        }else{
            cached{
                AddValue(this, const(right))
            }
        }
    }

    operator fun plus(right: Int) : Value{
        return plus(right.toDouble())
    }

    fun hasConstValue(value: Value, expect: Double) : Boolean{
        return value is Const && value.value == expect
    }

    operator fun times(right: Value) : Value{
        return if(hasConstValue(this, 0.0) || hasConstValue(right, 0.0)){
            Const.ZERO
        }else if(hasConstValue(this, 1.0)){
            right
        }else if(hasConstValue(right, 1.0)){
            this
        }else if(this is Const && right is Const){
            const(value * right.value)
        }else if(this === right){
            cached{
                PowValue(this, const(2.0))
            }
        }else{
            cached{
                TimesValue(this, right)
            }
        }
    }

    operator fun times(right: Double) : Value{
        return if(right == 0.0) {
            Const.ZERO
        }else if(right == 1.0) {
            this
        }else if(this is Const){
            const(value * right)
        }else{
            cached{
                TimesValue(this, const(right))
            }
        }
    }

    operator fun times(right: Int) : Value{
        return times(right.toDouble())
    }

    operator fun div(right: Value) : Value{
        return if(this is Const && value == 0.0){
            Const.ZERO
        }else if(right is Const && right.value == 1.0){
            this
        }else if(this is Const && right is Const){
            const(value / right.value)
        }else if(this === right){
            Const.ONE
        }else{
            cached{
                DivValue(this, right)
            }
        }
    }

    operator fun div(right: Double) : Value{
        return if(right == 1.0){
            this
        }else if(this is Const){
            const(value / right)
        }else{
            cached{
                DivValue(this, Const(right))
            }
        }
    }

    operator fun div(right: Int) : Value{
        return div(right.toDouble())
    }

    fun pow(right : Value) : Value{
        return if(hasConstValue(right, 0.0)){
            Const.ONE
        }else if(hasConstValue(right, 1.0)){
            this
        }else if(hasConstValue(this, 0.0)){
            Const.ZERO
        }else if(this is Const && right is Const){
            const(value.pow(right.value))
        }else{
            cached{PowValue(this, right)}
        }

    }

    fun pow(right : Double) : Value{
        return if(right == 0.0){
            Const.ONE
        }else if(right == 1.0){
            this
        }else if(this is Const){
            const(value.pow(right))
        }else{
            cached{
                PowValue(this, const(right))
            }
        }
    }

    fun pow(right : Int) : Value{
        return pow(right.toDouble())
    }

    fun sqrt() : Value{
        return pow(0.5)
    }

    fun smaller(other: Value) : Value{
        val newVal =  SmallerValue(this, other)
        if(this is Const && other is Const){
            return const(newVal.value)
        }

        return cached{
            newVal
        }
    }

    companion object{
        private val repeatCache = HashMap<Value, Value>()

        fun const(value: Double) : Const{
            return cached {
                Const(value)
            }
        }

        fun <T> cached(block: () -> T) : T where T : Value{
            val newVal = block()
            return repeatCache.computeIfAbsent(newVal) {
                newVal
            } as T
        }

        fun cos(value: Value) : Value{
            return if(value is Const){
                const(cos(value.value))
            }else{
                cached {
                    CosValue(value)
                }
            }
        }

        fun sin(value: Value) : Value{
            return if(value is Const){
                const(sin(value.value))
            }else{
                cached {
                    SinValue(value)
                }
            }
        }

        fun log(value: Value) : Value{
            return if(value is Const){
                const(log(value.value, Math.E))
            }else{
                cached {
                    LogValue(value)
                }
            }
        }

        fun conditional(condition: Value, left: Value, right: Value) : Value{
            return if(condition is Const){
                if(condition.value > 0.5){
                    left
                }else{
                    right
                }
            }else if(left is Const && right is Const && left == right){
                left
            }else{
                cached {
                    ConditionalValue(condition, left, right)
                }
            }
        }

        fun min(left: Value, right: Value) : Value{
            return conditional(left.smaller(right), left, right)
        }

        fun max(left: Value, right: Value) : Value{
            return conditional(left.smaller(right), right, left)
        }

        fun abs(other: Value) : Value{
            return if(other is Const){
                const(abs(other.value))
            }else{
                cached {
                    AbsValue(other)
                }
            }
        }

        fun sign(other: Value) : Value{
            return if(other is Const){
                const(sign(other.value))
            }else{
                cached {
                    SignValue(other)
                }
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
        val ZERO: Const = const(0.0)
        val ONE: Const = const(1.0)
    }

    override fun derivative(parameter: Parameter): Value {
        return ZERO
    }
    override fun equals(other: Any?): Boolean {
        if(other is Const){
            return value == other.value
        }else if(other is ProxyValue){
            return other == this
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return value.toRawBits().toInt()
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

    override fun equals(other: Any?): Boolean {
        return ref === this
    }
}


abstract class BinaryValue(left: Value, val right: Value) : UnaryValue(left){
    override val proxyChildren: Set<ProxyValue> = super.proxyChildren + right.proxyChildren

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryValue) return false
        if (left !== other.left) return false
        if (right !== other.right) return false
        return true
    }

    override fun hashCode(): Int {
        return left.hashCode() * 31 + right.hashCode()
    }
}


class NotCachedValue(val ref: Value) : RawValue(){
    override var value: Double
        get() = ref.calc()
        set(value) {}
}

abstract class UnaryValue(val left: Value, val cached: Boolean = true) : Value(){
    override val proxyChildren: Set<ProxyValue> = left.proxyChildren

    private var cachedModCounter = -1
    private var cache: Double = 0.0

    override var value: Double
        get() {
            if(cachedModCounter != modCounter){
                cachedModCounter = modCounter
                cache = calc()
            }

            return cache
        }
        set(value) {throw RuntimeException()}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnaryValue) return false
        if (left !== other.left) return false
        return true
    }

    override fun hashCode(): Int {
        return left.hashCode()
    }
}

class PowValue(left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double =  left.value.pow(right.value)

    override fun derivative(parameter: Parameter): Value {
        return if(right is Const){
            right * left.pow(right - 1) * left.derivative(parameter)
        }else{
            (right.derivative(parameter) * log(left) + right / left * left.derivative(parameter)) * this
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is PowValue && super.equals(other)
    }
}

class CosValue( left: Value) : UnaryValue(left){
    override fun calc(): Double = cos(left.value)

    override fun derivative(parameter: Parameter): Value {
        return -sin(left) * left.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is CosValue && super.equals(other)
    }
}

class ArcSinValue( left: Value) : UnaryValue(left){
    override fun calc(): Double = asin(left.value)

    override fun derivative(parameter: Parameter): Value {
        return 1.0/ (1.0 - left.pow(2)).sqrt() * left.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is ArcSinValue && super.equals(other)
    }
}

class SinValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = sin(left.value)

    override fun derivative(parameter: Parameter): Value {
        return cos(left) * left.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is SinValue && super.equals(other)
    }
}

class LogValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = sin(left.value)

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) / this
    }

    override fun equals(other: Any?): Boolean {
        return other is LogValue && super.equals(other)
    }
}

abstract class CommutativeValue( left: Value, right: Value): BinaryValue(left, right){
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

class AddValue( left: Value, right: Value) : CommutativeValue(left, right){
    override fun calc(): Double = left.value + right.value

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) + right.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is AddValue && super.equals(other)
    }
}

class TimesValue(left: Value, right: Value) : CommutativeValue(left, right){
    override fun calc(): Double = left.value * right.value

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) * right + left * right.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is TimesValue && super.equals(other)
    }
}

class SmallerValue(left: Value,  right: Value) : BinaryValue(left, right){
    override fun calc(): Double = if(left.value < right.value) 1.0 else 0.0

    override fun derivative(parameter: Parameter): Value {
        return Const.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is SmallerValue && super.equals(other)
    }
}

class DivValue(left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = left.value / right.value

    override fun derivative(parameter: Parameter): Value {
        return (left.derivative(parameter) * right + left * right.derivative(parameter)) / right.pow(2)
    }

    override fun equals(other: Any?): Boolean {
        return other is DivValue && super.equals(other)
    }
}

class MinusValue(left: Value,  right: Value) : BinaryValue(left, right){
    override fun calc(): Double = left.value - right.value

    override fun derivative(parameter: Parameter): Value {
        return left.derivative(parameter) - right.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is MinusValue && super.equals(other)
    }
}

class NegativeValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = -left.value

    override fun derivative(parameter: Parameter): Value {
        return -left.derivative(parameter)
    }

    override fun equals(other: Any?): Boolean {
        return other is NegativeValue && super.equals(other)
    }
}

class ConditionalValue(val condition: Value, left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = if(condition.value > 0.5) left.value else right.value

    override fun derivative(parameter: Parameter): Value {
        return conditional(condition, left.derivative(parameter), right.derivative(parameter))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConditionalValue) return false
        if (left !== other.left) return false
        if (right !== other.right) return false
        if (condition !== other.condition) return false
        return true
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + condition.hashCode();
    }
}

class AbsValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = abs(left.value)

    override fun derivative(parameter: Parameter): Value {
        val derivative = left.derivative(parameter)
        return conditional(left.smaller(Const.ZERO), -derivative, derivative)
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsValue && super.equals(other)
    }
}

class SignValue(left: Value) : UnaryValue(left){
    override fun calc(): Double = sign(left.value)

    override fun derivative(parameter: Parameter): Value {
        return Const.ZERO
    }

    override fun equals(other: Any?): Boolean {
        return other is SignValue && super.equals(other)
    }
}

class Vector(vararg val values: Value){
    fun squaredLength(){

    }
}
