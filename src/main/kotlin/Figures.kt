enum class LineType(val prio: Int){
    Normal(2), ToolPath(3), ToolContour(1)
}

open class Figure(var type: LineType = LineType.Normal)

class Line(val p0: Point, val p1: Point, type: LineType = LineType.Normal) : Figure(type){
    private var _squaredLength: Value? = null
    private var _length: Value? = null
    private var _midPoint: Point? = null

    val squaredLength : Value
        get(){
            if(_squaredLength == null){
                _squaredLength = ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))
            }
            return _squaredLength!!
        }

    val length : Value
        get(){
            if(_length == null){
                _length = squaredLength.sqrt()
            }
            return _length!!
        }

    val midPoint : Point
        get(){
            if(_midPoint == null){
                _midPoint = (p0 + p1) / 2.0
            }
            return _midPoint!!
        }
}

open class Circle(val center: Point, val radius: Value) : Figure()

class Arc(center: Point, radius: Value, val start: Value, val end: Value) : Circle(center,radius){
    val p0 = Point.onCircle(center, radius, start)
    val p1 = Point.onCircle(center, radius, end)
}

class Point(val x: Value, val y: Value, type: LineType = LineType.Normal) : Figure(type) {
    companion object{
        fun onCircle(center: Point, radius: Value, angle: Value) : Point{
            return Point(
                (center.x + radius * Value.cos(angle)),
                (center.y + radius * Value.sin(angle))
            )
        }
    }

    fun rotate(center: Point, angle: Value) : Point{
        val a = Value.sin(angle)
        val b = Value.cos(angle)

        return Point(
            b * ( x - center.x) - a * (y - center.y) + center.x,
            a * ( x - center.x) + b * ( y - center.y) + center.y)
    }

    fun copy(): Point {
        return Point(Const(x.value), Const(y.value))
    }

    fun scalarProduct(other: Point) : Value{
        return x * other.x + y * other.y
    }

    fun vectorProduct(other: Point) : Value{
        return x * other.y - y * other.x
    }

    operator fun times(other: Point) : Value{
        return x * other.y - y * other.x
    }

    operator fun times(other: Value) : Point{
        return Point(x * other, y * other)
    }

    operator fun times(other: Double) : Point{
        val value = Const(other)
        return Point(x * value, y * value)
    }

    operator fun div(other: Value) : Point{
        return Point(x / other, y / other)
    }

    operator fun div(other: Double) : Point{
        val value = Const(other)
        return Point(x / value, y / value)
    }

    operator fun plus(right: Point) : Point {
        return Point(x + right.x, y + right.y)
    }

    operator fun minus(right: Point) : Point {
        return Point(x - right.x, y - right.y)
    }

    operator fun minus(right: Value) : Point {
        return Point(x - right, y - right)
    }

    fun squaredLength(): Value {
        return x.pow(2) + y.pow(2)
    }

    fun length(): Value {
        return squaredLength().sqrt()
    }

    fun squaredLength(other: Point): Value {
        return (x - other.x).pow(2) + (y-other.y).pow(2)
    }

    fun length(other: Point): Value {
        return squaredLength(other).sqrt()
    }

    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }
}
