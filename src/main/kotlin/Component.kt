import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class LineType(val prio: Int){
    Normal(2), ToolPath(3), ToolContour(1)
}

class Line(val a: Point, val b: Point, type: LineType = LineType.Normal) : Element(type){
    fun squaredLength() : Value{
        return ((b.x - a.x).pow(2) + (b.y - a.y).pow(2))
    }

    fun length() : Value{
        return squaredLength().sqrt()
    }
}

class Circle(val center: Point, val rad: Value, val start: Value? = null, val end: Value? = null) : Element()


abstract class Value {
    abstract var value: Double

    override fun toString(): String {
        return value.toString()
    }

    fun replaceWithConst(value: Value) : Value{
        return if(value.isZero()){
            Const.ZERO
        }else if(value.isOne()){
            Const.ONE
        }else{
            value
        }
    }

    operator fun unaryMinus() : Value{
        if(isZero()){
            return Const.ZERO
        }else if(isConst()){
            return Const(-value)
        }

        return MinusValue(Const.ZERO, this)
    }

    operator fun minus(right: Value) : Value{
        return if(isZero()){
            replaceWithConst(right.unaryMinus())
        }else if(right.isZero()){
            replaceWithConst(this)
        }else if(isOne() && right.isOne()){
            Const.ZERO
        }else if(isConst() && right.isConst()){
            return Const(value - right.value)
        }else{
            return MinusValue(this, right)
        }
    }

    operator fun minus(right: Double) : Value{
        return minus(Const(right))
    }

    operator fun minus(right: Int) : Value{
        return minus(right.toDouble())
    }

    operator fun plus(right: Value) : Value{
        return if(isZero()){
            replaceWithConst(right)
        }else if(right.isZero()){
            replaceWithConst(this)
        }else if(isConst() && right.isConst()){
            return Const(value + right.value)
        }else{
            return AddValue(this, right)
        }
    }

    operator fun times(right: Value) : Value{
        return if(isZero() || right.isZero()){
            Const.ZERO
        }else if(isOne()){
            replaceWithConst(right)
        }else if(right.isOne()){
            replaceWithConst(this)
        }else if(isConst() && right.isConst()){
            return Const(value * right.value)
        }else{
            return TimesValue(this, right)
        }
    }

    operator fun times(right: Double) : Value{
        return times(Const(right))
    }

    operator fun times(right: Int) : Value{
        return times(right.toDouble())
    }

    operator fun div(right: Value) : Value{
        return if(isZero()){
            Const.ZERO
        }else if(right.isOne()){
            replaceWithConst(this)
        }else if(isConst() && right.isConst()){
            return Const(value / right.value)
        }else{
            return DivValue(this, right)
        }
    }

    operator fun div(right: Double) : Value{
        return div(Const(right))
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
            replaceWithConst(this)
        }else if(isConst() && right.isConst()){
            return Const(value.pow(right.value))
        }else{
            return PowValue(this, right)
        }
    }

    fun pow(right : Double) : Value{
        return pow(Const(right))
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

    abstract fun derivative(parameter: Parameter) : Value
    abstract fun isZero() : Boolean
    abstract fun isOne() : Boolean
    abstract fun isConst() : Boolean
}

class Const(_value: Double) : Value() {
    override var value: Double = _value

    companion object{
        val ZERO: Const = Const(0.0)
        val ONE: Const = Const(1.0)
    }

    override fun derivative(parameter: Parameter): Value {
        return ZERO
    }

    override fun isOne(): Boolean {
        return value == 1.0
    }

    override fun isZero(): Boolean {
        return value == 0.0
    }

    override fun isConst(): Boolean {
        return true
    }
}

class Parameter(_value: Double) : Value() {
    override var value: Double = _value

    override fun derivative(parameter: Parameter): Value {
        return Const(if(this === parameter){
            1.0
        }else{
            0.0
        })
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
}

class ProxyValue(var proxy: Value) : Value() {
    override var value: Double
        get() = proxy.value
        set(value) {
            proxy.value = value
        }

    override fun derivative(parameter: Parameter): Value {
        return proxy.derivative(parameter)
    }

    override fun isOne(): Boolean {
        return proxy.isOne()
    }

    override fun isZero(): Boolean {
        return proxy.isZero()
    }

    override fun isConst(): Boolean {
        return proxy.isConst()
    }
}


open class Element(var type: LineType = LineType.Normal)

interface AbstractPoint{
    val x: Value
    val y: Value

    fun scalarProduct(other: AbstractPoint) : Value{
        return x * other.x + y * other.y
    }

    fun vectorProduct(other: AbstractPoint) : Value{
        return x * other.y - y * other.x
    }

    operator fun times(other: AbstractPoint) : Value{
        return x * other.y - y * other.x
    }

    operator fun times(other: Value) : Point{
        return Point(x * other, y * other)
    }

    operator fun plus(right: AbstractPoint) : AbstractPoint {
        return Point(this.x + right.x, this.y + right.y)
    }

    operator fun minus(right: AbstractPoint) : AbstractPoint {
        return Point(this.x - right.x, this.y - right.y)
    }

    operator fun minus(right: Value) : AbstractPoint {
        return Point(this.x - right, this.y - right)
    }

    fun squaredLength(): Value {
        return x.pow(2) + y.pow(2)
    }

    fun length(): Value {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: AbstractPoint): Value {
        return (x - other.x).pow(2) + (y-other.y).pow(2)
    }

    fun length(other: AbstractPoint): Value {
        return squaredLength(other).sqrt()
    }
}


class PowValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value.pow(right.value)
        set(value) {throw RuntimeException()}

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

class CosValue(val left: Value) : Value(){
    override var value: Double
        get() = cos(left.value)
        set(value) {throw RuntimeException()}

    override fun derivative(parameter: Parameter): Value {
        return -SinValue(left) * left.derivative(parameter)
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

class SinValue(val left: Value) : Value(){
    override var value: Double
        get() = sin(left.value)
        set(value) {throw RuntimeException()}

    override fun derivative(parameter: Parameter): Value {
        return CosValue(left) * left.derivative(parameter)
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


class AddValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value + right.value
        set(value) {throw RuntimeException()}

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

class TimesValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value * right.value
        set(value) {throw RuntimeException()}

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


class SmallerValue(private val left: Value, private val right: Value) : Value(){
    override var value: Double
        get() = if(left.value < right.value) 1.0 else 0.0
        set(value) {throw RuntimeException()}

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

class DivValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value / right.value
        set(value) {throw RuntimeException()}

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

class MinusValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value - right.value
        set(value) {throw RuntimeException()}

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


class ConditionalValue(val condition: Value, val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = if(condition.value > 0.5) left.value else right.value
        set(value) {throw RuntimeException()}

    override fun derivative(parameter: Parameter): Value {
        return ConditionalValue(condition, left.derivative(parameter), right.derivative(parameter))
    }

    override fun isZero(): Boolean {
        return left.isZero() && right.isZero()
    }

    override fun isOne(): Boolean {
        return left.isOne() && right.isOne()
    }

    override fun isConst(): Boolean {
        return condition.isOne() && left.isConst() || condition.isZero() && right.isConst()
    }
}

class Point(override val x: Value, override val y: Value, type: LineType = LineType.Normal) : Element(type), AbstractPoint {
    fun toVector(): Vector {
        return Vector(x.value, y.value)
    }
}
