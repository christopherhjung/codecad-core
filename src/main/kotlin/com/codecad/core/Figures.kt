package com.codecad.core

import com.codecad.common.PointD
import com.codecad.core.SketchScope.Companion.AXIS_X
import java.lang.Math.atan2

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}

abstract class Figure(var type: LineType = LineType.Normal){
}

class LineSegment(val p0: Point, val p1: Point, type: LineType = LineType.Normal) : Figure(type){

    val squaredLength : Value
        get() = ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))

    val length : Value
        get() = squaredLength.sqrt()

    val midPoint : Point
        get() = (p0 + p1) / 2.0

    val difference : Point
        get() = p1 - p0

    val direction : Point
        get() = difference.normalized()
}

open class Circle(val center: Point, val radius: Value) : Figure()

/*
class com.codecad.core.Arc(center: com.codecad.core.Point, radius: com.codecad.core.Value, val start: com.codecad.core.Value, val end: com.codecad.core.Value) : com.codecad.core.Circle(center,radius){
    val p0 = com.codecad.core.Point.onCircle(center, radius, start)
    val p1 = com.codecad.core.Point.onCircle(center, radius, end)
}*/


class Arc(val p0: Point, val p1: Point, val helper: Value) : Circle( centerFunction(p0,p1,helper), (p0 -  centerFunction(p0,p1,helper)).length()){
    companion object{
        private fun centerFunction(p0: Point, p1: Point, arcRadius: Value): Point {
            val direction = p1 - p0
            val half = direction / 2.0
            val middle = p0 + half
            val normalizedDirection = direction.normalized()
            val positive = Point(-normalizedDirection.y, normalizedDirection.x)
            return middle + positive * arcRadius
        }
    }
}

class FunctionFigure( val function: (Value) -> Point) : Figure(){

}

class Point(val x: Value, val y: Value, type: LineType = LineType.Normal) : Figure(type) {
    companion object{
        fun onCircle(center: Point, radius: Value, angle: Value) : Point {
            return Point(
                (center.x + radius * Value.cos(angle)),
                (center.y + radius * Value.sin(angle))
            )
        }

        fun conditional(condition: Value, left: Point, right: Point) : Point {
            return Point(
                Value.conditional(condition, left.x, right.x),
                Value.conditional(condition, left.y, right.y)
            )
        }
    }

    fun absoluteAngle(target: Point) : Double{
        val a = AXIS_X.p1
        val b = target - this
        return atan2((a.x * b.y - a.y * b.x).value , (a.x * b.x + a.y * b.y).value)
    }

    fun normalized() : Point {
        return this / length()
    }

    fun rotate(center: Point, angle: Value) : Point {
        val a = Value.sin(angle)
        val b = Value.cos(angle)

        return Point(
            b * ( x - center.x) - a * (y - center.y) + center.x,
            a * ( x - center.x) + b * ( y - center.y) + center.y)
    }

    fun copy(): Point {
        return Point(Value.const(x.value), Value.const(y.value))
    }

    fun fixed() : PointD {
        return PointD(x.value, y.value)
    }

    fun scalar(other: Point) : Value {
        return x * other.x + y * other.y
    }

    fun cross(other: Point) : Value {
        return x * other.y - y * other.x
    }

    operator fun times(other: Point) : Value {
        return x * other.y - y * other.x
    }

    operator fun times(other: Value) : Point {
        return Point(x * other, y * other)
    }

    operator fun times(other: Double) : Point {
        val value = Value.const(other)
        return Point(x * value, y * value)
    }

    operator fun div(other: Value) : Point {
        return Point(x / other, y / other)
    }

    operator fun div(other: Double) : Point {
        val value = Value.const(other)
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
        return "com.codecad.core.Point(x=$x, y=$y)"
    }
}
