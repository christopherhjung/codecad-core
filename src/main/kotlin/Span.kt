import kotlin.math.cos
import kotlin.math.sin

class LineStepper(val line: Line) : Stepper {
    var i = 0

    override fun hasNext(): Boolean {
        return i <= 1
    }

    override fun next(): Vector {
        val value = if(i == 0) line.a else line.b
        i++
        return value.toVector()
    }
}

class ArcStepper(val arc: Circle) : Stepper {
    var i = 0
    var max = 100

    val start = arc.end!!.value
    val end = arc.start!!.value

    val diff = end - start

    override fun hasNext(): Boolean {
        return i <= 100
    }

    override fun next(): Vector {
        val currentAngle = start + diff * (i / 100.0)
        val x = (arc.center.x.value + arc.rad.value * cos(currentAngle))
        val y = (arc.center.y.value + arc.rad.value * sin(currentAngle))
        i++
        return Vector(x, y)
    }
}


interface Span{
    fun getPoint(t: Double): Vector
}

class LineSpan(val line: Line) : Span {
    override fun getPoint(t: Double) : Vector{
        val a = line.a.toVector()
        val b = line.b.toVector()
        return (b - a) * t + a
    }
}

class ArcSpan(val arc: Circle) : Span {
    val start = arc.end?.value ?: 0.0
    val end = arc.start?.value ?: Math.PI * 2
    val diff = end - start
    override fun getPoint(t: Double): Vector {
        val currentAngle = start + diff * t
        val x = (arc.center.x.value + arc.rad.value * cos(currentAngle))
        val y = (arc.center.y.value + arc.rad.value * sin(currentAngle))
        return Vector(x, y)
    }
}
