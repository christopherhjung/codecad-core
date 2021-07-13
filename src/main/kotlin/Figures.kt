enum class LineType(val prio: Int){
    Normal(2), ToolPath(3), ToolContour(1)
}

open class Figure(var type: LineType = LineType.Normal)

class Line(val p0: Point, val p1: Point, type: LineType = LineType.Normal) : Figure(type){
    fun squaredLength() : Value{
        return ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))
    }

    fun length() : Value{
        return squaredLength().sqrt()
    }
}

open class Circle(val center: Point, val radius: Value) : Figure()

class Arc(center: Point, radius: Value, val start: Value, val end: Value) : Circle(center,radius)

class Point(val x: Value, val y: Value, type: LineType = LineType.Normal) : Figure(type) {

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

    operator fun plus(right: Point) : Point {
        return Point(this.x + right.x, this.y + right.y)
    }

    operator fun minus(right: Point) : Point {
        return Point(this.x - right.x, this.y - right.y)
    }

    operator fun minus(right: Value) : Point {
        return Point(this.x - right, this.y - right)
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
