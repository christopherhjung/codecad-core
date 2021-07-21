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
    val start = arc.center.absoluteAngle(arc.p0)
    var end = arc.center.absoluteAngle(arc.p1)
    val diff: Double

    init{
        if(end < start){
            end += 2*Math.PI
        }
        diff = end - start
    }
    override fun getPoint(t: Double): Point {

        val currentAngle = start + diff * t
        val x = (arc.center.x.value + arc.radius.value * cos(currentAngle))
        val y = (arc.center.y.value + arc.radius.value * sin(currentAngle))
        return Point(Value.const(x), Value.const(y))
    }
}

class FunctionSpan(val func: FunctionFigure) : Span {
    override fun getPoint(t: Double): Point {
        func.t .value = t
        return func.function.copy()
    }
}
