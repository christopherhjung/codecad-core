
enum class LineType(val prio: Int){
    Normal(2), ToolPath(3), ToolContour(1)
}

class Line(val a: Point, val b: Point, type: LineType = LineType.Normal) : Element(type)

class Circle(val center: Point, val rad: Value, val start: Value? = null, val end: Value? = null) : Element()


abstract class Value {
    abstract var value: Double

    override fun toString(): String {
        return value.toString()
    }
}



class Parameter(_value: Double) : Value() {
    override var value: Double = _value
}

class ProxyValue(var proxy: Value) : Value() {
    override var value: Double
        get() = proxy.value
        set(value) {
            proxy.value = value
        }
}


open class Element(var type: LineType = LineType.Normal)

interface AbstractPoint{
    val x: Value
    val y: Value
}

class AddValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value + right.value
        set(value) {throw RuntimeException()}
}

class MinusValue(val left: Value, val right: Value) : Value(){
    override var value: Double
        get() = left.value - right.value
        set(value) {throw RuntimeException()}
}

class AddPoint(left: Point, right: Point) : AbstractPoint{
    override val x: Value = AddValue(left.x, right.x)
    override val y: Value = AddValue(left.y, right.y)
}

class MinusPoint(left: Point, right: Point) : AbstractPoint{
    override val x: Value = MinusValue(left.x, right.x)
    override val y: Value = MinusValue(left.y, right.y)
}

class Point(override val x: Value, override val y: Value, type: LineType = LineType.Normal) : Element(type), AbstractPoint {
    fun toVector(): Vector {
        return Vector(x.value, y.value)
    }

    operator fun plus(right: Point) : AbstractPoint {
        return AddPoint(this, right)
    }

    operator fun minus(right: Point) : AbstractPoint {
        return MinusPoint(this, right)
    }
}
