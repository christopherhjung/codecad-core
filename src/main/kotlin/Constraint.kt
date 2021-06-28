import kotlin.math.pow
import kotlin.math.sqrt

abstract class Constraint {

    abstract fun error() : Double

}

class Value(var value: Double){

}

class Point(val x: Value, val y: Value){

}


class Line(val a: Point, val b: Point){

}

class PointOnPoint(val a: Point, val b: Point) : Constraint() {
    override fun error() : Double {
        return (a.x.value - b.x.value).pow(2) + (a.y.value - b.y.value).pow(2)
    }
}

class LineLength(val line : Line, val length: Value) : Constraint(){
    override fun error(): Double {
        val temp = sqrt((line.b.x.value - line.a.x.value).pow(2) + (line.b.y.value - line.a.y.value).pow(2)) - length.value
        return temp * temp * 100
    }
}

class Horizontal(val line:Line) : Constraint(){
    override fun error(): Double {
        val ody = line.b.y.value - line.a.y.value
        return ody * ody * 1000
    }
}

class Vertical(val line:Line) : Constraint(){
    override fun error(): Double {
        val ody = line.b.x.value - line.a.x.value
        return ody * ody * 1000
    }
}


