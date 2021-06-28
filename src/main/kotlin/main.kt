class Builder{
    val sketch = Sketch()

    fun param(value: Double = 0.0) : ProxyValue{
        return sketch.createParameter(value)
    }

    fun const(value: Double = 0.0) : Parameter{
        return sketch.createConst(value)
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

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0) : Line{
        return sketch.createLine(x,y,x2,y2)
    }
}

fun sketch(init: Builder.() -> Unit): Builder {
    val builder = Builder()
    builder.init()
    return builder
}

fun main(args: Array<String>) {
    val constraints = ArrayList<Constraint>()

    sketch{

    }

    /*val v1 = Value(0.0)
    val v2 = Value(0.0)
    val v3 = Value(5.0)
    val v4 = Value(-1.0)
    val v5 = Value(10.0)
    val a = Point(v1,v2)
    val b = Point(v3,v4)

    val line = Line(a,b)

    constraints.add(LineLength(line, v5))

    println(Solver().solve(listOf(v4),constraints,false))

    println(v3.value)
    println(v4.value)*/

    val iter = 0
    val start = System.currentTimeMillis()
    for(i in 0 until iter){
        val sketch = Sketch()

        val A = sketch.createConstPoint()
        val M = sketch.createPoint(1.0,0.0)
        val C = sketch.createPoint(2.0,0.0)
        val P = sketch.createPoint(2.0,1.0)
        val B = sketch.createPoint(1.0,2.0)

        val r = sketch.createConst(1.0)
        val circle = Circle(P,r)

        val lineAB = sketch.createLine(A,B)
        val lineAC = sketch.createLine(A,C)
        val lineAP = sketch.createLine(A,P)
        val linePC = sketch.createLine(P,C)
        val lineMP = sketch.createLine(M,P)
        val lineMC = sketch.createLine(M,C)

        sketch.addConstraint(CircleTangent(circle,lineAB))
        sketch.addConstraint(CircleTangent(circle,lineAC))
        sketch.addConstraint(PointOnLineMidpoint(M, lineAC))
        sketch.addConstraint(Horizontal(lineAC))
        sketch.addConstraint(Vertical(linePC))
        sketch.addConstraint(PointOnCircle(C, circle))
        //sketch.addConstraint(EqualLength(lineMC, linePC))

        val angle = sketch.createConst(Math.toRadians(45.0))

        sketch.addConstraint(InternalAngle(lineMP, lineAC, angle))
        //sketch.addConstraint(InternalAngle(lineMP, lineMC, angle))


        //val angle = ;



        sketch.solve()

        println(sketch)

        if(iter == 1){
            println((lineAC.a.toVector() - lineAC.b.toVector()).length())
            println((lineAP.a.toVector() - lineAP.b.toVector()).length())
        }
    }
    val end = System.currentTimeMillis()

    println("time need: ${end-start}")
/*
    */

    Output().draw()



}

