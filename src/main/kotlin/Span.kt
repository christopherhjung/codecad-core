import kotlin.math.cos
import kotlin.math.sin

interface Span{
    fun getPoint(t: Double): Point
}

class LineSpan(val line: Line) : Span {
    override fun getPoint(t: Double) : Point{
        val a = line.p0
        val b = line.p1
        return ((b - a) * t + a).copy()
    }
}

class ArcSpan(val arc: Arc) : Span {
    val start = arc.end.value
    val end = arc.start.value
    val diff = end - start
    override fun getPoint(t: Double): Point {
        val currentAngle = start + diff * t
        val x = (arc.center.x.value + arc.radius.value * cos(currentAngle))
        val y = (arc.center.y.value + arc.radius.value * sin(currentAngle))
        return Point(Const(x), Const(y))
    }
}
