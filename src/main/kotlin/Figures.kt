enum class LineType(val prio: Int){
    Normal(2), ToolPath(3), ToolContour(1)
}

open class Figure(var type: LineType = LineType.Normal)

class Line(val a: Point, val b: Point, type: LineType = LineType.Normal) : Figure(type){
    fun squaredLength() : Value{
        return ((b.x - a.x).pow(2) + (b.y - a.y).pow(2))
    }

    fun length() : Value{
        return squaredLength().sqrt()
    }
}

open class Circle(val center: Point, val rad: Value) : Figure()

class Arc(center: Point, radius: Value, val start: Value, val end: Value) : Circle(center,radius)

class Point(override val x: Value, override val y: Value, type: LineType = LineType.Normal) : Figure(type), AbstractPoint {


    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }
}
