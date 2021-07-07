import com.angusj.clipper.Clipper
var length: Value? = null

sketch {
    //circle(point(0.0,0.0), const(1.0))

    val nullPoint = constPoint()
    val xAxis = line(nullPoint, constPoint(1.0, 0.0))


    val a = line(nullPoint,point(1.0,0.5))
    val b = line(nullPoint,point(1.0,1.0))


    //perpendicular(a,b)
    //pointOnPoint(a.a, nullPoint)
    //pointOnPoint(a.b, nullPoint)

    //pointOnPoint(a.a, b.a)
    //pointOnPoint(a.a, nullPoint)

    val arc = arc(point(1.5,0.5), param(1.2), param(0.0), param(90.0 unit deg))

    pointOnArcEnd(b.b, arc)
    pointOnArcStart(a.b, arc)
    equalLength(a,b)
    length = const(1.0 unit mm)
    length(a, length!!)
    angle(a,b,const(30.0 unit deg))
    angle(a, xAxis, const(30.0 unit deg))

    //pointOnLine(arc.center, b)
    tangent(arc, a)
    tangent(arc, b)
}


println(length!!.value)


