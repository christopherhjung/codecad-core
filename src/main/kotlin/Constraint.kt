import kotlin.math.*

abstract class Constraint {

    var lineNumber: Int = -1
    var cache: Value? = null

    protected abstract fun equationImpl() : Value

    val equation: Value
        get() {
            if(cache == null) {
                cache = equationImpl()
            }

            return cache!!
        }

    open fun prune(sketch: Sketch) {
    }
}

//class Arc(val center: Point, val rad: Value, val start: Value, val end: Value) : Element

class PointOnPoint(val a: Point, val b: Point) : Constraint() {


    override fun equationImpl() : Value{
        return ((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
    }

    override fun prune(sketch: Sketch) {
        sketch.merge(a.x, b.x)
        sketch.merge(a.y, b.y)
    }
}

class PointToPointDistance(val a: Point, val b: Point, val distance: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (a.x - b.x).pow(2) + (a.y - b.y).pow(2) - distance.pow(2)
    }
}

class PointOnLine(val point: Point, val line: Line) : Constraint() {
    /*override fun error(): Double {
        val dx = line.b.x.value - line.a.x.value
        val dy = line.b.y.value - line.a.y.value

        val m = dy / dx
        val n = dx / dy

        return if (m <= 1 && m >= -1) {
            //Calculate the expected y point given the x coordinate of the point
            val Ey = line.a.y.value + m * (point.x.value - line.a.x.value)
            (Ey - point.y.value).pow(2)
        } else {
            //Calculate the expected x point given the y coordinate of the point
            val Ex = line.a.x.value + n * (point.y.value - line.a.y.value)
            (Ex - point.x.value).pow(2)
        }
    }*/

    override fun equationImpl(): Value {
        TODO("Not yet implemented")
    }
}

class LineLength(val line: Line, val length: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (line.length - length).pow(2)
    }
}

class EqualLength(val line1: Line, val line2: Line) : Constraint() {
    override fun equationImpl(): Value {
        return (line1.length - line2.length).pow(2)
    }
}

class Horizontal(val line: Line) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(line.p0.y, line.p1.y)
    }

    override fun equationImpl(): Value {
        val direction = line.p1 - line.p0
        val angle = ArcSinValue(direction.y / direction.length())
        return angle.pow(2)
    }
}

class Vertical(val line: Line) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(line.p0.x, line.p1.x)
    }

    override fun equationImpl(): Value {
        val direction = line.p1 - line.p0
        val angle = ArcSinValue(direction.x / direction.length())
        return angle.pow(2)
    }
}

class CircleTangent(val circle: Circle, val line: Line) : Constraint() {
    fun getDelta() : Value{
        val direction = line.p1 - line.p0
        val distanceCenter = line.p0 - circle.center
        val offset = direction.vectorProduct(distanceCenter).pow(2) / direction.squaredLength()
        return offset.sqrt()
    }

    override fun equationImpl(): Value {
        val direction = line.p1 - line.p0
        val distanceCenter = line.p0 - circle.center
        val offset = direction.vectorProduct(distanceCenter).pow(2) / direction.squaredLength()
        val error = (offset - circle.radius.pow(2)).pow(2)
        return error

/*
        val distanceCenter = circle.center - line.p0
        val direction = line.p1 - line.p0
        val projected = direction * distanceCenter.scalarProduct(direction) / direction.squaredLength()
        val offset = distanceCenter - projected
        val error = (offset.length() - circle.radius).pow(2)
        return error */



        /*val circleCenter = circle.center
        val lineStart = line.p0
        val lineEnd = line.p1
        val lineDirection = lineEnd - lineStart

        val distToCenter = circleCenter - lineStart

        val temp = ( distToCenter.scalarProduct(lineDirection) ) / ( lineDirection.scalarProduct(lineDirection) )
        val temp2 = lineDirection * temp
        val perpendicular = distToCenter - temp2

        val a = (perpendicular.squaredLength() - circle.radius.pow(2.0) ).pow(2.0)
        val b = ( perpendicular.scalarProduct(lineDirection) ).pow(2.0)

        return (a + b).sqrt()*/
/*
        val projection = projectOntoLine(this.line, this.circle.center);
        val projectionRadius = distanceBetweenPoints(this.circle.center, projection);

        val dr = projectionRadius - this.circle.radius

        val dx = projection.x - this.circle.center.x;
        val dy = projection.y - this.circle.center.y;
        val da = Point(dx, dy);

        val normalized = da / da.length()
        val offset = Point(normalized.x * dr, normalized.y * dr);

        return (this.circle.radius - dr).pow(2) + offset.squaredLength()*/
/*
        val lineStart = line.p0
        val lineEnd = line.p1
        val lineDirection = lineEnd - lineStart

        val hyp = lineDirection.length()
        val hypRad = circle.radius / hyp

        val Rx =
            Point(circle.center.x - lineDirection.y * hypRad, circle.center.y + lineDirection.x * hypRad)
        val Ry =
            Point(circle.center.x + lineDirection.y * hypRad, circle.center.y - lineDirection.x * hypRad)

        val cross = lineStart * lineEnd
        var error1 = (lineDirection.vectorProduct(Rx) + cross) / hyp
        var error2 = (lineDirection.vectorProduct(Ry) + cross) / hyp
        error1 *= error1
        error2 *= error2
        return Value.min(error1, error2)*/
    }
}

fun pointAlongLine(line: Line, r: Value): Point {
    val px = line.p0.x + r * (line.p1.x - line.p0.x)
    val py = line.p0.y + r * (line.p1.y - line.p0.y)
    return Point(px, py);
}

fun projectionFactorBetween(line: Line, point: Point): Value {
    val dx = line.p0.x - line.p1.x;
    val dy = line.p0.y - line.p1.y;
    val len2 = dx * dx + dy * dy;
    return -((point.x - line.p0.x) * dx + (point.y - line.p0.y) * dy) / len2;
}

fun projectOntoLine(line: Line, point: Point): Point {
    val r = projectionFactorBetween(line, point);
    return pointAlongLine(line, r);
}

fun distanceBetweenPoints(p0: Point, p1: Point): Value {
    val dx = p0.x - p1.x;
    val dy = p0.y - p1.y;
    return (dx * dx + dy * dy).sqrt();
}

class Perpendicular(val line1: Line, val line2: Line) : Constraint() {
    override fun equationImpl(): Value {
        return lineCross(line1, line2, false).pow(2)
    }
}

fun lineCross(line1: Line, line2: Line, cross: Boolean = true): Value {
    var dx = line1.p1.x - line1.p0.x
    var dy = line1.p1.y - line1.p0.y
    var dx2 = line2.p1.x - line2.p0.x
    var dy2 = line2.p1.y - line2.p0.y

    val hyp1 = line1.length
    val hyp2 = line2.length

    dx /= hyp1
    dy /= hyp1
    dx2 /= hyp2
    dy2 /= hyp2

    return if (cross) {
        dx * dy2 - dy * dx2
    } else {
        dx * dx2 + dy * dy2
    }
}

class Parallel(val line1: Line, val line2: Line) : Constraint() {
    override fun equationImpl(): Value {
        return lineCross(line1, line2).pow(2)
    }
}

class Colinear(val line1: Line, val line2: Line) : Constraint() {
    /*override fun error(): Double {
        var error = 0.0
        val dx = line1.b.x.value - line1.a.x.value
        val dy = line1.b.y.value - line1.a.y.value

        val m = dy / dx
        val n = dx / dy
        // Calculate the error between the expected intersection point
        // and the true point of the second lines two end points on the
        // first line
        if (m <= 1 && m > -1) {
            //Calculate the expected y point given the x coordinate of the point
            var Ey = line1.a.y.value + m * (line2.a.x.value - line1.a.x.value)
            error += (Ey - line2.a.y.value).pow(2.0)

            Ey = line1.a.y.value + m * (line2.b.x.value - line1.a.x.value)
            error += (Ey - line2.b.y.value).pow(2.0)
        } else {
            //Calculate the expected x point given the y coordinate of the point
            var Ex = line1.a.x.value + n * (line2.a.y.value - line1.a.y.value)
            error += (Ex - line2.a.x.value).pow(2.0)

            Ex = line1.a.x.value + n * (line2.b.y.value - line1.a.y.value)
            error += (Ex - line2.b.x.value).pow(2.0)
        }

        return error
    }*/

    override fun equationImpl(): Value {
        TODO("Not yet implemented")
    }
}

class PointOnCircle(val point: Point, val circle: Circle) : Constraint() {

    override fun equationImpl(): Value {
        val rad1 = circle.center.length(point)
        return (rad1 - circle.radius).pow(2)
    }
}

class Concentric(val circle1: Circle, val circle2: Circle) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(circle1.radius, circle2.radius)
    }

    override fun equationImpl(): Value {
        return circle1.center.squaredLength(circle2.center)
    }
}

fun pointOnArcError(point: Point, arc: Circle, angle: Value): Value {
    val x = (arc.center.x + arc.radius * Value.cos(angle))
    val y = (arc.center.y + arc.radius * Value.sin(angle))

    return (point.x - x).pow(2) + (point.y - y).pow(2)
}

class PointOnArcStart(val point: Point, val arc: Arc) : Constraint() {
    override fun equationImpl(): Value {
        return pointOnArcError(point, arc, arc.start)
    }
}

class PointOnArcEnd(val point: Point, val arc: Arc) : Constraint() {
    override fun equationImpl(): Value {
        return pointOnArcError(point, arc, arc.end)
    }
}

class PointOnLineMidpoint(val point: Point, val line: Line) : Constraint() {
    override fun equationImpl(): Value {
        val eX = (line.p0.x + line.p1.x) / 2
        val eY = (line.p0.y + line.p1.y) / 2
        return (eX - point.x).pow(2) + (eY - point.y).pow(2)
    }
}

class InternalAngle(val line1: Line, val line2: Line, val angle: Value) : Constraint() {
    override fun equationImpl(): Value {
        return (lineCross(line1, line2, false) - CosValue(angle)).pow(2)
    }
}

class Radius(val circle: Circle, val radius: Value) : Constraint() {
    override fun prune(sketch: Sketch) {
        sketch.merge(circle.radius, radius)
    }

    override fun equationImpl(): Value {
        return (radius - circle.radius).pow(2)
    }
}

class Equals(val left: Value, val right: Value) : Constraint() {


    override fun prune(sketch: Sketch) {
        sketch.merge(left, right)
    }

    override fun equationImpl(): Value {
        return (left - right).pow(2)
    }
}
