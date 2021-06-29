class Builder{
    val sketch = Sketch()

    fun param(value: Double = 0.0) : ProxyValue{
        return sketch.createParameter(value)
    }

    fun paramDegree(value: Double = 0.0) : ProxyValue{
        return sketch.createParameter(Math.toRadians(value))
    }

    fun const(value: Double = 0.0) : Parameter{
        return sketch.createConst(value)
    }

    fun constDegree(value: Double = 0.0) : Parameter{
        return sketch.createConst(Math.toRadians(value))
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0) : Point{
        return sketch.createConstPoint(x,y)
    }

    fun point(x: Double = 0.0, y: Double = 0.0) : Point{
        return sketch.createPoint(x,y)
    }

    fun line(a: Point, b:Point) : Line{
        return sketch.createLine(a,b)
    }

    fun circle(center: Point, radius:Value) : Circle{
        return sketch.createCircle(center,radius)
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0) : Line{
        return sketch.createLine(x,y,x2,y2)
    }

    fun tangent(circle: Circle, line: Line){
        sketch.addConstraint(CircleTangent(circle,line))
    }

    fun pointOnLineMidpoint(point: Point, line: Line){
        sketch.addConstraint(PointOnLineMidpoint(point, line))
    }

    fun horizontal(line: Line){
        sketch.addConstraint(Horizontal(line))
    }

    fun vertical(line: Line){
        sketch.addConstraint(Vertical(line))
    }

    fun pointOnCircle(point: Point, circle: Circle){
        sketch.addConstraint(PointOnCircle(point, circle))
    }

    fun equalLength(line1: Line, line2: Line){
        sketch.addConstraint(EqualLength(line1, line2))
    }

    fun angle(line1: Line, line2: Line, angle: Value){
        sketch.addConstraint(InternalAngle(line1, line2, angle))
    }

    fun solve(){
        sketch.solve()
    }

}

fun sketch(init: Builder.() -> Unit): Sketch {
    val builder = Builder()
    builder.init()
    return builder.sketch
}


fun Canvas.line(line: Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}

fun main(args: Array<String>) {

    val start = System.currentTimeMillis()
    val sketch = sketch{
        val A = constPoint()
        val M = point(0.9,0.0)
        val C = point(1.8,0.0)
        val P = point(10.0,1.0)
        val B = point(1.0,2.0)

        val r = const(1.0)
        val circle = circle(P,r)

        val lineAB = line(A,B)
        val lineAC = line(A,C)
        val lineAP = line(A,P)
        val linePC = line(P,C)
        val lineMP = line(M,P)
        val lineMC = line(M,C)

        tangent(circle, lineAB)
        tangent(circle, lineAC)
        pointOnLineMidpoint(M, lineAC)
        horizontal(lineAC)
        vertical(linePC)
        pointOnCircle(C, circle)
        pointOnCircle(B, circle)
        equalLength(lineMC, linePC)
        val rad = param(Math.toRadians(30.0))
        angle(linePC, lineMP, rad)
        angle(linePC, lineMP, rad)



        solve()

        /*var theAngle = InternalAngle(linePC, lineMP,const(Math.toRadians(45.0)) )
        println(theAngle.error())
        theAngle = InternalAngle(lineMP, lineMC,const(Math.toRadians(45.0)) )
        println(theAngle.error())
*/

        println(Math.toDegrees(rad.value))




        println((lineAC.a.toVector() - lineAC.b.toVector()).length())
        println((lineAP.a.toVector() - lineAP.b.toVector()).length())

        val canvas = Canvas()
        canvas.line(lineAB)
        canvas.line(lineAC)
        canvas.line(lineAP)
        canvas.line(linePC)
        canvas.line(lineMP)
        canvas.line(lineMC)
        canvas.circle(circle)
        canvas.writeImage()
    }



    println(sketch)

    val end = System.currentTimeMillis()

    println("time need: ${end-start}")


}

