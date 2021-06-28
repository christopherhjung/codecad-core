import org.opencv.imgcodecs.Imgcodecs


fun main(args: Array<String>) {
    val constraints = ArrayList<Constraint>()

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

    val iter = 1
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

        val lineAB = Line(A,B)
        val lineAC = Line(A,C)
        val lineAP = Line(A,P)
        val lineMP = Line(M,P)
        val linePC = Line(P,C)
        val lineMC = Line(M,C)

        sketch.addConstraint(CircleTangent(circle,lineAB))
        sketch.addConstraint(CircleTangent(circle,lineAC))
        sketch.addConstraint(PointOnLineMidpoint(M, lineAC))
        sketch.addConstraint(Horizontal(lineAC))
        sketch.addConstraint(PointOnCircle(C, circle))
        sketch.addConstraint(EqualLength(lineMC, linePC))

        sketch.solve()

        if(iter == 1){
            println((lineAC.a.toVector() - lineAC.b.toVector()).length())
            println((lineAP.a.toVector() - lineAP.b.toVector()).length())
        }
    }
    val end = System.currentTimeMillis()

    println("time need: ${end-start}")
/*
    */

    //Output().draw()



}

