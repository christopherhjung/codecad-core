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

class Observable{

}

class CachedValue(val ref: Value) : Value(){
    override val references: Set<Value> = emptySet()
    var listRef: Array<Value>? = null
    var listMod: IntArray? = null
    var cache: Double = 0.0

    override var value: Double
        get() {
            if(listRef != null){
                var found = false

                for(i in listRef!!.indices){
                    if(listMod!![i] != listRef!![i].modCounter){
                        if(!found){
                            cache = ref.calc()
                            found = true
                        }

                        listMod!![i] = listRef!![i].modCounter
                    }
                }
            }else{
                listRef = ref.references.toTypedArray()
                cache = ref.calc()
                listMod = IntArray(ref.references.size){listRef!![it].modCounter}
            }

            return cache
        }
        set(value) {throw RuntimeException()}

    override var modCounter: Int
        get() = super.modCounter
        set(value) {}

    override fun derivative(parameter: Parameter): Value {
        TODO("Not yet implemented")
    }

    override fun isZero(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isOne(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isConst(): Boolean {
        TODO("Not yet implemented")
    }
}

abstract class Value {
    abstract var value: Double
    abstract val references: Set<Value>
    open var modCounter: Int = 0

    override fun toString(): String {
        return value.toString()
    }

    private fun replaceWithConst(value: Value) : Value{
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
    }

    abstract fun derivative(parameter: Parameter) : Value
    abstract fun isZero() : Boolean
    abstract fun isOne() : Boolean
    abstract fun isConst() : Boolean
    open fun calc() : Double{
        return value
    }

}

class Const(_value: Double) : Value() {
    override var value: Double = _value
    override val references: Set<Value> = emptySet()

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
    override var value: Double = _value
        set(value){
            field = value
            modCounter++
        }

    override val references: Set<Value> = setOf(this)

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

    override var modCounter: Int = 0
        get() = target.modCounter

    override val references: Set<Value> = setOf(this)

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
            modCounter = modCounter + 1 - ref.modCounter
            field = value
        }

    override var modCounter: Int = 0
        get() = field + ref.modCounter

    override val references: Set<Value> = setOf(this)

    var listRef: Array<Value>? = null
    var listMod: IntArray? = null
    var cache: Double = 0.0

    override var value: Double
        get() {
            if(listRef != null){
                var found = false

                for(i in listRef!!.indices){
                    if(listMod!![i] != listRef!![i].modCounter){
                        if(!found){
                            cache = ref.calc()
                            found = true
                        }

                        listMod!![i] = listRef!![i].modCounter
                    }
                }
            }else{
                listRef = ref.references.toTypedArray()
                cache = ref.calc()
                listMod = IntArray(ref.references.size){listRef!![it].modCounter}
            }

            return cache
        }
        set(value) {throw RuntimeException()}

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

abstract class BinaryValue(left: Value, val right: Value) : UnaryValue(left){
    override val references: Set<Value> = super.references + right.references
}

class Context(val param: Value, var counter: Int)

abstract class UnaryValue(val left: Value, val cached: Boolean = true) : Value(){
    override val references: Set<Value> = left.references
    private val observer = if(cached) ProxyValue(this) else this

    override var value: Double
        get() = observer.value
        set(value) {throw RuntimeException()}
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


class ConditionalValue(val condition: Value, left: Value, right: Value) : BinaryValue(left, right){
    override fun calc(): Double = if(condition.value > 0.5) left.value else right.value

    override val references: Set<Value> = condition.references + super.references

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

class Point(override val x: Value, override val y: Value, type: LineType = LineType.Normal) : Element(type), AbstractPoint {
    fun toVector(): Vector {
        return Vector(x.value, y.value)
    }

    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }
}
