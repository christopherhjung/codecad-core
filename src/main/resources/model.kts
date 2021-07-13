import SketchScope.Companion.AXIS_X
import SketchScope.Companion.ORIGIN


project {
    var length: Value? = null
    var radius: Value? = null

    sketch {
        //circle(point(0.0,0.0), const(1.0))

        val a = line(ORIGIN,point(1.0,0.5))
        val b = line(ORIGIN,point(1.0,1.0))


        //perpendicular(a,b)
        //pointOnPoint(a.a, nullPoint)
        //pointOnPoint(a.b, nullPoint)

        //pointOnPoint(a.a, b.a)
        //pointOnPoint(a.a, nullPoint)

        radius = param(1.2)
        val arc = arc(point(1.5,0.5), radius!!, param(0.2), param(90.0 unit deg))

        pointOnArcEnd(b.p1, arc)
        pointOnArcStart(a.p1, arc)
        equalLength(a,b)
        length = param(1.0 unit mm)
        length(a, length!!)
        angle(a,b,const(30.0 unit deg))
        angle(a, AXIS_X, const(30.0 unit deg))

        //pointOnLine(arc.center, b)
        tangent(arc, a)
        tangent(arc, b)


        //val area = getArea(arc.center)
    }


    println(length?.value)
    println(radius?.value)

}
